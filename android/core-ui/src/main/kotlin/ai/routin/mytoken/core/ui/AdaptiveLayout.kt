package ai.routin.mytoken.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass

/** MyToken 的三档窗口布局模式。 */
@Immutable
enum class MyTokenLayoutMode {
    Compact,
    Medium,
    Expanded,
}

internal fun WindowWidthSizeClass.toMyTokenLayoutMode(): MyTokenLayoutMode = when (this) {
    WindowWidthSizeClass.COMPACT -> MyTokenLayoutMode.Compact
    WindowWidthSizeClass.MEDIUM -> MyTokenLayoutMode.Medium
    WindowWidthSizeClass.EXPANDED -> MyTokenLayoutMode.Expanded
    else -> MyTokenLayoutMode.Compact
}

/** 按布局模式返回各页面允许使用的最大列数。 */
fun MyTokenLayoutMode.maxColumns(
    compact: Int,
    medium: Int,
    expanded: Int,
): Int = when (this) {
    MyTokenLayoutMode.Compact -> compact
    MyTokenLayoutMode.Medium -> medium
    MyTokenLayoutMode.Expanded -> expanded
}

/** 根据可用宽度和最小项目宽度计算网格列数，空间不足时回退为单列。 */
fun adaptiveGridColumns(
    availableWidth: Dp,
    maxColumns: Int,
    minItemWidth: Dp,
    spacing: Dp,
): Int {
    if (maxColumns <= 1 || availableWidth <= 0.dp || minItemWidth <= 0.dp) return 1
    for (columns in maxColumns downTo 2) {
        val usable = availableWidth - spacing * (columns - 1)
        if (usable / columns >= minItemWidth) return columns
    }
    return 1
}

/** 读取当前窗口宽度并映射为 MyToken 的布局模式。 */
@Composable
fun rememberMyTokenLayoutMode(): MyTokenLayoutMode =
    currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass.toMyTokenLayoutMode()

/** 在大屏上限制内容最大宽度并居中，紧凑屏保持原有水平边距。 */
@Composable
fun MyTokenAdaptiveContent(
    maxWidth: Dp,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxSize(),
            content = content,
        )
    }
}
