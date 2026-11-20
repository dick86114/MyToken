package ai.routin.mytoken.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 简洁卡片环形用量：数字放大突出，百分号缩小上移。 */
@Composable
internal fun UsageRing(
    percent: Double?,
    tone: UsageMetricTone,
    isDark: Boolean,
    diameter: Dp,
) {
    val colors = statusColors()
    val accent = when (tone) {
        UsageMetricTone.Warning -> colors.warning
        UsageMetricTone.Critical -> colors.critical
        UsageMetricTone.Normal -> colors.brand
    }
    val track = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.08f)
    val strokeWidth = diameter * 0.10f
    val clamped = ((percent ?: 0.0).coerceIn(0.0, 100.0)).toFloat()

    val numberSize = (if (diameter > 42.dp) 13.5 else 11.5).sp
    val percentSize = (if (diameter > 42.dp) 10.5 else 9.0).sp
    val numberWeight = if ((percent ?: 0.0) <= 0.0) FontWeight.SemiBold else FontWeight.Bold
    // 状态色只用于圆环本体；数值保留常规前景色以维持易读性。
    val labelColor = if ((percent ?: 0.0) <= 0.0) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
    }
    val numberText = CompactUsageCardPresentation.compactPercentNumberText(percent)

    Box(modifier = Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = size.copy(width = size.width - stroke, height = size.height - stroke)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (clamped > 0f) {
                drawArc(
                    color = accent.copy(alpha = if (isDark) 0.20f else 0.13f),
                    startAngle = -90f,
                    sweepAngle = 360f * clamped / 100f,
                    useCenter = true,
                    topLeft = androidx.compose.ui.geometry.Offset(inset + stroke * 1.5f, inset + stroke * 1.5f),
                    size = androidx.compose.ui.geometry.Size(
                        width = (arcSize.width - stroke * 3f).coerceAtLeast(0f),
                        height = (arcSize.height - stroke * 3f).coerceAtLeast(0f),
                    ),
                )
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * clamped / 100f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = numberText,
                style = TextStyle(
                    fontSize = numberSize,
                    fontWeight = numberWeight,
                    fontFamily = FontFamily.Monospace,
                ),
                color = labelColor,
                maxLines = 1,
            )
            Text(
                text = "%",
                style = TextStyle(
                    fontSize = percentSize,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                ),
                color = labelColor,
                modifier = Modifier
                    .padding(start = 1.dp)
                    .offset(y = (-1).dp),
                maxLines = 1,
            )
        }
    }
}

/**
 * 简洁模式方案 A：270° 细弧。数字仍居中，但圆弧不再闭合成完整仪表盘，
 * 轨道的视觉重量也更低；状态色只出现在进度弧上。
 */
@Composable
internal fun CompactArcGauge(
    percent: Double?,
    tone: UsageMetricTone,
    isDark: Boolean,
    diameter: Dp,
) {
    val colors = statusColors()
    val accent = when (tone) {
        UsageMetricTone.Warning -> colors.warning
        UsageMetricTone.Critical -> colors.critical
        UsageMetricTone.Normal -> colors.brand
    }
    val track = if (isDark) Color.White.copy(alpha = 0.065f) else Color.Black.copy(alpha = 0.065f)
    val clamped = ((percent ?: 0.0).coerceIn(0.0, 100.0)).toFloat()
    val trackStroke = if (diameter > 50.dp) 2.4.dp else 2.1.dp
    val progressStroke = if (diameter > 50.dp) 3.1.dp else 2.7.dp
    val numberSize = if (diameter > 50.dp) 14.sp else 12.5.sp
    val percentSize = if (diameter > 50.dp) 9.sp else 8.sp
    val numberText = CompactUsageCardPresentation.compactPercentNumberText(percent)
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)

    Box(modifier = Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            fun arcInset(stroke: Float) = stroke / 2f

            val inset = arcInset(maxOf(trackStroke.toPx(), progressStroke.toPx()))
            val arcSize = Size(
                width = size.width - inset * 2f,
                height = size.height - inset * 2f,
            )
            drawArc(
                color = track,
                startAngle = CompactGaugeStartAngle,
                sweepAngle = CompactGaugeSweepAngle,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = trackStroke.toPx(), cap = StrokeCap.Round),
            )

            val progressSweep = if (clamped <= 0f) {
                CompactGaugeZeroSweepAngle
            } else {
                CompactGaugeSweepAngle * clamped / 100f
            }
            drawArc(
                color = accent.copy(alpha = if (clamped <= 0f) 0.72f else 1f),
                startAngle = CompactGaugeStartAngle,
                sweepAngle = progressSweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = progressStroke.toPx(), cap = StrokeCap.Round),
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = numberText,
                style = TextStyle(
                    fontSize = numberSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.35).sp,
                ),
                color = labelColor,
                maxLines = 1,
            )
            Text(
                text = "%",
                style = TextStyle(
                    fontSize = percentSize,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = labelColor,
                modifier = Modifier
                    .padding(start = 1.dp)
                    .offset(y = (-1).dp),
                maxLines = 1,
            )
        }
    }
}

private const val CompactGaugeStartAngle = 135f
private const val CompactGaugeSweepAngle = 270f
private const val CompactGaugeZeroSweepAngle = 0.9f
