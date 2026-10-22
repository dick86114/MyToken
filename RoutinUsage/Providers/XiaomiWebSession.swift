import AppKit
import Foundation
import WebKit

@MainActor
final class XiaomiWebSession: NSObject {
    nonisolated static let consoleURL = URL(string: "https://platform.xiaomimimo.com/console/balance")!
    /// 与网页在登录态失效时使用的入口一致：先进入 genLoginUrl，再沿用持久 SSO 登录态自动换票。
    nonisolated static let silentRenewURL = URL(
        string: "https://platform.xiaomimimo.com/api/v1/genLoginUrl?currentPath=%2Fconsole%2Fbalance"
    )!

    let webView: WKWebView
    var onLoginCompleted: (@MainActor () -> Void)?

    private var isAwaitingLogin = false

    override init() {
        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = .default()
        configuration.preferences.isElementFullscreenEnabled = false
        webView = WKWebView(frame: .zero, configuration: configuration)
        super.init()
        webView.navigationDelegate = self
        webView.allowsBackForwardNavigationGestures = true
    }

    func prepareLogin(resetSession: Bool = false) async {
        if resetSession {
            webView.stopLoading()
            await clearWebsiteData()
        }
        isAwaitingLogin = true
        let request = URLRequest(
            url: Self.consoleURL,
            cachePolicy: resetSession ? .reloadIgnoringLocalAndRemoteCacheData : .useProtocolCachePolicy,
            timeoutInterval: 30
        )
        webView.load(request)
    }

    func captureCookieHeader() async -> String? {
        let cookies = await withCheckedContinuation { continuation in
            webView.configuration.websiteDataStore.httpCookieStore.getAllCookies { cookies in
                continuation.resume(returning: cookies)
            }
        }
        let relevant = cookies.filter { cookie in
            let domain = cookie.domain.lowercased()
            return domain == "xiaomimimo.com" || domain.hasSuffix(".xiaomimimo.com")
        }
        guard relevant.contains(where: { $0.name == "api-platform_serviceToken" }) else {
            return nil
        }

        let preferredOrder = [
            "api-platform_serviceToken",
            "api-platform_ph",
            "api-platform_slh",
            "userId"
        ]
        let ordered = relevant.sorted { lhs, rhs in
            let lhsIndex = preferredOrder.firstIndex(of: lhs.name) ?? preferredOrder.count
            let rhsIndex = preferredOrder.firstIndex(of: rhs.name) ?? preferredOrder.count
            if lhsIndex != rhsIndex {
                return lhsIndex < rhsIndex
            }
            return lhs.name < rhs.name
        }
        return ordered
            .map { "\($0.name)=\($0.value)" }
            .joined(separator: "; ")
    }

    /**
     静默续期：控制台只认短期服务票据（api-platform_serviceToken），但小米账号登录态
     （passToken 等）保存在持久化的 WKWebsiteDataStore 里。票据过期时重新加载控制台，
     站点会用账号登录态静默换发新票据；只有登录态也失效时才会停在同一串旧票据上。

     返回新的 Cookie 头；票据没有变化或超时返回 nil（调用方应退回手动登录）。
     */
    func refreshSessionSilently(
        previousCookieHeader: String?,
        timeout: TimeInterval = 25
    ) async -> String? {
        guard !isAwaitingLogin else {
            return nil
        }
        let previous = previousCookieHeader?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        webView.stopLoading()
        webView.load(URLRequest(
            url: Self.silentRenewURL,
            cachePolicy: .reloadIgnoringLocalAndRemoteCacheData,
            timeoutInterval: 30
        ))

        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            if Task.isCancelled {
                return nil
            }
            if Self.requiresInteractiveLogin(url: webView.url) {
                // 落到交互式登录页：账号登录态也失效了，继续等没有意义。
                return nil
            }
            if let cookie = await captureCookieHeader(), cookie != previous {
                return cookie
            }
            try? await Task.sleep(nanoseconds: 500_000_000)
        }
        return nil
    }

    /// 判断隐藏 WebView 是否已确认停在必须人工操作的登录页。
    ///
    /// `account.xiaomi.com/pass/serviceLogin` 也是浏览器自动换票的 SSO 中转页：
    /// 持久 `passToken` 仍有效时会继续 302 回控制台并换发新的短期票据。这里不能
    /// 仅凭账号域提前终止，否则每次 24 小时服务票据过期都会误判为需要重新登录。
    nonisolated static func requiresInteractiveLogin(url: URL?) -> Bool {
        guard let url else {
            return false
        }
        let host = url.host?.lowercased() ?? ""
        if host == "account.xiaomi.com" || host.hasSuffix(".account.xiaomi.com") {
            return false
        }
        let value = url.absoluteString.lowercased()
        if host == "xiaomimimo.com" || host.hasSuffix(".xiaomimimo.com") {
            return value.contains("/login") || value.contains("#/login")
        }
        return value.contains("login") && value.contains("xiaomi")
    }

    func clearWebsiteData() async {
        let dataStore = WKWebsiteDataStore.default()
        let dataTypes = WKWebsiteDataStore.allWebsiteDataTypes()
        let records = await dataStore.dataRecords(ofTypes: dataTypes)
        let xiaomiRecords = records.filter { record in
            let host = record.displayName.lowercased()
            return host == "xiaomimimo.com"
                || host.hasSuffix(".xiaomimimo.com")
                || host == "xiaomi.com"
                || host.hasSuffix(".xiaomi.com")
                || host == "mi.com"
                || host.hasSuffix(".mi.com")
        }
        guard !xiaomiRecords.isEmpty else {
            return
        }
        await dataStore.removeData(ofTypes: dataTypes, for: xiaomiRecords)
    }
}

extension XiaomiWebSession: WKNavigationDelegate {
    func webView(
        _ webView: WKWebView,
        decidePolicyFor navigationAction: WKNavigationAction,
        decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
    ) {
        guard let url = navigationAction.request.url else {
            decisionHandler(.cancel)
            return
        }
        guard url.scheme?.lowercased() == "https" else {
            decisionHandler(.cancel)
            return
        }
        let host = url.host?.lowercased() ?? ""
        let allowed = host == "xiaomimimo.com"
            || host.hasSuffix(".xiaomimimo.com")
            || host == "xiaomi.com"
            || host.hasSuffix(".xiaomi.com")
            || host == "mi.com"
            || host.hasSuffix(".mi.com")
        if allowed {
            decisionHandler(.allow)
        } else {
            NSWorkspace.shared.open(url)
            decisionHandler(.cancel)
        }
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        guard isAwaitingLogin, let url = webView.url else {
            return
        }
        guard url.host?.lowercased().hasSuffix("xiaomimimo.com") == true,
              url.path.hasPrefix("/console")
        else {
            return
        }
        isAwaitingLogin = false
        onLoginCompleted?()
    }
}
