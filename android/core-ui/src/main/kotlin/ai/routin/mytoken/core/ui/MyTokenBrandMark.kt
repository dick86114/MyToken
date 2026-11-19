package ai.routin.mytoken.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** MyToken 品牌标记；尺寸和圆角与首页、设置页身份区保持一致。 */
@Composable
fun MyTokenBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.mytoken_brand_logo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            // 原图四周带白边；轻微放大后裁掉白框，避免任何页面再出现白色描边。
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = 1.16f
                    scaleY = 1.16f
                },
        )
    }
}
