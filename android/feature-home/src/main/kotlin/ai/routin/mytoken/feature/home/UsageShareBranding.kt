package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.ProviderId

internal data class ProviderShareBranding(
    val logoRes: Int,
    val websiteUrl: String,
    val websiteDisplay: String,
) {
    companion object {
        fun make(
            providerId: ProviderId,
            websiteUrl: String?,
        ): ProviderShareBranding? {
            val value = websiteUrl?.trim().orEmpty()
            if (value.isEmpty()) return null

            val logoRes = when (providerId) {
                ProviderId.Routin -> R.drawable.share_provider_routin_logo
                ProviderId.DeepSeek -> R.drawable.share_provider_deepseek_logo
                ProviderId.Glm -> R.drawable.share_provider_glm_logo
                ProviderId.Volcengine -> R.drawable.share_provider_volcengine_logo
                ProviderId.NewAPI -> R.drawable.share_provider_newapi_logo
                ProviderId.CommandCode -> R.drawable.share_provider_commandcode_logo
                ProviderId.Xiaomi -> R.drawable.share_provider_xiaomi_logo
                ProviderId.OpenCode -> R.drawable.share_provider_opencode_logo
            }
            return ProviderShareBranding(
                logoRes = logoRes,
                websiteUrl = value,
                websiteDisplay = value.removeSuffix("/"),
            )
        }
    }
}
