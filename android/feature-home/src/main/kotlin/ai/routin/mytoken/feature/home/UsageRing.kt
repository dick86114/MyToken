package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenMotion
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.core.ui.rememberMyTokenMotionEnabled
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 唯一的弧线几何：270°，从 135° 起顺时针，圆头，无渐变、无发光。 */
private const val ArcStartAngle = 135f
private const val ArcSweepAngle = 270f
private const val ArcZeroSweepAngle = 0.9f
private val ArcStrokeWidth = 3.5.dp

@Composable
internal fun UsageArcGauge(
    percent: Double?,
    tone: UsageMetricTone,
    diameter: Dp,
    inactive: Boolean = false,
) {
    val colors = MyTokenThemeTokens.colors
    val accent = when {
        inactive -> colors.track
        tone == UsageMetricTone.Warning -> colors.warningFill
        tone == UsageMetricTone.Critical -> colors.criticalFill
        else -> colors.positiveFill
    }
    val clamped = ((percent ?: 0.0).coerceIn(0.0, 100.0)).toFloat()
    // 数值变化时弧线平滑过渡；系统关闭动画时退化为瞬间跳变。
    val motionEnabled = rememberMyTokenMotionEnabled()
    val animatedPercent by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(
            durationMillis = MyTokenMotion.ringDurationMillis(motionEnabled),
            easing = FastOutSlowInEasing,
        ),
        label = "usageArcPercent",
    )
    val trackStroke = ArcStrokeWidth
    // 动态字体放大时同步放大弧线，避免圆心数字被 52dp 直径裁切。
    val scaledDiameter = diameter * LocalDensity.current.fontScale.coerceIn(1f, 1.35f)
    val numberStyle = if (scaledDiameter >= 64.dp) MyTokenType.ringFull else MyTokenType.ringCompact
    val numberText = CompactUsageCardPresentation.compactPercentNumberText(percent)
    val contentColor = if (inactive) colors.muted else colors.ink

    Box(
        modifier = Modifier.size(scaledDiameter),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val inset = trackStroke.toPx() / 2f
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            drawArc(
                color = colors.track,
                startAngle = ArcStartAngle,
                sweepAngle = ArcSweepAngle,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = trackStroke.toPx(), cap = StrokeCap.Round),
            )

            val progressSweep = if (animatedPercent <= 0f) {
                ArcZeroSweepAngle
            } else {
                ArcSweepAngle * animatedPercent / 100f
            }
            drawArc(
                color = accent,
                startAngle = ArcStartAngle,
                sweepAngle = progressSweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = trackStroke.toPx(), cap = StrokeCap.Round),
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = numberText,
                style = numberStyle,
                color = contentColor,
                maxLines = 1,
            )
            Text(
                text = "%",
                style = MyTokenType.percentMark,
                color = contentColor,
                modifier = Modifier
                    .padding(start = 1.dp)
                    .offset(y = (-1).dp),
                maxLines = 1,
            )
        }
    }
}
