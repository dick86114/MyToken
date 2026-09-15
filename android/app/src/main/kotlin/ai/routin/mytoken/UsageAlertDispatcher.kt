package ai.routin.mytoken

import ai.routin.mytoken.core.alerts.AlertStateStore
import ai.routin.mytoken.core.alerts.AlertThresholds
import ai.routin.mytoken.core.alerts.InvalidCredentialAlert
import ai.routin.mytoken.core.alerts.MetricAlertEvaluator
import ai.routin.mytoken.core.alerts.MetricAlertSettings
import ai.routin.mytoken.core.notifications.NotificationChannels
import ai.routin.mytoken.domain.repository.CredentialRepository
import ai.routin.mytoken.domain.usage.CredentialUsageState
import ai.routin.mytoken.feature.settings.NotificationSettingsStore
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.UUID
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * 当前只投递凭证失效提醒；系统权限关闭时静默跳过。通知内容不包含密钥。
 */
class UsageAlertDispatcher(
    private val context: Context,
    private val repository: CredentialRepository,
    private val states: StateFlow<Map<UUID, CredentialUsageState>>,
    private val alertStateStore: AlertStateStore,
    private val notificationSettingsStore: NotificationSettingsStore,
) {

    private val evaluator = MetricAlertEvaluator()

    suspend fun dispatchAfterRefresh() {
        val settings = notificationSettingsStore.settings.first()

        val notifier = NotificationManagerCompat.from(context)
        if (!notifier.areNotificationsEnabled()) return

        val credentials = repository.observeCredentials().first()
            .filter { it.isEnabled }
        if (credentials.isEmpty()) return

        val alertSettings = MetricAlertSettings(thresholds = AlertThresholds.DEFAULT)

        var state = alertStateStore.load()
        credentials.forEach { credential ->
            val evaluation = evaluator.evaluate(
                credential = credential,
                usageState = states.value[credential.id],
                settings = alertSettings,
                previousState = state,
                credentialFailureAlertsEnabled = settings.credentialFailureAlertsEnabled,
            )
            state = evaluation.state
            evaluation.invalidCredentialAlerts.forEach { invalid ->
                post(notifier, invalidNotification(invalid), notificationId(credential.id, INVALID_METRIC_KEY))
            }
        }
        alertStateStore.save(state)
    }

    private fun post(notifier: NotificationManagerCompat, notification: android.app.Notification, id: Int) {
        // Double-check inside post: the permission may have been revoked mid-pass.
        if (!notifier.areNotificationsEnabled()) return
        runCatching { notifier.notify(id, notification) }
    }

    private fun invalidNotification(alert: InvalidCredentialAlert) =
        baseNotification()
            .setContentTitle("凭证已失效")
            .setContentText(alert.notificationBody())
            .setChannelId(NotificationChannels.CREDENTIAL_ERRORS_ID)
            .build()

    private fun baseNotification() =
        NotificationCompat.Builder(context, NotificationChannels.USAGE_ALERTS_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setAutoCancel(true)
            .setContentIntent(contentIntent())

    private fun contentIntent() = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun notificationId(credentialId: UUID, metricKey: String): Int =
        "$credentialId/$metricKey".hashCode()

    companion object {
        private const val INVALID_METRIC_KEY = "__invalid__"
    }
}
