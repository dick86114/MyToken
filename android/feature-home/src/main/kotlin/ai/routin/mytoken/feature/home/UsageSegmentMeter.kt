package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenMotion
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.rememberMyTokenMotionEnabled
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

private const val SegmentCells = 12

/** 主机面板式分段进度条：12 格刻度，对应设计稿的硬件仪表。 */
@Composable
internal fun UsageSegmentMeter(
    percent: Double?,
    tone: UsageMetricTone,
    modifier: Modifier = Modifier,
    height: Dp = 9.dp,
) {
    val colors = MyTokenThemeTokens.colors
    val litColor = when (tone) {
        UsageMetricTone.Warning -> colors.warningFill
        UsageMetricTone.Critical -> colors.criticalFill
        UsageMetricTone.Normal -> colors.ink
    }
    val clamped = ((percent ?: 0.0).coerceIn(0.0, 100.0)).toFloat()
    val motionEnabled = rememberMyTokenMotionEnabled()
    val animatedPercent by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(
            durationMillis = MyTokenMotion.ringDurationMillis(motionEnabled),
        ),
        label = "usageSegmentMeter",
    )
    val litCells = if (animatedPercent <= 0f) {
        0
    } else {
        max(1, (animatedPercent / 100f * SegmentCells).roundToInt())
    }
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(colors.subtle)
            .border(1.dp, colors.line, shape)
            .padding(2.dp)
            .testTag("usage_segment_meter"),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        repeat(SegmentCells) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (index < litCells) litColor else colors.track),
            )
        }
    }
}
