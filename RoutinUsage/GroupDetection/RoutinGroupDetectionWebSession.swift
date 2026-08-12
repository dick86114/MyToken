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
        let displayName: String
    }

    private struct LogRow: Decodable {
        let userAgent: String
        let groupName: String
    }

    static func accountIdentity(from json: String) -> RoutinAccountIdentity? {
        guard
            let data = json.data(using: .utf8),
            let payload = try? JSONDecoder().decode(AccountPayload.self, from: data),
            !payload.email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
            !payload.displayName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        else {
            return nil
        }
        return RoutinAccountIdentity.make(email: payload.email, displayName: payload.displayName)
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

        let matches = rows.filter { $0.userAgent == marker.userAgent }
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
        let content = try await evaluate("""
        (() => {
          const text = document.body?.innerText || '';
          const email = (text.match(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}/i) || [''])[0];
          const usernameLabel = Array.from(document.querySelectorAll('*')).find((element) => {
            return (element.textContent || '').trim() === '用户名';
          });
          const displayName = usernameLabel?.parentElement?.innerText
            ?.split('\n')
            .map((value) => value.trim())
            .find((value) => value && value !== '用户名') || '';
          return JSON.stringify({ email, displayName });
        })()
        """)
        guard
            let json = content as? String,
            let identity = RoutinGroupDetectionPageParser.accountIdentity(from: json)
        else {
            throw RoutinGroupDetectionWebError.accountUnavailable
        }
        return identity
    }

    func findGroupName(marker: CodexGroupProbeRequestMarker) async throws -> String {
        try await load(Self.logsURL)

        for _ in 0..<15 {
            if let json = try await logRowsJSON() {
                do {
                    return try RoutinGroupDetectionPageParser.groupName(from: json, marker: marker)
                } catch RoutinGroupDetectionWebError.logNotFound {
                    try await Task.sleep(for: .seconds(2))
                    continue
                }
            }
            throw RoutinGroupDetectionWebError.pageChanged
        }

        throw RoutinGroupDetectionWebError.logTimeout
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

    private func logRowsJSON() async throws -> String? {
        let value = try await evaluate("""
        (() => {
          const table = document.querySelector('table');
          if (!table) return null;
          const headers = Array.from(table.querySelectorAll('thead th')).map((cell) =>
            (cell.innerText || cell.textContent || '').trim()
          );
          const userAgentIndex = headers.findIndex((label) => label === 'User Agent');
          const tokenIndex = headers.findIndex((label) => label === '令牌');
          if (userAgentIndex < 0 || tokenIndex < 0) return null;

          return JSON.stringify(Array.from(table.querySelectorAll('tbody tr')).map((row) => {
            const cells = Array.from(row.querySelectorAll('td'));
            const tokenText = (cells[tokenIndex]?.innerText || '').trim().split('\n')
              .map((value) => value.trim())
              .filter(Boolean);
            return {
              userAgent: (cells[userAgentIndex]?.innerText || '').trim(),
              groupName: tokenText.length > 1 ? tokenText[tokenText.length - 1] : ''
            };
          }));
        })()
        """)
        return value as? String
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
}
