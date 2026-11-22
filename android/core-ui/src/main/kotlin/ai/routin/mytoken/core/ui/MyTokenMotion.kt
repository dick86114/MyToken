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
    const val starMillis = 2400

    fun ringDurationMillis(enabled: Boolean): Int = if (enabled) ringMillis else 0
    fun locateDurationMillis(enabled: Boolean): Int = if (enabled) locateMillis else 0
    fun modeSwitchDurationMillis(enabled: Boolean): Int = if (enabled) modeSwitchMillis else 0
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
