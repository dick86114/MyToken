package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.ProviderId
import androidx.annotation.DrawableRes

/**
 * Provider display names and logo resources. 颜色只来自 MyTokenColors token，
 * 不再提供供应商主题色。
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
}
