package ai.routin.mytoken.core.alerts

import java.util.UUID

/**
 * Pure assembly rules used by the Android usage-alert dispatcher, kept free of
 * Android framework types so the gate and threshold mapping are unit-testable.
 */
object AlertDispatchPolicy {

    /** Every app notification is gated by the in-app master switch and the system permission. */
    fun shouldDispatch(notificationsEnabled: Boolean, systemNotificationsGranted: Boolean): Boolean =
        notificationsEnabled && systemNotificationsGranted

    /**
     * Builds the evaluator settings from the user's global thresholds plus the
     * per-credential overrides. Invalid persisted values fall back to the shared
     * 80/95 defaults instead of crashing the background dispatcher.
     */
    fun alertSettings(
        lowPercent: Int,
        highPercent: Int,
        perCredential: Map<UUID, AlertThresholds> = emptyMap(),
    ): MetricAlertSettings {
        val thresholds = runCatching { AlertThresholds(lowPercent, highPercent) }
            .getOrDefault(AlertThresholds.DEFAULT)
        return MetricAlertSettings(thresholds = thresholds, perCredential = perCredential)
    }
}
