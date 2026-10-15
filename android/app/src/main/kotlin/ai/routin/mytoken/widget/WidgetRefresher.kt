package ai.routin.mytoken.widget

import android.appwidget.AppWidgetManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager

/** User-initiated widget refreshes reuse the app's single background refresh delegate. */
internal object WidgetRefresher {

    const val ACTION_REFRESH = "ai.routin.mytoken.action.WIDGET_REFRESH"
    const val EXTRA_WIDGET_ID = "appWidgetId"
    private const val UNIQUE_WORK_NAME = "mytoken-widget-refresh-all"

    /**
     * 同一时刻只保留一个全局刷新任务。KEEP 语义保证连点刷新不会取消正在进行的
     * 网络请求，也不会重复排队，任务结束后再次点击即可重新入队。
     */
    val existingWorkPolicy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP

    fun refreshPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, CredentialWidgetProvider::class.java)
            .setAction(ACTION_REFRESH)
            .putExtra(EXTRA_WIDGET_ID, appWidgetId)
        return PendingIntent.getBroadcast(
            context,
            appWidgetId + 200_000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            existingWorkPolicy,
            request,
        )
    }
}
