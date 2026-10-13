package ai.routin.mytoken.widget

enum class WidgetTextRole {
    ProviderName,
    CredentialName,
    MetricLabel,
    MetricValue,
    StatusText,
}

object WidgetTextLayoutPolicy {
    fun lineLimit(role: WidgetTextRole): Int = 1

    fun usesEllipsis(role: WidgetTextRole): Boolean = when (role) {
        WidgetTextRole.ProviderName,
        WidgetTextRole.CredentialName,
        WidgetTextRole.MetricLabel,
        -> true
        WidgetTextRole.MetricValue,
        WidgetTextRole.StatusText,
        -> false
    }
}

object WidgetLayoutPolicy {
    fun kindFor(widthDp: Int, heightDp: Int): WidgetSizeKind {
        if (heightDp < 110) return WidgetSizeKind.Compact
        return when {
            widthDp < 180 -> WidgetSizeKind.Compact
            widthDp < 300 -> WidgetSizeKind.Standard
            else -> WidgetSizeKind.Expanded
        }
    }
}
