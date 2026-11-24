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
 * 详情宽度按窗口宽高比推导，不按设备型号硬编码：
 * 16:9 → 380dp，1:1 → 360dp，其余（4:3 / 3:4 / 9:16）→ 340dp。
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

    val detailWidthWide = 380.dp
    val detailWidthSquare = 360.dp
    val detailWidthNarrow = 340.dp

    val normalReportHeight = 80.dp
    val alertReportHeight = 110.dp

    /** 1:1 附近视为方形窗口；其余宽高比走 340dp。 */
    private val squareRatio = 0.9f..1.1f
    private const val wideRatio = 1.5f

    fun railWidth(layoutMode: MyTokenLayoutMode): Dp =
        if (layoutMode == MyTokenLayoutMode.Expanded) railWidthExpanded else railWidthMedium

    fun detailWidth(aspectRatio: Float): Dp = when {
        aspectRatio >= wideRatio -> detailWidthWide
        aspectRatio in squareRatio -> detailWidthSquare
        else -> detailWidthNarrow
    }

    fun reportOrientation(isLandscape: Boolean): WideReportOrientation =
        if (isLandscape) WideReportOrientation.Horizontal else WideReportOrientation.Vertical

    /** “全部供应商”合成图标的列数：1 个居中；2–4 个 2×2；更多 3 列自适应行数。 */
    fun allProviderLogoColumns(count: Int): Int = when {
        count <= 1 -> 1
        count <= 4 -> 2
        else -> 3
    }

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
