package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.adaptiveGridColumns
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 报告的两种排布：16:9 横排，其余比例竖排。 */
internal enum class WideReportOrientation { Horizontal, Vertical }

/**
 * 大屏三栏的尺寸单一来源。
 *
 * 宽度按 [MyTokenLayoutMode] 与窗口宽高比推导，不按设备型号硬编码：
 * 16:9 → 380dp，1:1 → 360dp，其余（4:3 / 3:4 / 9:16）→ 340dp。
 */
internal object WideHomeSpec {
    val railWidthMedium = 64.dp
    val railWidthExpanded = 72.dp

    val cardMinWidth = 250.dp
    val cardSpacing = 10.dp
    const val maxCardColumns = 6

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

    fun reportOrientation(aspectRatio: Float): WideReportOrientation =
        if (aspectRatio >= wideRatio) {
            WideReportOrientation.Horizontal
        } else {
            WideReportOrientation.Vertical
        }

    /**
     * 卡片列宽不低于 250dp，列数随中间栏可用宽度自适应。
     * 动态字体放大到 1.3× 时退回单列，和手机首页的规则一致。
     */
    fun columnCount(availableWidth: Dp, fontScale: Float): Int {
        if (fontScale >= 1.3f) return 1
        return adaptiveGridColumns(
            availableWidth = availableWidth,
            maxColumns = maxCardColumns,
            minItemWidth = cardMinWidth,
            spacing = cardSpacing,
        )
    }
}
