package ai.routin.mytoken.widget

object WidgetFocusRingPolicy {
    const val MAX_RINGS = 3

    fun metrics(credential: WidgetCredentialPresentation): List<WidgetMetricPresentation> =
        credential.orderedMetrics.take(MAX_RINGS)
}
