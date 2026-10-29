package ai.routin.mytoken.security

import ai.routin.mytoken.core.security.SecretStore
import ai.routin.mytoken.feature.settings.GitHubTokenMask
import ai.routin.mytoken.feature.settings.GitHubTokenStore
import java.util.UUID

/**
 * GitHub API 令牌使用 Android Keystore 包装加密，并保存在 noBackupFilesDir。
 * 设置页只读取脱敏值，完整令牌仅在更新控制器请求 GitHub API 时使用。
 */
class KeystoreGitHubTokenStore(
    private val secretStore: SecretStore,
) : GitHubTokenStore {

    override suspend fun readToken(): String? =
        secretStore.read(TOKEN_ID)?.toString(Charsets.UTF_8)?.trim()?.takeIf { it.isNotEmpty() }

    override suspend fun saveToken(token: String) {
        val normalized = token.trim()
        require(normalized.isNotEmpty()) { "GitHub token must not be blank" }
        secretStore.save(TOKEN_ID, normalized.toByteArray(Charsets.UTF_8))
    }

    override suspend fun clearToken() {
        secretStore.delete(TOKEN_ID)
    }

    override suspend fun tokenMask(): String? = GitHubTokenMask.mask(readToken())

    private companion object {
        val TOKEN_ID = UUID.fromString("E2B4B7A0-34F4-45B3-96F4-6B0D6C4D1E31")
    }
}
