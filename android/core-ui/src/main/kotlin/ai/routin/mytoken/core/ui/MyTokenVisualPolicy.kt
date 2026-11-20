package ai.routin.mytoken.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * 旧调色板的过渡别名。页面迁移完成后（Task 11）删除；
 * 新代码一律使用 [MyTokenThemeTokens.colors]。
 */
@Deprecated("改用 MyTokenThemeTokens.colors")
object MyTokenPalette {
    val brandLight: Color = MyTokenLightColors.actionFill
    val brandDark: Color = MyTokenDarkColors.actionFill
    val positiveLight: Color = MyTokenLightColors.positiveFill
    val positiveDark: Color = MyTokenDarkColors.positiveFill
    val warning: Color = MyTokenLightColors.warningFill
    val critical: Color = MyTokenLightColors.criticalFill

    val lightCanvas: Color = MyTokenLightColors.canvas
    val lightSurface: Color = MyTokenLightColors.surface
    val lightElevated: Color = MyTokenLightColors.subtle
    val lightText: Color = MyTokenLightColors.ink
    val lightMutedText: Color = MyTokenLightColors.muted

    val darkCanvas: Color = MyTokenDarkColors.canvas
    val darkSurface: Color = MyTokenDarkColors.surface
    val darkElevated: Color = MyTokenDarkColors.subtle
    val darkText: Color = MyTokenDarkColors.ink
    val darkMutedText: Color = MyTokenDarkColors.muted
}

object MyTokenVisualPolicy {
    enum class SurfaceRole {
        Window,
        Card,
        Metric,
        Control,
        Modal,
    }

    /** 实色表面是默认；只有浮层允许阴影。 */
    enum class Material {
        Solid,
        Overlay,
    }

    val outerRadiusDp: Dp = MyTokenRadius.card
    val innerRadiusDp: Dp = MyTokenRadius.inner
    val buttonRadiusDp: Dp = MyTokenRadius.inner

    fun materialFor(role: SurfaceRole): Material = when (role) {
        SurfaceRole.Modal -> Material.Overlay
        SurfaceRole.Window, SurfaceRole.Card, SurfaceRole.Metric, SurfaceRole.Control -> Material.Solid
    }

    fun allowsShadow(role: SurfaceRole): Boolean = role == SurfaceRole.Modal
}
