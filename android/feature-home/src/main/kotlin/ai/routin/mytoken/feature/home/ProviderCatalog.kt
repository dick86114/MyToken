package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.ProviderId
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color

/**
 * Provider display names and accent colors, mirroring the macOS popover
 * (ProviderRegistry display names + ProviderTheme accent colors).
 */
object ProviderCatalog {
    fun displayName(providerId: ProviderId): String = when (providerId) {
        ProviderId.Routin -> "Routin"
        ProviderId.DeepSeek -> "DeepSeek"
        ProviderId.Glm -> "智谱 GLM"
        ProviderId.Volcengine -> "火山方舟"
        ProviderId.NewAPI -> "New API"
        ProviderId.CommandCode -> "Command Code"
        ProviderId.Xiaomi -> "小米 MiMo"
        ProviderId.OpenCode -> "OpenCode"
    }

    @DrawableRes
    fun logoRes(providerId: ProviderId): Int = when (providerId) {
        ProviderId.Routin -> R.drawable.share_provider_routin_logo
        ProviderId.DeepSeek -> R.drawable.share_provider_deepseek_logo
        ProviderId.Glm -> R.drawable.share_provider_glm_logo
        ProviderId.Volcengine -> R.drawable.share_provider_volcengine_logo
        ProviderId.NewAPI -> R.drawable.share_provider_newapi_logo
        ProviderId.CommandCode -> R.drawable.share_provider_commandcode_logo
        ProviderId.Xiaomi -> R.drawable.share_provider_xiaomi_logo
        ProviderId.OpenCode -> R.drawable.share_provider_opencode_logo
    }

    fun accentColor(providerId: ProviderId): Color = when (providerId) {
        ProviderId.Routin -> Color(0xFF1565C0)
        ProviderId.DeepSeek -> Color(0xFF3949AB)
        ProviderId.Glm -> Color(0xFF2E7D32)
        ProviderId.Volcengine -> Color(0xFFEF6C00)
        ProviderId.NewAPI -> Color(0xFF6A1B9A)
        ProviderId.CommandCode -> Color(0xFF00897B)
        ProviderId.Xiaomi -> Color(0xFFAD1457)
        ProviderId.OpenCode -> Color(0xFFCCE833)
    }
}
