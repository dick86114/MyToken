package ai.routin.mytoken.widget

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** 应用内刷新后合并成一波再重绘组件，避免“主应用更新了但组件还是旧数据”。 */
internal const val WIDGET_SYNC_DEBOUNCE_MILLIS = 1_500L

/**
 * 监听用量状态变化，静默 [debounceMillis] 之后统一重绘一次两种组件。
 * 连续多次状态推送只会触发一次重绘。
 */
internal fun CoroutineScope.syncWidgetsOnUsageChanges(
    states: Flow<*>,
    debounceMillis: Long = WIDGET_SYNC_DEBOUNCE_MILLIS,
    redrawAll: suspend () -> Unit,
): Job = launch {
    states.collectLatest {
        delay(debounceMillis)
        runCatching { redrawAll() }
    }
}
