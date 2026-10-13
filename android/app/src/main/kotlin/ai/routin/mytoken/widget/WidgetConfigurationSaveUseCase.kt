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
        updateWidget(appWidgetId)
    }
}
