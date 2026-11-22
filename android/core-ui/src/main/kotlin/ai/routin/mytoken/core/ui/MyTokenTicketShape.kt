package ai.routin.mytoken.core.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * 票据切角：左上、右下保持 20dp 圆角，右上（可选左下）各切掉 20dp。
 * 只作为 background 的 Shape 使用，不用 Modifier.clip，避免吞掉内部点击。
 */
class MyTokenTicketShape(
    private val cutCorner: Boolean,
    private val roundBottomEnd: Boolean = false,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val limit = minOf(size.width, size.height) / 2f
        fun Dp.px(): Float = with(density) { toPx() }.coerceIn(0f, limit)
        val cut = 20.dp.px()
        val topStart = 20.dp.px()
        val bottomEnd = if (roundBottomEnd) 20.dp.px() else 0f
        val path = Path().apply {
            moveTo(topStart, 0f)
            lineTo(size.width - cut, 0f)
            lineTo(size.width, cut)
            if (cutCorner) {
                lineTo(size.width, size.height - bottomEnd)
                arcTo(
                    rect = Rect(
                        size.width - bottomEnd * 2f,
                        size.height - bottomEnd * 2f,
                        size.width,
                        size.height,
                    ),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false,
                )
                lineTo(cut, size.height)
                lineTo(0f, size.height - cut)
            } else {
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
            }
            lineTo(0f, topStart)
            arcTo(
                rect = Rect(0f, 0f, topStart * 2f, topStart * 2f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
            close()
        }
        return Outline.Generic(path)
    }
}

/** 报告/铭牌主体：右上、左下切角，左上、右下圆角。 */
val MyTokenReportBodyShape: Shape = MyTokenTicketShape(cutCorner = true, roundBottomEnd = true)

/** 报告/铭牌黄色票面：仅右上切角，其余圆角。 */
val MyTokenReportTicketShape: Shape = MyTokenTicketShape(cutCorner = false)
