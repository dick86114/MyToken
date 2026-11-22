package ai.routin.mytoken.core.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * SF Symbol 风格的 `star.circle.fill`：黄色圆底 + 五角星镂空。
 * 整个符号做连续呼吸闪烁；减少动态时保持静态。调用方只负责尺寸和 testTag。
 */
@Composable
fun MyTokenUpdateStarIcon(
    modifier: Modifier = Modifier,
    tint: Color = MyTokenThemeTokens.colors.actionFill,
) {
    val motionEnabled = rememberMyTokenMotionEnabled()
    val breathe = if (motionEnabled) {
        val transition = rememberInfiniteTransition(label = "update-star-breathe")
        val value by transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "update-star-breathe-value",
        )
        value
    } else {
        1f
    }
    Canvas(
        modifier = modifier.graphicsLayer {
            alpha = if (motionEnabled) 0.5f + 0.5f * breathe else 1f
            val scale = if (motionEnabled) 0.92f + 0.08f * breathe else 1f
            scaleX = scale
            scaleY = scale
            // 让星形以 Clear 混合模式真正镂空，露出父级背景而不是画一块假底色。
            compositingStrategy = CompositingStrategy.Offscreen
        },
    ) {
        val radius = size.minDimension / 2f
        drawCircle(color = tint, radius = radius, center = center)
        val star = Path()
        val outer = radius * 0.64f
        val inner = radius * 0.26f
        repeat(10) { index ->
            val angle = -PI / 2 + index * PI / 5
            val r = if (index % 2 == 0) outer else inner
            val point = Offset(
                x = center.x + (cos(angle) * r).toFloat(),
                y = center.y + (sin(angle) * r).toFloat(),
            )
            if (index == 0) star.moveTo(point.x, point.y) else star.lineTo(point.x, point.y)
        }
        star.close()
        drawPath(path = star, color = Color.Transparent, blendMode = BlendMode.Clear)
    }
}
