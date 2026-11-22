package ai.routin.mytoken.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 设计稿使用的黑白品牌标记：浅色主题用黑色版本，深色主题用白色反色版本。
 * 资源与 macOS 菜单栏图标同源，避免 Android 端继续使用带底色的旧彩色图。
 */
internal fun brandLogoResource(darkTheme: Boolean): Int = if (darkTheme) {
    R.drawable.mytoken_brand_logo_dark
} else {
    R.drawable.mytoken_brand_logo_light
}

/** 当前是否深色外观；供需要在深色底上反色的调用方判断。 */
@Composable
fun isMyTokenDarkTheme(): Boolean = LocalMyTokenDarkTheme.current

/** MyToken 品牌标记；尺寸与首页、设置页身份区保持一致。 */
@Composable
fun MyTokenBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    darkTheme: Boolean = LocalMyTokenDarkTheme.current,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(brandLogoResource(darkTheme)),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
