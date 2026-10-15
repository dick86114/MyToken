package ai.routin.mytoken.widget

/**
 * 保存组件配置：同步落盘并入队刷新后立即返回。
 * 首次添加组件时 Glance 直接重绘可能被启动器拒绝，因此重绘是独立的尽力步骤，
 * 由调用方放到应用级作用域执行，不阻塞配置页返回桌面。
 */
internal class WidgetConfigurationSaveUseCase(
    private val save: (Int, WidgetConfig) -> Unit,
    private val enqueueRefresh: () -> Unit,
    private val redrawWidget: suspend (Int) -> Unit,
) {
    fun apply(appWidgetId: Int, config: WidgetConfig) {
        save(appWidgetId, config)
        enqueueRefresh()
    }

    suspend fun redraw(appWidgetId: Int) {
        runCatching { redrawWidget(appWidgetId) }
    }
}
