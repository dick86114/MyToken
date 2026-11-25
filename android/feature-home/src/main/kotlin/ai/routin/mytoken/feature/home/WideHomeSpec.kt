package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

/** 报告排布：横屏（宽 > 高）左右横排，竖屏（含 1:1）竖排。 */
internal enum class WideReportOrientation { Horizontal, Vertical }

/**
 * 大屏三栏的尺寸单一来源。
 *
 * 详情抽屉与设置页共用同一套宽度规则：窗口宽度的 88%，上限 420dp。
 *
 * 卡片宽度以手机竖屏两档卡片为基准（简洁 176dp / 完整 360dp），
 * 只允许最多 12% 的拉伸来铺满行，避免大屏卡片被拉得过宽。
 */
internal object WideHomeSpec {
    val railWidthMedium = 64.dp
    val railWidthExpanded = 72.dp

    val compactBaseCardWidth = 176.dp
    val fullBaseCardWidth = 360.dp
    val compactCardSpacing = 8.dp
    val fullCardSpacing = 12.dp
    const val maxCardStretch = 1.12f
    const val maxCardColumns = 8

    /** 与 `MyTokenEdgeDrawerHost` 的设置/凭证抽屉保持一致。 */
    val maxDrawerWidth = 420.dp
    const val drawerWidthFraction = 0.88f

    val normalReportHeight = 80.dp
    val alertReportHeight = 110.dp

    /** 报告下方的密度/操作条高度；密度开关始终横排右对齐。 */
    val reportControlsHeight = 44.dp

    fun railWidth(layoutMode: MyTokenLayoutMode): Dp =
        if (layoutMode == MyTokenLayoutMode.Expanded) railWidthExpanded else railWidthMedium

    fun drawerWidth(availableWidth: Dp): Dp =
        minOf(availableWidth * drawerWidthFraction, maxDrawerWidth)

    fun reportOrientation(isLandscape: Boolean): WideReportOrientation =
        if (isLandscape) WideReportOrientation.Horizontal else WideReportOrientation.Vertical

    /**
     * 以手机竖屏卡宽为基准，按可用宽度选列数；最多拉伸 12% 来铺满整行。
     * 动态字体放大时基准卡宽同步放大，避免大字号下文字被压缩。
     */
    fun columnCount(
        availableWidth: Dp,
        baseWidth: Dp,
        spacing: Dp,
        fontScale: Float,
    ): Int {
        if (availableWidth <= 0.dp || baseWidth <= 0.dp) return 1
        val scaledBase = baseWidth * fontScale.coerceAtLeast(1f)
        val maxWidth = scaledBase * maxCardStretch
        val columns = ceil(((availableWidth + spacing) / (maxWidth + spacing)).toFloat()).toInt()
        return columns.coerceIn(1, maxCardColumns)
    }

    fun compactColumns(availableWidth: Dp, fontScale: Float): Int = columnCount(
        availableWidth = availableWidth,
        baseWidth = compactBaseCardWidth,
        spacing = compactCardSpacing,
        fontScale = fontScale,
    )

    fun fullColumns(availableWidth: Dp, fontScale: Float): Int = columnCount(
        availableWidth = availableWidth,
        baseWidth = fullBaseCardWidth,
        spacing = fullCardSpacing,
        fontScale = fontScale,
    )
}
