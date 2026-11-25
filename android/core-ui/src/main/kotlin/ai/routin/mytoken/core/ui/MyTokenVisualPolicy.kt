package ai.routin.mytoken.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * 旧调色板的过渡别名。页面迁移完成后（Task 11）删除；
 * 新代码一律使用 [MyTokenThemeTokens.colors]。
 */
@Deprecated("改用 MyTokenThemeTokens.colors")

object MyTokenVisualPolicy {
    enum class SurfaceRole {
        Window,
        Card,
        /** 首页供应商白卡：从纸灰页底上浮，使用 0.5dp 发丝线与一层很轻的阴影。 */
        FloatingCard,
        Metric,
        Control,
        Modal,
        /** 侧边抽屉页：与页面同用 canvas 背景，靠 1dp 描边与阴影和背后内容分隔。 */
        Drawer,
    }

    /** 实色表面是默认；浮层与首页浮起白卡允许阴影。 */
    enum class Material {
        Solid,
        Overlay,
    }

    val outerRadiusDp: Dp = MyTokenRadius.card
    val innerRadiusDp: Dp = MyTokenRadius.inner
    val buttonRadiusDp: Dp = MyTokenRadius.inner

    fun materialFor(role: SurfaceRole): Material = when (role) {
        SurfaceRole.Modal -> Material.Overlay
        SurfaceRole.Window,
        SurfaceRole.Card,
        SurfaceRole.FloatingCard,
        SurfaceRole.Metric,
        SurfaceRole.Control,
        -> Material.Solid

        SurfaceRole.Modal,
        SurfaceRole.Drawer,
        -> Material.Overlay
    }

    fun allowsShadow(role: SurfaceRole): Boolean =
        role == SurfaceRole.Modal ||
            role == SurfaceRole.Drawer ||
            role == SurfaceRole.FloatingCard
}
