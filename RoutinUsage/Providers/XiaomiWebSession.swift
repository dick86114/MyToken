import AppKit
import Foundation
import WebKit

@MainActor
final class XiaomiWebSession: NSObject {
    nonisolated static let consoleURL = URL(string: "https://platform.xiaomimimo.com/console/balance")!

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
