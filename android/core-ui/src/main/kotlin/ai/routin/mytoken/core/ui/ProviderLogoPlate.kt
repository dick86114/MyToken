package ai.routin.mytoken.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 供应商 Logo 的统一承托层。
 *
 * 多数供应商使用透明底深色图标，直接叠在深色背景上会失去辨识度；深色模式改用
 * 接近白色的轻量底板，既保留品牌原色，也避免额外描边制造视觉噪声。
 */
@Composable
fun ProviderLogoPlate(
    painter: Painter,
    contentDescription: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    contentPadding: Dp = 2.dp,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                if (dark) Color.White.copy(alpha = 0.94f) else Color.Transparent,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        )
    }
}
