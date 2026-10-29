import Foundation
/// GitHub 访问令牌只保存在本机；备份设置不允许携带该密钥材料。
protocol GitHubTokenStoring: Sendable {
    func readToken() -> String?
    func saveToken(_ token: String) throws
    func removeToken()
    func tokenMask() -> String?
}

/// GitHub 令牌保存在应用偏好存储中，避免启动时触发系统钥匙串授权。
final class AppLocalGitHubTokenStore: GitHubTokenStoring, @unchecked Sendable {
    private let defaults: UserDefaults
    private let storageKey: String

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        self.storageKey = "github.update.token"
    }

    func readToken() -> String? {
        defaults.string(forKey: storageKey)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .nonEmpty
    }

    func saveToken(_ token: String) throws {
        guard let normalized = token.trimmingCharacters(in: .whitespacesAndNewlines).nonEmpty else {
            removeToken()
            return
        }
        defaults.set(normalized, forKey: storageKey)
    }

    func removeToken() {
        defaults.removeObject(forKey: storageKey)
    }

    func tokenMask() -> String? {
        GitHubTokenMask.mask(readToken())
    }
}

/// UI 只展示前缀和末四位，避免完整令牌留在视图状态或辅助功能里。
enum GitHubTokenMask {
    static func mask(_ token: String?) -> String? {
        guard let token, !token.isEmpty else { return nil }
        guard token.count > 8 else {
            return String(repeating: "•", count: token.count)
        }
        return "\(token.prefix(4))…\(token.suffix(4))"
    }
}

private extension String {
    var nonEmpty: String? {
        isEmpty ? nil : self
    }
}
