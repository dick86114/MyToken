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
        UsageMetricTone.Normal -> CardPalette.emerald(isDark)
    }
    val track = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.08f)
    val strokeWidth = diameter * 0.10f
    val clamped = ((percent ?: 0.0).coerceIn(0.0, 100.0)).toFloat()

    val numberSize = (if (diameter > 42.dp) 13.5 else 11.5).sp
    val percentSize = (if (diameter > 42.dp) 9.5 else 8.0).sp
    val numberWeight = if ((percent ?: 0.0) <= 0.0) FontWeight.SemiBold else FontWeight.Bold
    val labelColor = when (tone) {
        UsageMetricTone.Critical -> colors.critical
        UsageMetricTone.Warning -> colors.warning
        UsageMetricTone.Normal ->
            if ((percent ?: 0.0) <= 0.0 && percent != null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            }
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
