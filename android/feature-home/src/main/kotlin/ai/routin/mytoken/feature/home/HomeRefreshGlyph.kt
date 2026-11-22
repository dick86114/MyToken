package ai.routin.mytoken.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser

/**
 * `home-latest.html` 报告刷新图标：上下两组 8 半径圆弧 + 箭头短线。
 * 24×24 viewBox、1.7 线宽；在黄色票面上使用深色 ink 反色。
 */
private const val RefreshPathData =
    "M20 7v5h-5M4 17v-5h5M6.1 6.1a8 8 0 0 1 13.2 3M4.7 14.9a8 8 0 0 0 13.2 3"

@Composable
internal fun ReportRefreshGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val path = remember { PathParser().parsePathString(RefreshPathData).toPath() }
    Canvas(modifier = modifier) {
        val factor = size.minDimension / 24f
        scale(factor, factor, pivot = Offset.Zero) {
            drawPath(
                path = path,
                color = tint,
                style = Stroke(
                    width = 1.7f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}
