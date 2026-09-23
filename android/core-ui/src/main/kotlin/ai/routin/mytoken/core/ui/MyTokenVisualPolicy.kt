package ai.routin.mytoken.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 方案 A 的跨端视觉契约：石墨蓝承载结构，品牌蓝表达操作和正常用量。 */
object MyTokenPalette {
    val brandLight = Color(0xFF2F80ED)
    val brandDark = Color(0xFF4D94F5)
    val positiveLight = Color(0xFF2F9E78)
    val positiveDark = Color(0xFF45B48B)
    val warning = Color(0xFFC98A2E)
    val critical = Color(0xFFD15B5B)

    val lightCanvas = Color(0xFFF4F5F7)
    val lightSurface = Color(0xFFFFFFFF)
    val lightElevated = Color(0xFFEEF1F5)
    val lightText = Color(0xFF20252B)
    val lightMutedText = Color(0xFF68717D)

    val darkCanvas = Color(0xFF0B0F14)
    val darkSurface = Color(0xFF121820)
    val darkElevated = Color(0xFF1A2230)
    val darkText = Color(0xFFF7F8FA)
    val darkMutedText = Color(0xFF94A3B8)
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

    val outerRadiusDp = 14.dp
    val innerRadiusDp = 10.dp
    val buttonRadiusDp = 10.dp

    fun materialFor(role: SurfaceRole): Material = when (role) {
        SurfaceRole.Window -> Material.WindowGlass
        SurfaceRole.Card, SurfaceRole.Metric, SurfaceRole.Control -> Material.Solid
        SurfaceRole.Modal -> Material.ModalGlass
    }

    fun allowsShadow(role: SurfaceRole): Boolean = role == SurfaceRole.Modal
}
