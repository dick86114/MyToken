package ai.routin.mytoken.widget

import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.feature.home.ProviderCatalog
import androidx.compose.ui.graphics.Color

/** Shared widget palette: provider accent first, severity overrides only for risk states. */
object WidgetProviderPalette {
    fun accentColor(providerId: String): Color =
        ProviderId.fromRawValue(providerId)?.let(ProviderCatalog::accentColor)
            ?: Color(0xFF2563EB)
}
