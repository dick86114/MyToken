package ai.routin.mytoken.domain.usage

import ai.routin.mytoken.domain.model.UsageHealthThresholdConfig
import java.util.UUID

/**
 * Resolves the effective health thresholds for a credential at call time. The app
 * implementation reads the current global setting plus the per-credential override,
 * so a threshold change applies immediately without rebuilding the refresh use case.
 */
interface UsageHealthThresholdSource {
    fun thresholdsFor(credentialId: UUID): UsageHealthThresholdConfig
}

/** Defaults used by tests and by callers that have no settings wiring. */
object DefaultUsageHealthThresholdSource : UsageHealthThresholdSource {
    override fun thresholdsFor(credentialId: UUID): UsageHealthThresholdConfig =
        UsageHealthThresholdConfig.DEFAULT
}
