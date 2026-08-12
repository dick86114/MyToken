import Foundation
import WebKit

enum RoutinGroupDetectionWebError: Error, Equatable, Sendable {
    case needsLogin
    case accountUnavailable
    case logNotFound
    case logTimeout
    case ambiguousLog
    case pageChanged
}

protocol RoutinGroupDetectionWebSessionManaging: Sendable {
    func hasAuthenticatedSession() async -> Bool
    func prepareLogin() async
    func readCurrentAccountIdentity() async throws -> RoutinAccountIdentity
    func findGroupName(marker: CodexGroupProbeRequestMarker) async throws -> String
}

enum RoutinGroupDetectionPageParser {
    private struct AccountPayload: Decodable {
        let email: String
        let displayName: String?
    }

    private struct LogRow: Decodable {
        let userAgent: String
        let groupName: String
    }

    private struct LogDetailPayload: Decodable {
        let state: String
        let groupName: String?
    }

    static func accountIdentity(from json: String) -> RoutinAccountIdentity? {
        guard
            let data = json.data(using: .utf8),
            let payload = try? JSONDecoder().decode(AccountPayload.self, from: data),
            !payload.email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        else {
            return nil
        }
        let displayName = payload.displayName?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        return RoutinAccountIdentity.make(
            email: payload.email,
            displayName: displayName?.isEmpty == false ? displayName! : "Routin 账号"
        )
    }

    static func groupName(
        from json: String,
        marker: CodexGroupProbeRequestMarker
    ) throws -> String {
        guard
            let data = json.data(using: .utf8),
            let rows = try? JSONDecoder().decode([LogRow].self, from: data)
        else {
            throw RoutinGroupDetectionWebError.pageChanged
        }

        let matches = rows.filter { $0.userAgent.contains(marker.userAgent) }
        guard matches.count == 1, let groupName = matches.first?.groupName
            .trimmingCharacters(in: .whitespacesAndNewlines),
              !groupName.isEmpty
        else {
            if matches.isEmpty {
                throw RoutinGroupDetectionWebError.logNotFound
            }
            throw RoutinGroupDetectionWebError.ambiguousLog
        }
        return groupName
    }

    static func detailGroupName(from json: String) throws -> String {
        guard
            let data = json.data(using: .utf8),
            let payload = try? JSONDecoder().decode(LogDetailPayload.self, from: data)
        else {
            throw RoutinGroupDetectionWebError.pageChanged
        }

        guard payload.state == "ready" else {
            if payload.state == "loading" {
                throw RoutinGroupDetectionWebError.logNotFound
            }
            throw RoutinGroupDetectionWebError.pageChanged
        }

        guard let groupName = payload.groupName?.trimmingCharacters(in: .whitespacesAndNewlines),
              !groupName.isEmpty
        else {
            throw RoutinGroupDetectionWebError.pageChanged
        }
        return groupName
    }

    static func isLogPageReady(bodyText _: String, hasTable: Bool) -> Bool {
        hasTable
    }
}

@MainActor
final class RoutinGroupDetectionWebSession: RoutinGroupDetectionWebSessionManaging {
    private static let accountURL = URL(string: "https://routin.ai/dashboard/account")!
    private static let logsURL = URL(string: "https://routin.ai/dashboard/logs/model-requests")!

    private let session: RoutinWebSession

    init(session: RoutinWebSession) {
        self.session = session
    }

    func hasAuthenticatedSession() async -> Bool {
        await session.hasAuthenticatedSession()
    }

    func prepareLogin() async {
        await session.prepareLogin()
    }

    func readCurrentAccountIdentity() async throws -> RoutinAccountIdentity {
        try await load(Self.accountURL)
        for _ in 0..<20 {
            let content = try await evaluate(Self.accountIdentityScript)
            if let json = content as? String,
               let identity = RoutinGroupDetectionPageParser.accountIdentity(from: json) {
                return identity
            }
            try await Task.sleep(for: .milliseconds(500))
        }
        throw RoutinGroupDetectionWebError.accountUnavailable
    }

    func findGroupName(marker: CodexGroupProbeRequestMarker) async throws -> String {
        try await loadLogsPage()

        for _ in 0..<15 {
            switch try await openLogDetail(marker: marker) {
            case .opened:
                return try await readOpenedLogDetail()
            case .notFound:
                try await Task.sleep(for: .seconds(2))
            case .ambiguous:
                throw RoutinGroupDetectionWebError.ambiguousLog
            case .pageChanged:
                throw RoutinGroupDetectionWebError.pageChanged
            }
        }

        throw RoutinGroupDetectionWebError.logTimeout
    }

    private func loadLogsPage() async throws {
        session.webView.load(URLRequest(url: Self.logsURL))
        for _ in 0..<20 {
            try await Task.sleep(for: .milliseconds(500))
            guard isCurrentPage(Self.logsURL) else {
                continue
            }
            let content = try await evaluate("""
            JSON.stringify({
              bodyText: document.body ? document.body.innerText : '',
              hasTable: Boolean(document.querySelector('table'))
            })
            """)
            guard
                let json = content as? String,
                let data = json.data(using: .utf8),
                let page = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                let bodyText = page["bodyText"] as? String,
                let hasTable = page["hasTable"] as? Bool
            else {
                continue
            }
            if isLoginPage(bodyText) {
                throw RoutinGroupDetectionWebError.needsLogin
            }
            if RoutinGroupDetectionPageParser.isLogPageReady(
                bodyText: bodyText,
                hasTable: hasTable
            ) {
                return
            }
        }
        throw RoutinGroupDetectionWebError.pageChanged
    }

