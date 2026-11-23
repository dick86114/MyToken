package ai.routin.mytoken.feature.settings

import ai.routin.mytoken.data.preferences.AppPreferencesRepository
import ai.routin.mytoken.domain.model.UsageHealthThresholds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Notification preferences. The master switch pauses every in-app notification
 * (usage alerts and credential failures). Percent thresholds default to the shared
 * health contract (80% warning / 95% critical) but are independently configurable
 * here; they drive both notification evaluation and the home-screen health colors.
 */
data class NotificationSettings(
    val notificationsEnabled: Boolean = true,
    val lowThresholdPercent: Int = DEFAULT_LOW_THRESHOLD_PERCENT,
    val highThresholdPercent: Int = DEFAULT_HIGH_THRESHOLD_PERCENT,
    val credentialFailureAlertsEnabled: Boolean = true,
) {
    companion object {
        val DEFAULT_LOW_THRESHOLD_PERCENT = UsageHealthThresholds.WARNING_USED_PERCENT
        val DEFAULT_HIGH_THRESHOLD_PERCENT = UsageHealthThresholds.CRITICAL_USED_PERCENT
        const val MIN_THRESHOLD_PERCENT = 1
        const val MAX_THRESHOLD_PERCENT = 99
    }
}

interface NotificationSettingsStore {
    val settings: Flow<NotificationSettings>
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setThresholds(lowPercent: Int, highPercent: Int)
    suspend fun setCredentialFailureAlertsEnabled(enabled: Boolean)
}

/** Single-source-of-truth adapter over the Task 5 [AppPreferencesRepository] DataStore. */
class AppPreferencesNotificationSettingsStore(
    private val preferencesRepository: AppPreferencesRepository,
) : NotificationSettingsStore {

    override val settings: Flow<NotificationSettings> =
        preferencesRepository.preferences.map { prefs ->
            NotificationSettings(
                notificationsEnabled = prefs.notificationsEnabled,
                lowThresholdPercent = prefs.alertLowThresholdPercent,
                highThresholdPercent = prefs.alertHighThresholdPercent,
                credentialFailureAlertsEnabled = prefs.credentialFailureAlertsEnabled,
            )
        }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        preferencesRepository.setNotificationsEnabled(enabled)
    }

    override suspend fun setThresholds(lowPercent: Int, highPercent: Int) {
        preferencesRepository.setAlertThresholds(lowPercent, highPercent)
    }

    override suspend fun setCredentialFailureAlertsEnabled(enabled: Boolean) {
        preferencesRepository.setCredentialFailureAlertsEnabled(enabled)
    }
}
