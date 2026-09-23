package ai.routin.mytoken.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = MyTokenPalette.brandLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEAFF),
    onPrimaryContainer = Color(0xFF102A4C),
    secondary = MyTokenPalette.positiveLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDF2E9),
    onSecondaryContainer = Color(0xFF123B2E),
    tertiary = MyTokenPalette.warning,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF5E8CF),
    onTertiaryContainer = Color(0xFF3A2A0F),
    background = MyTokenPalette.lightCanvas,
    onBackground = MyTokenPalette.lightText,
    surface = MyTokenPalette.lightSurface,
    onSurface = MyTokenPalette.lightText,
    surfaceVariant = MyTokenPalette.lightElevated,
    onSurfaceVariant = MyTokenPalette.lightMutedText,
    outlineVariant = Color(0x1420252B),
    error = MyTokenPalette.critical,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = MyTokenPalette.brandDark,
    onPrimary = Color(0xFF071B2F),
    primaryContainer = Color(0xFF203B5C),
    onPrimaryContainer = Color(0xFFDCEAFF),
    secondary = MyTokenPalette.positiveDark,
    onSecondary = Color(0xFF06271C),
    secondaryContainer = Color(0xFF174434),
    onSecondaryContainer = Color(0xFFDDF2E9),
    tertiary = MyTokenPalette.warning,
    onTertiary = Color(0xFF241705),
    tertiaryContainer = Color(0xFF4B3517),
    onTertiaryContainer = Color(0xFFF5E8CF),
    background = MyTokenPalette.darkCanvas,
    onBackground = MyTokenPalette.darkText,
    surface = MyTokenPalette.darkSurface,
    onSurface = MyTokenPalette.darkText,
    surfaceVariant = MyTokenPalette.darkElevated,
    onSurfaceVariant = MyTokenPalette.darkMutedText,
    outlineVariant = Color(0x1FFFFFFF),
    error = MyTokenPalette.critical,
    onError = Color.White,
)

/** 固定使用 MyToken 的跨端品牌层级，避免动态取色破坏供应商语义色。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyTokenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
    ) {
        // OriginOS 等机型上系统拉伸 overscroll 会额外走一层 RenderEffect，滑动容易掉帧。
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            content()
        }
    }
}
