package ai.routin.mytoken.widget

import android.content.Context
import java.time.Clock

/**
 * 记录全局刷新态，供刷新图标显示进度反馈。
 * 进程被杀或网络约束一直不满足时，超时的刷新态会自动失效，避免转圈卡死。
 */
class WidgetRefreshStateStore(
    context: Context,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val preferences = context.applicationContext
        .getSharedPreferences("mytoken_widget_refresh", Context.MODE_PRIVATE)

    fun isRefreshing(): Boolean {
        if (!preferences.getBoolean(KEY_REFRESHING, false)) return false
        val elapsed = clock.millis() - preferences.getLong(KEY_STARTED_AT, 0L)
        return elapsed in 0..STALE_AFTER_MILLIS
    }

    fun setRefreshing(refreshing: Boolean) {
        preferences.edit()
            .putBoolean(KEY_REFRESHING, refreshing)
            .putLong(KEY_STARTED_AT, if (refreshing) clock.millis() else 0L)
            .apply()
    }

    private companion object {
        const val KEY_REFRESHING = "refreshing"
        const val KEY_STARTED_AT = "refreshing_started_at"
        const val STALE_AFTER_MILLIS = 5 * 60 * 1000L
    }
}
