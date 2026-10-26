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

            return ProviderShareBranding(
                logoRes = ProviderCatalog.logoRes(providerId),
                websiteUrl = value,
                websiteDisplay = value.removeSuffix("/"),
            )
        }
    }
}
