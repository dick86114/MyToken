package ai.routin.mytoken.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LightColors = lightColorScheme(
    primary = MyTokenLightColors.actionFill,
    onPrimary = MyTokenLightColors.onAction,
    primaryContainer = MyTokenLightColors.subtle,
    onPrimaryContainer = MyTokenLightColors.actionText,
    secondary = MyTokenLightColors.positiveFill,
    onSecondary = MyTokenLightColors.onAction,
    secondaryContainer = MyTokenLightColors.subtle,
    onSecondaryContainer = MyTokenLightColors.positiveText,
    tertiary = MyTokenLightColors.warningFill,
    onTertiary = MyTokenLightColors.onAction,
    tertiaryContainer = MyTokenLightColors.subtle,
    onTertiaryContainer = MyTokenLightColors.warningText,
    background = MyTokenLightColors.canvas,
    onBackground = MyTokenLightColors.ink,
    surface = MyTokenLightColors.surface,
    onSurface = MyTokenLightColors.ink,
    surfaceVariant = MyTokenLightColors.subtle,
    onSurfaceVariant = MyTokenLightColors.muted,
    outline = MyTokenLightColors.line,
    outlineVariant = MyTokenLightColors.line,
    error = MyTokenLightColors.criticalFill,
    onError = MyTokenLightColors.onAction,
    errorContainer = MyTokenLightColors.subtle,
    onErrorContainer = MyTokenLightColors.criticalText,
)

private val DarkColors = darkColorScheme(
    primary = MyTokenDarkColors.actionFill,
    onPrimary = MyTokenDarkColors.onAction,
    primaryContainer = MyTokenDarkColors.subtle,
    onPrimaryContainer = MyTokenDarkColors.actionText,
    secondary = MyTokenDarkColors.positiveFill,
    onSecondary = MyTokenDarkColors.onAction,
    secondaryContainer = MyTokenDarkColors.subtle,
    onSecondaryContainer = MyTokenDarkColors.positiveText,
    tertiary = MyTokenDarkColors.warningFill,
    onTertiary = MyTokenDarkColors.onAction,
    tertiaryContainer = MyTokenDarkColors.subtle,
    onTertiaryContainer = MyTokenDarkColors.warningText,
    background = MyTokenDarkColors.canvas,
    onBackground = MyTokenDarkColors.ink,
    surface = MyTokenDarkColors.surface,
    onSurface = MyTokenDarkColors.ink,
    surfaceVariant = MyTokenDarkColors.subtle,
    onSurfaceVariant = MyTokenDarkColors.muted,
    outline = MyTokenDarkColors.line,
    outlineVariant = MyTokenDarkColors.line,
    error = MyTokenDarkColors.criticalFill,
    onError = MyTokenDarkColors.onAction,
    errorContainer = MyTokenDarkColors.subtle,
    onErrorContainer = MyTokenDarkColors.criticalText,
)

/** 固定使用 MyToken 的跨端品牌层级，避免动态取色破坏供应商语义色。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyTokenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val tokens = if (darkTheme) MyTokenDarkColors else MyTokenLightColors
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MyTokenTypography,
    ) {
        // OriginOS 等机型上系统拉伸 overscroll 会额外走一层 RenderEffect，滑动容易掉帧。
        CompositionLocalProvider(
            LocalMyTokenColors provides tokens,
            LocalOverscrollConfiguration provides null,
        ) {
            content()
        }
    }
}