    private func load(_ url: URL) async throws {
        session.webView.load(URLRequest(url: url))
        for _ in 0..<20 {
            try await Task.sleep(for: .milliseconds(500))
            guard isCurrentPage(url) else {
                continue
            }
            let content = try await evaluate("document.body ? document.body.innerText : ''")
            let text = content as? String ?? ""
            if isLoginPage(text) {
                throw RoutinGroupDetectionWebError.needsLogin
            }
            if !text.contains("Loading...") {
                return
            }
        }
        throw RoutinGroupDetectionWebError.pageChanged
    }

    private func isCurrentPage(_ expectedURL: URL) -> Bool {
        guard let currentURL = session.webView.url else {
            return false
        }
        return currentURL.host?.lowercased() == expectedURL.host?.lowercased()
            && currentURL.path == expectedURL.path
    }

    private enum LogDetailOpeningResult {
        case opened
        case notFound
        case ambiguous
        case pageChanged
    }

    private func openLogDetail(
        marker: CodexGroupProbeRequestMarker
    ) async throws -> LogDetailOpeningResult {
        let value = try await evaluate("""
        (() => {
          const table = document.querySelector('table');
          if (!table) return JSON.stringify({ state: 'pageChanged' });
          const headers = Array.from(table.querySelectorAll('thead th')).map((cell) =>
            (cell.innerText || cell.textContent || '').trim()
          );
          const userAgentIndex = headers.findIndex((label) => label === 'User Agent');
          if (userAgentIndex < 0) return JSON.stringify({ state: 'pageChanged' });
          const matches = Array.from(table.querySelectorAll('tbody tr')).filter((row) => {
            const cells = Array.from(row.querySelectorAll('td'));
            return (cells[userAgentIndex]?.innerText || '').includes('\(marker.userAgent)');
          });
          if (matches.length === 0) return JSON.stringify({ state: 'notFound' });
          if (matches.length > 1) return JSON.stringify({ state: 'ambiguous' });
          matches[0].click();
          return JSON.stringify({ state: 'opened' });
        })()
        """)
        guard
            let json = value as? String,
            let data = json.data(using: .utf8),
            let payload = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
            let state = payload["state"] as? String
        else {
            return .pageChanged
        }

        switch state {
        case "opened":
            return .opened
        case "notFound":
            return .notFound
        case "ambiguous":
            return .ambiguous
        default:
            return .pageChanged
        }
    }

    private func readOpenedLogDetail() async throws -> String {
        for _ in 0..<60 {
            let value = try await evaluate("""
            (() => {
              const dialogs = Array.from(document.querySelectorAll('[role="dialog"]'));
              const dialog = dialogs.find((element) => {
                const text = element.innerText || '';
                return text.includes('日志详情') || text.includes('分组名称');
              });
              if (!dialog) return JSON.stringify({ state: 'loading' });

              const label = Array.from(dialog.querySelectorAll('p')).find((element) =>
                (element.innerText || '').trim() === '分组名称'
              );
              const groupName = (label?.nextElementSibling?.innerText || '').trim();
              if (!groupName) return JSON.stringify({ state: 'loading' });
              return JSON.stringify({ state: 'ready', groupName });
            })()
            """)

            if let json = value as? String {
                do {
                    return try RoutinGroupDetectionPageParser.detailGroupName(from: json)
                } catch RoutinGroupDetectionWebError.logNotFound {
                    try await Task.sleep(for: .milliseconds(500))
                    continue
                }
            }
            throw RoutinGroupDetectionWebError.pageChanged
        }
        throw RoutinGroupDetectionWebError.logTimeout
    }

    private func isLoginPage(_ text: String) -> Bool {
        let normalized = text.lowercased()
        return normalized.contains("sign in")
            || normalized.contains("login")
            || text.contains("登录你的账号")
            || text.contains("登录") && text.contains("密码")
    }

    private func evaluate(_ script: String) async throws -> Any {
        try await withCheckedThrowingContinuation { continuation in
            session.webView.evaluateJavaScript(script) { value, error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: value as Any)
                }
            }
        }
    }

    private static let accountIdentityScript = """
    (() => {
      let user = null;
      try {
        user = JSON.parse(window.localStorage.getItem('meteor_user') || 'null');
      } catch (_) {}

      const text = document.body?.innerText || '';
      const bodyEmail = (text.match(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}/i) || [''])[0];
      const email = typeof user?.email === 'string' ? user.email : bodyEmail;
      const displayName = [user?.nickname, user?.username]
        .find((value) => typeof value === 'string' && value.trim()) || '';
      return JSON.stringify({ email, displayName });
    })()
    """
}
