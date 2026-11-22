package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenMotion
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.core.ui.rememberMyTokenMotionEnabled
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
import kotlin.math.max
import kotlin.math.roundToInt

/** 主机面板式仪表：270° 内 12 段刻度，外圈表框、内圈轴心。 */
private const val SegmentCount = 12
private const val ArcStartAngle = 135f
private const val ArcSweepAngle = 270f
private const val SegmentSweepAngle = 18.1f
private const val SegmentGapDegrees = 2.2f
private const val BezelRadiusRatio = 31.4f / 64f
private const val SegmentRadiusRatio = 26f / 64f
private const val HubRadiusRatio = 17.2f / 64f
private const val SegmentStrokeRatio = 6f / 64f
private const val HairlineRatio = 0.9f / 64f
private const val SegmentHighlightRadiusRatio = 27.4f / 64f
private const val SegmentHighlightSweep = 15.1f
private const val SegmentHighlightInset = 1.5f

@Composable
internal fun UsageArcGauge(
    percent: Double?,
    tone: UsageMetricTone,
    diameter: Dp,
    inactive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    val accent = when {
        inactive -> colors.track
        tone == UsageMetricTone.Warning -> colors.warningFill
        tone == UsageMetricTone.Critical -> colors.criticalFill
        else -> colors.ink
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
    // 动态字体放大时同步放大仪表，避免圆心数字被 52dp 直径裁切。
    val scaledDiameter = diameter * LocalDensity.current.fontScale.coerceIn(1f, 1.35f)
    val numberStyle = when {
        scaledDiameter >= 80.dp -> MyTokenType.detailRing
        scaledDiameter >= 64.dp -> MyTokenType.ringFull
        else -> MyTokenType.ringCompact
    }
    val numberText = CompactUsageCardPresentation.compactPercentNumberText(percent)
    val contentColor = if (inactive) colors.muted else colors.ink

    Box(
        modifier = modifier.size(scaledDiameter),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val side = size.minDimension
            val center = Offset(size.width / 2f, size.height / 2f)
            val bezelRadius = side * BezelRadiusRatio
            val segmentRadius = side * SegmentRadiusRatio
            val hubRadius = side * HubRadiusRatio
            val segmentStroke = side * SegmentStrokeRatio
            val hairline = side * HairlineRatio
            val highlightRadius = side * SegmentHighlightRadiusRatio

            // 外圈表框：浅底 + 细描边 + 上下两道高光/阴影。
            drawCircle(color = colors.subtle, radius = bezelRadius, center = center)
            drawCircle(
                color = colors.line,
                radius = bezelRadius,
                center = center,
                style = Stroke(width = hairline),
            )
            drawArc(
                color = colors.surface.copy(alpha = 0.7f),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(center.x - bezelRadius, center.y - bezelRadius),
                size = Size(bezelRadius * 2f, bezelRadius * 2f),
                style = Stroke(width = hairline, cap = StrokeCap.Round),
            )
            drawArc(
                color = colors.ink.copy(alpha = 0.08f),
                startAngle = 22f,
                sweepAngle = 136f,
                useCenter = false,
                topLeft = Offset(center.x - bezelRadius, center.y - bezelRadius),
                size = Size(bezelRadius * 2f, bezelRadius * 2f),
                style = Stroke(width = hairline, cap = StrokeCap.Round),
            )

            val litSegments = if (animatedPercent <= 0f) {
                0
            } else {
                max(1, (animatedPercent / 100f * SegmentCount).roundToInt())
            }
            repeat(SegmentCount) { index ->
                val start = ArcStartAngle +
                    index * (ArcSweepAngle / SegmentCount) +
                    SegmentGapDegrees
                drawArc(
                    color = if (index < litSegments) accent else colors.track,
                    startAngle = start,
                    sweepAngle = SegmentSweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - segmentRadius, center.y - segmentRadius),
                    size = Size(segmentRadius * 2f, segmentRadius * 2f),
                    style = Stroke(width = segmentStroke, cap = StrokeCap.Round),
                )
                if (index < litSegments) {
                    drawArc(
                        color = colors.surface.copy(alpha = 0.38f),
                        startAngle = start + SegmentHighlightInset,
                        sweepAngle = SegmentHighlightSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - highlightRadius, center.y - highlightRadius),
                        size = Size(highlightRadius * 2f, highlightRadius * 2f),
                        style = Stroke(width = hairline * 0.9f, cap = StrokeCap.Round),
                    )
                }
            }

            // 内圈轴心：surface 底 + 细描边 + 内阴影弧。
            drawCircle(color = colors.surface, radius = hubRadius, center = center)
            drawCircle(
                color = colors.line,
                radius = hubRadius,
                center = center,
                style = Stroke(width = hairline),
            )
            drawArc(
                color = colors.ink.copy(alpha = 0.10f),
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = Offset(center.x - hubRadius, center.y - hubRadius),
                size = Size(hubRadius * 2f, hubRadius * 2f),
                style = Stroke(width = hairline * 0.8f, cap = StrokeCap.Round),
            )
        }
        Text(
            text = buildAnnotatedString {
                append(numberText)
                withStyle(SpanStyle(fontSize = MyTokenType.percentMark.fontSize)) {
                    append("%")
                }
            },
            style = numberStyle,
            color = contentColor,
            maxLines = 1,
        )
    }
}
