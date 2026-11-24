package ai.routin.mytoken.core.ui

import android.animation.ValueAnimator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

/** 动效时长集中在此；系统关闭动画时全部折叠为 0。 */
object MyTokenMotion {
    const val pressMillis = 100
    const val modeSwitchMillis = 320
    const val ringMillis = 500
    const val locateMillis = 300
    const val locatePulseGapMillis = 90
    const val wideLocatePulseCount = 3
    const val starMillis = 2400

    /** 大屏三栏：详情挤入 / 退出、中间重排、筛选与报告高度。 */
    const val detailOpenMillis = 260
    const val detailCloseMillis = 240
    const val detailReflowMillis = 200
    const val filterMillis = 160
    const val reportResizeMillis = 220

    fun ringDurationMillis(enabled: Boolean): Int = if (enabled) ringMillis else 0
    fun locateDurationMillis(enabled: Boolean): Int = if (enabled) locateMillis else 0

    /** 定位高亮从开始到完全结束的时间，用于决定何时清除高亮状态。 */
    fun locateHighlightWindowMillis(enabled: Boolean, pulses: Int): Int =
        if (enabled) {
            locateMillis * 2 * pulses + locatePulseGapMillis * (pulses - 1) + 120
        } else {
            250
        }
    fun modeSwitchDurationMillis(enabled: Boolean): Int = if (enabled) modeSwitchMillis else 0
    fun detailOpenDurationMillis(enabled: Boolean): Int = if (enabled) detailOpenMillis else 0
    fun detailCloseDurationMillis(enabled: Boolean): Int = if (enabled) detailCloseMillis else 0
    fun detailReflowDurationMillis(enabled: Boolean): Int = if (enabled) detailReflowMillis else 0
    fun filterDurationMillis(enabled: Boolean): Int = if (enabled) filterMillis else 0
    fun reportResizeDurationMillis(enabled: Boolean): Int = if (enabled) reportResizeMillis else 0
}

/** 读取系统“移除动画/减少动态”开关。 */
@Composable
fun rememberMyTokenMotionEnabled(): Boolean = ValueAnimator.areAnimatorsEnabled()

/** 按压缩放：卡片 0.99，按钮 0.97；ripple 仍由 clickable 提供。 */
@Composable
fun Modifier.myTokenPressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.99f,
): Modifier {
    val enabled = rememberMyTokenMotionEnabled()
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = tween(
            durationMillis = if (enabled) MyTokenMotion.pressMillis else 0,
        ),
        label = "myTokenPressScale",
    )
    return scale(scale)
}
