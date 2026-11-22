package ai.routin.mytoken.feature.settings

import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** 给 SF 风格的「基础符号 + 三角警告 badge」提供统一落地。 */
@Composable
internal fun SettingsBadgedIcon(
    base: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MyTokenThemeTokens.colors.muted,
    size: androidx.compose.ui.unit.Dp = 18.dp,
) {
    Box(modifier = modifier.size(size)) {
        Icon(
            imageVector = base,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(size * 0.94f)
                .align(Alignment.BottomStart),
        )
        Icon(
            imageVector = Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(size * 0.5f)
                .align(Alignment.TopEnd)
                .offset(x = 1.dp, y = (-1).dp),
        )
    }
}
