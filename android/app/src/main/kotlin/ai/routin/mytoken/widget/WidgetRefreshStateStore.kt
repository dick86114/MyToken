package ai.routin.mytoken.widget

import android.content.Context

/** Tracks the single global widget refresh so refresh icons can show progress feedback. */
class WidgetRefreshStateStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("mytoken_widget_refresh", Context.MODE_PRIVATE)

    fun isRefreshing(): Boolean = preferences.getBoolean(KEY_REFRESHING, false)

    fun setRefreshing(refreshing: Boolean) {
        preferences.edit().putBoolean(KEY_REFRESHING, refreshing).apply()
    }

    private companion object {
        const val KEY_REFRESHING = "refreshing"
    }
}
