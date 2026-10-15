package ai.routin.mytoken.domain.usage

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialSecret

/**
 * 用持久化的登录态静默续期供应商会话。
 *
 * 小米 MiMo 这类控制台只认短期服务票据，票据过期后接口返回 401；但底层的小米账号
 * 登录态（passToken 等）通常仍然有效，可以不带界面地重新换取新票据。返回 null 表示
 * 无法静默续期，调用方应保持原有的“需要重新登录”提示。
 */
fun interface CredentialSessionRenewer {
    suspend fun renew(credential: Credential, previousSecret: CredentialSecret): CredentialSecret?
}
