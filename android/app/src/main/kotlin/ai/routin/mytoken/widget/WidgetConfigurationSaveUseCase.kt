package ai.routin.mytoken.widget

/** Applies a widget selection, schedules data refresh, then redraws the widget. */
internal class WidgetConfigurationSaveUseCase(
    private val save: (Int, WidgetConfig) -> Unit,
    private val enqueueRefresh: () -> Unit,
    private val updateWidget: suspend (Int) -> Unit,
) {
    suspend fun apply(appWidgetId: Int, config: WidgetConfig) {
        save(appWidgetId, config)
        enqueueRefresh()
        // First-time widget registration can reject a direct Glance update on some
        // launchers; the enqueued refresh remains the durable path and will redraw.
        runCatching { updateWidget(appWidgetId) }
    }
}
