import Foundation

/// GitHub 访问令牌只保存在本机；备份设置不允许携带该密钥材料。
protocol GitHubTokenStoring: Sendable {
    func readToken() -> String?
    func saveToken(_ token: String) throws
    func removeToken()
    func tokenMask() -> String?
}

struct KeychainGitHubTokenStore: GitHubTokenStoring {
    private let keychain: KeychainSecretStore
    private let tokenID: UUID

    init(
        keychain: KeychainSecretStore = KeychainSecretStore(
            service: "ai.routin.usage-monitor.github-token"
        ),
        tokenID: UUID = UUID(uuidString: "E2B4B7A0-34F4-45B3-96F4-6B0D6C4D1E31")!
    ) {
        self.keychain = keychain
        self.tokenID = tokenID
    }

    func readToken() -> String? {
        let token = try? keychain.read(for: tokenID)
        return token?.trimmingCharacters(in: .whitespacesAndNewlines).nonEmpty
    }

    func saveToken(_ token: String) throws {
        guard let normalized = token.trimmingCharacters(in: .whitespacesAndNewlines).nonEmpty else {
            removeToken()
            return
        }
        try keychain.save(normalized, for: tokenID)
    }

    func removeToken() {
        try? keychain.delete(for: tokenID)
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
