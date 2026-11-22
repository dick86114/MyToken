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
import androidx.compose.ui.graphics.Color
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

internal data class UsageMeterSegment(
    val weight: Double,
    val color: Color,
)

/** 三段 Token 构成的分段条：与用量条共用 12 格硬件面板样式。 */
@Composable
internal fun UsageMultiSegmentMeter(
    segments: List<UsageMeterSegment>,
    modifier: Modifier = Modifier,
    height: Dp = 11.dp,
) {
    val colors = MyTokenThemeTokens.colors
    val motionEnabled = rememberMyTokenMotionEnabled()
    val weights = segments.map { segment ->
        animateFloatAsState(
            targetValue = segment.weight.coerceAtLeast(0.0).toFloat(),
            animationSpec = tween(
                durationMillis = MyTokenMotion.ringDurationMillis(motionEnabled),
            ),
            label = "usageMultiSegmentMeter",
        ).value
    }
    val total = weights.sum()
    val cellColors = MutableList(SegmentCells) { colors.track }
    if (total > 0f) {
        var boundary = 0
        var cumulative = 0f
        weights.forEachIndexed { index, weight ->
            cumulative += weight
            val next = ((cumulative / total) * SegmentCells)
                .roundToInt()
                .coerceIn(boundary, SegmentCells)
            for (cell in boundary until next) {
                cellColors[cell] = segments[index].color
            }
            boundary = next
        }
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
            .testTag("usage_token_meter"),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        repeat(SegmentCells) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.dp))
                    .background(cellColors[index]),
            )
        }
    }
}

/**
 * 余额构成的分段条：左段墨色表示充值/现金，右段黄色表示赠送。
 * [primaryWeight] 为 null 时整条回到轨道色（没有可拆分的构成）。
 */
@Composable
internal fun UsageCompositionMeter(
    primaryWeight: Double?,
    modifier: Modifier = Modifier,
    height: Dp = 9.dp,
) {
    val colors = MyTokenThemeTokens.colors
    val motionEnabled = rememberMyTokenMotionEnabled()
    val target = (primaryWeight ?: 0.0).coerceIn(0.0, 1.0).toFloat()
    val animatedWeight by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(
            durationMillis = MyTokenMotion.ringDurationMillis(motionEnabled),
        ),
        label = "usageCompositionMeter",
    )
    val primaryCells = if (primaryWeight == null) {
        0
    } else {
        (animatedWeight * SegmentCells).roundToInt().coerceIn(0, SegmentCells)
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
            .testTag("usage_composition_meter"),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        repeat(SegmentCells) { index ->
            val color = when {
                primaryWeight == null -> colors.track
                index < primaryCells -> colors.ink
                else -> colors.actionFill
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.dp))
                    .background(color),
            )
        }
    }
}
