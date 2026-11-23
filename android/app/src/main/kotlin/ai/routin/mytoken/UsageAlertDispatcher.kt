package ai.routin.mytoken

import ai.routin.mytoken.core.alerts.AlertDispatchPolicy
import ai.routin.mytoken.core.alerts.AlertStateStore
import ai.routin.mytoken.core.alerts.CredentialAlertThresholdStore
import ai.routin.mytoken.core.alerts.InvalidCredentialAlert
import ai.routin.mytoken.core.alerts.MetricAlert
import ai.routin.mytoken.core.alerts.MetricAlertEvaluator
import ai.routin.mytoken.core.notifications.NotificationChannels
import ai.routin.mytoken.domain.repository.CredentialRepository
import ai.routin.mytoken.domain.usage.CredentialUsageState
import ai.routin.mytoken.feature.settings.NotificationSettingsStore
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.UUID
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * Posts usage-threshold and credential-failure notifications after a refresh.
 *
 * The in-app master switch and the system notification permission gate the whole
 * pass. Usage thresholds come from Settings (default 80/95) plus per-credential
 * overrides, and the evaluator keeps its notify-once bookkeeping. Notification
 * text carries only alias, metric label and percentages — never secrets.
 */
class UsageAlertDispatcher(
    private val context: Context,
    private val repository: CredentialRepository,
    private val states: StateFlow<Map<UUID, CredentialUsageState>>,
    private val alertStateStore: AlertStateStore,
    private val notificationSettingsStore: NotificationSettingsStore,
    private val credentialAlertThresholdStore: CredentialAlertThresholdStore,
) {

    private val evaluator = MetricAlertEvaluator()

    suspend fun dispatchAfterRefresh() {
        val settings = notificationSettingsStore.settings.first()

        val notifier = NotificationManagerCompat.from(context)
        if (!AlertDispatchPolicy.shouldDispatch(
                notificationsEnabled = settings.notificationsEnabled,
                systemNotificationsGranted = notifier.areNotificationsEnabled(),
            )
        ) {
            return
        }

        val credentials = repository.observeCredentials().first()
            .filter { it.isEnabled }
        if (credentials.isEmpty()) return

        val alertSettings = AlertDispatchPolicy.alertSettings(
            lowPercent = settings.lowThresholdPercent,
            highPercent = settings.highThresholdPercent,
            perCredential = credentialAlertThresholdStore.overrides.first(),
        )

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
            evaluation.usageAlerts.forEach { alert ->
                post(
                    notifier = notifier,
                    notification = usageNotification(alert),
                    id = notificationId(credential.id, alert.metricId),
                )
            }
            evaluation.invalidCredentialAlerts.forEach { invalid ->
                post(
                    notifier = notifier,
                    notification = invalidNotification(invalid),
                    id = notificationId(credential.id, INVALID_METRIC_KEY),
                )
            }
        }
        alertStateStore.save(state)
    }

    private fun post(notifier: NotificationManagerCompat, notification: Notification, id: Int) {
        // Double-check inside post: the permission may have been revoked mid-pass.
        if (!notifier.areNotificationsEnabled()) return
        runCatching { notifier.notify(id, notification) }
    }

    private fun usageNotification(alert: MetricAlert): Notification {
        val body = alert.notificationBody()
        return baseNotification()
            .setContentTitle(alert.notificationTitle)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setChannelId(NotificationChannels.USAGE_ALERTS_ID)
            .build()
    }

    private fun invalidNotification(alert: InvalidCredentialAlert): Notification =
        baseNotification()
            .setContentTitle("凭证已失效")
            .setContentText(alert.notificationBody())
            .setChannelId(NotificationChannels.CREDENTIAL_ERRORS_ID)
            .build()

    private fun baseNotification(): NotificationCompat.Builder =
        NotificationCompat.Builder(context, NotificationChannels.USAGE_ALERTS_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setAutoCancel(true)
            .setContentIntent(contentIntent())

    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
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
