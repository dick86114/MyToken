package ai.routin.mytoken.domain.model

/**
 * Which alert settings the credential editor exposes for a provider/usage kind.
 * Shared by the editor and the credential detail screen so the two visibility
 * rules cannot drift apart.
 */
object CredentialAlertCapabilities {

    /** Per-credential percent thresholds only apply to credentials with percent metrics. */
    fun supportsPercentAlertThreshold(providerId: ProviderId, usageKind: String?): Boolean =
        when (providerId) {
            ProviderId.DeepSeek -> false
            ProviderId.Xiaomi -> usageKind?.trim()?.lowercase() == "plan"
            else -> true
        }

    /** Absolute low-balance warnings are only meaningful for balance-bearing accounts. */
    fun supportsBalanceWarningThreshold(providerId: ProviderId, usageKind: String?): Boolean =
        when (providerId) {
            ProviderId.DeepSeek,
            ProviderId.Glm,
            ProviderId.NewAPI,
            -> true
            ProviderId.Xiaomi -> usageKind?.trim()?.lowercase() == "api"
            else -> false
        }
}
