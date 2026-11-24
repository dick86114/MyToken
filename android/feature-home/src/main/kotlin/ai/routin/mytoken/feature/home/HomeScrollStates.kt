package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenMotion

import ai.routin.mytoken.domain.model.UsageCardDensity
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import kotlin.math.abs
import kotlinx.coroutines.delay

/** 首页三种滚动容器各自持有状态，离开首页再返回时保留位置。 */
@Stable
class HomeScrollStates(
    val compact: LazyStaggeredGridState = LazyStaggeredGridState(),
    val full: LazyListState = LazyListState(),
    val fullWide: LazyGridState = LazyGridState(),
)

@Composable
fun rememberHomeScrollStates(): HomeScrollStates {
    val compact = rememberLazyStaggeredGridState()
    val full = rememberLazyListState()
    val fullWide = rememberLazyGridState()
    return remember(compact, full, fullWide) { HomeScrollStates(compact, full, fullWide) }
}

internal enum class HomeScrollMode { CompactGrid, FullList, FullWideGrid }

internal fun homeScrollMode(
    density: UsageCardDensity,
    screenWidthDp: Int,
): HomeScrollMode = when {
    density.isCompact() -> HomeScrollMode.CompactGrid
    screenWidthDp >= 700 -> HomeScrollMode.FullWideGrid
    else -> HomeScrollMode.FullList
}

internal fun homeGridColumns(
    mode: HomeScrollMode,
    screenWidthDp: Int,
    fontScale: Float,
): Int = when (mode) {
    HomeScrollMode.FullList -> 1
    HomeScrollMode.FullWideGrid -> 2
    HomeScrollMode.CompactGrid -> when {
        // 动态字体放大到 1.3× 时，简洁模式退回单列，避免文字被压缩。
        fontScale >= 1.3f -> 1
        screenWidthDp >= 840 -> 4
        screenWidthDp >= 600 -> 3
        else -> 2
    }
}

internal fun homeGridTag(mode: HomeScrollMode, columns: Int): String = when (mode) {
    HomeScrollMode.FullList -> "home_list_full"
    else -> "home_grid_${columns}_columns"
}

/** 报告风险项定位：滚动到目标卡片并把卡片中心对准屏幕中心（含固定头部偏移）。 */
internal suspend fun locateCard(
    states: HomeScrollStates,
    mode: HomeScrollMode,
    index: Int,
    centerBiasPx: Float,
    durationMillis: Int = MyTokenMotion.locateMillis,
) {
    when (mode) {
        HomeScrollMode.CompactGrid ->
            locateInStaggeredGrid(states.compact, index, centerBiasPx, durationMillis)
        HomeScrollMode.FullWideGrid -> locateInGrid(states.fullWide, index, centerBiasPx, durationMillis)
        HomeScrollMode.FullList -> locateInList(states.full, index, centerBiasPx, durationMillis)
    }
}

internal suspend fun locateInStaggeredGrid(
    state: LazyStaggeredGridState,
    index: Int,
    centerBiasPx: Float,
    durationMillis: Int,
) {
    val viewportHeight = state.layoutInfo.viewportSize.height
    state.animateScrollToItem(index, -viewportHeight / 3)
    withFrameNanos { }
    var item = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (item == null) {
        delay(16)
        item = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    }
    item ?: return
    val viewportCenter = state.layoutInfo.viewportSize.height / 2f
    val itemCenter = item.offset.y + item.size.height / 2f
    val delta = itemCenter - (viewportCenter - centerBiasPx)
    if (abs(delta) > 1f) {
        state.animateScrollBy(delta, tween(durationMillis, easing = FastOutSlowInEasing))
    }
}

private suspend fun locateInGrid(state: LazyGridState, index: Int, centerBiasPx: Float, durationMillis: Int) {
    val viewportHeight = state.layoutInfo.viewportSize.height
    state.animateScrollToItem(index, -viewportHeight / 3)
    withFrameNanos { }
    var item = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (item == null) {
        delay(16)
        item = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    }
    item ?: return
    val viewportCenter = state.layoutInfo.viewportSize.height / 2f
    val itemCenter = item.offset.y + item.size.height / 2f
    val delta = itemCenter - (viewportCenter - centerBiasPx)
    if (abs(delta) > 1f) {
        state.animateScrollBy(delta, tween(durationMillis, easing = FastOutSlowInEasing))
    }
}

private suspend fun locateInList(state: LazyListState, index: Int, centerBiasPx: Float, durationMillis: Int) {
    val viewportHeight = state.layoutInfo.viewportSize.height
    state.animateScrollToItem(index, -viewportHeight / 3)
    withFrameNanos { }
    var item = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (item == null) {
        delay(16)
        item = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    }
    item ?: return
    val viewportCenter = state.layoutInfo.viewportSize.height / 2f
    val itemCenter = item.offset + item.size / 2f
    val delta = itemCenter - (viewportCenter - centerBiasPx)
    if (abs(delta) > 1f) {
        state.animateScrollBy(delta, tween(durationMillis, easing = FastOutSlowInEasing))
    }
}
