package ai.routin.mytoken.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** A 晨雾跨端视觉契约：浅表面承载结构，品牌蓝表达操作和正常用量。 */
object MyTokenPalette {
    val brandLight = Color(0xFF2F6BFF)
    val brandDark = Color(0xFF6E9DFF)
    val positiveLight = Color(0xFF2E9B77)
    val positiveDark = Color(0xFF55C49D)
    val warning = Color(0xFFBF8126)
    val critical = Color(0xFFD25A60)

    val lightCanvas = Color(0xFFF3F6F9)
    val lightSurface = Color(0xFFFFFFFF)
    val lightElevated = Color(0xFFF7F9FC)
    val lightText = Color(0xFF17222C)
    val lightMutedText = Color(0xFF71808E)

    val darkCanvas = Color(0xFF0B1015)
    val darkSurface = Color(0xFF121920)
    val darkElevated = Color(0xFF1A2430)
    val darkText = Color(0xFFF7F8FA)
    val darkMutedText = Color(0xFF93A1AE)
}

object MyTokenVisualPolicy {
    enum class SurfaceRole {
        Window,
        Card,
        Metric,
        Control,
        Modal,
    }

    enum class Material {
        WindowGlass,
        Solid,
        ModalGlass,
    }

    val outerRadiusDp = 20.dp
    val innerRadiusDp = 12.dp
    val buttonRadiusDp = 12.dp

    fun materialFor(role: SurfaceRole): Material = when (role) {
        SurfaceRole.Window -> Material.WindowGlass
        SurfaceRole.Card, SurfaceRole.Metric, SurfaceRole.Control -> Material.Solid
        SurfaceRole.Modal -> Material.ModalGlass
    }

    fun allowsShadow(role: SurfaceRole): Boolean = false
}
