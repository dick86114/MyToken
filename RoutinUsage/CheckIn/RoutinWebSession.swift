import AppKit
import Foundation
import WebKit

enum RoutinNavigationDecision: Equatable {
    case allowInWebView
    case openExternally
    case cancel
}

@MainActor
final class RoutinWebSession: NSObject, RoutinWebSessionManaging {
    nonisolated static let siteURL = URL(string: "https://routin.ai")!
    nonisolated static let loginURL = URL(string: "https://routin.ai/login")!
    nonisolated static let lotteryURL = URL(string: "https://routin.ai/dashboard/lottery")!

    // 这些域名来自 Routin 官方登录页当前支持的第三方授权入口。
    nonisolated static let approvedIdentityHosts: Set<String> = [
        "github.com",
        "gitee.com",
        "accounts.google.com",
        "connect.linux.do"
    ]

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

    func hasAuthenticatedSession() async -> Bool {
        // 网站的认证状态由页面自己判定，应用既不读取也不导出 Cookie。
        webView.load(URLRequest(url: Self.lotteryURL))
        for _ in 0..<20 {
            do {
                try await Task.sleep(for: .milliseconds(500))
                let page = try await readPage()
                if isLoginPage(page) {
                    return false
                }
                if isLotteryPage(page) {
                    return true
                }
            } catch {
                return false
            }
        }
        return false
    }

    func prepareLogin() async {
        isAwaitingLogin = true
        webView.load(URLRequest(url: Self.loginURL))
    }

    func performCheckIn() async throws -> RoutinCheckInOutcome {
        isAwaitingLogin = false
        webView.load(URLRequest(url: Self.lotteryURL))

        for _ in 0..<20 {
            try await Task.sleep(for: .milliseconds(500))
            let page = try await readPage()

            if isLoginPage(page) {
                return .needsLogin
            }
            if let outcome = Self.pageOutcome(from: page.title, bodyText: page.bodyText) {
                return outcome
            }

            let didClick = try await clickDrawButtonIfAvailable()
            if didClick {
                return try await waitForDrawResult()
            }
        }
        return .cannotConfirm
    }

    func clearRoutinWebsiteData() async {
        let dataStore = WKWebsiteDataStore.default()
        let dataTypes = WKWebsiteDataStore.allWebsiteDataTypes()
        let records = await dataStore.dataRecords(ofTypes: dataTypes)
        let routinRecords = records.filter { record in
            let host = record.displayName.lowercased()
            return host == "routin.ai" || host.hasSuffix(".routin.ai")
        }
        guard !routinRecords.isEmpty else {
            return
        }
        await dataStore.removeData(ofTypes: dataTypes, for: routinRecords)
    }

    nonisolated static func navigationDecision(
        for url: URL,
        approvedIdentityHosts: Set<String>
    ) -> RoutinNavigationDecision {
        guard url.scheme?.lowercased() == "https", let host = url.host?.lowercased() else {
            return .cancel
        }
        if host == "routin.ai" || host.hasSuffix(".routin.ai") || approvedIdentityHosts.contains(host) {
            return .allowInWebView
        }
        return .openExternally
    }

    nonisolated static func pageOutcome(from title: String?, bodyText: String) -> RoutinCheckInOutcome? {
        let normalized = [title ?? "", bodyText].joined(separator: "\n")
        if normalized.contains("今日已抽") || normalized.contains("今天已签到") {
            return .alreadyCheckedIn
        }
        if normalized.contains("抽奖成功") || normalized.contains("签到成功") {
            return .succeeded
        }
        return nil
    }
}

private extension RoutinWebSession {
    struct PageContent {
        let title: String?
        let bodyText: String
    }

    func readPage() async throws -> PageContent {
        let result = try await evaluateJavaScript("""
            JSON.stringify({
              title: document.title || '',
              bodyText: document.body ? document.body.innerText : '',
              path: window.location.pathname || ''
            })
            """)
        guard
            let json = result as? String,
            let data = json.data(using: .utf8),
            let value = try JSONSerialization.jsonObject(with: data) as? [String: String]
        else {
            return PageContent(title: nil, bodyText: "")
        }
        return PageContent(title: value["title"], bodyText: value["bodyText"] ?? "")
    }

    func isLoginPage(_ page: PageContent) -> Bool {
        let text = [page.title ?? "", page.bodyText].joined(separator: "\n").lowercased()
        return text.contains("sign in")
            || text.contains("login")
            || text.contains("登录你的账号")
            || text.contains("登录") && text.contains("密码")
    }

    func isLotteryPage(_ page: PageContent) -> Bool {
        let text = [page.title ?? "", page.bodyText].joined(separator: "\n")
        return text.contains("签到抽奖")
            || text.contains("Daily Lottery")
            || text.contains("立即抽奖")
            || text.contains("今日已抽")
    }

    func clickDrawButtonIfAvailable() async throws -> Bool {
        let result = try await evaluateJavaScript("""
            (() => {
              const button = Array.from(document.querySelectorAll('button')).find((element) => {
                const label = (element.innerText || element.textContent || '').trim();
                return label === '立即抽奖';
              });
              if (!button || button.disabled) return false;
              button.click();
              return true;
            })()
            """)
        return result as? Bool ?? false
    }

    func waitForDrawResult() async throws -> RoutinCheckInOutcome {
        for _ in 0..<20 {
            try await Task.sleep(for: .milliseconds(500))
            let page = try await readPage()
            if let outcome = Self.pageOutcome(from: page.title, bodyText: page.bodyText) {
                return outcome == .alreadyCheckedIn ? .succeeded : outcome
            }
            if page.bodyText.contains("明天再来") {
                return .succeeded
            }
            if isLoginPage(page) {
                return .needsLogin
            }
        }
        return .cannotConfirm
    }

    func evaluateJavaScript(_ script: String) async throws -> Any {
        try await withCheckedThrowingContinuation { continuation in
            webView.evaluateJavaScript(script) { value, error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: value as Any)
                }
            }
        }
    }
}

extension RoutinWebSession: WKNavigationDelegate {
    func webView(
        _ webView: WKWebView,
        decidePolicyFor navigationAction: WKNavigationAction,
        decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
    ) {
        guard let url = navigationAction.request.url else {
            decisionHandler(.cancel)
            return
        }
        switch Self.navigationDecision(for: url, approvedIdentityHosts: Self.approvedIdentityHosts) {
        case .allowInWebView:
            decisionHandler(.allow)
        case .openExternally:
            NSWorkspace.shared.open(url)
            decisionHandler(.cancel)
        case .cancel:
            decisionHandler(.cancel)
        }
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        guard isAwaitingLogin, let url = webView.url else {
            return
        }
        guard
            url.host?.lowercased().hasSuffix("routin.ai") == true,
            url.path.hasPrefix("/dashboard")
        else {
            return
        }
        isAwaitingLogin = false
        onLoginCompleted?()
    }
}
