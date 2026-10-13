package ai.routin.mytoken.widget

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetFocusRingPolicyTest {

    @Test
    fun `focus ring shows up to three ordered metrics`() {
        val credential = WidgetCredentialPresentation(
            id = UUID.randomUUID(),
            providerId = "glm",
            providerName = "智谱 GLM",
            name = "GLM",
            planName = "",
            isStale = false,
            primary = metric("five-hour", "5 小时用量", "20%"),
            secondary = listOf(
                metric("weekly", "每周用量", "42%"),
                metric("monthly", "每月用量", "31%"),
                metric("model-calls", "模型调用量", "12 次"),
            ),
            fixedMetricUnavailable = false,
        )

        assertEquals(
            listOf("five-hour", "weekly", "monthly"),
            WidgetFocusRingPolicy.metrics(credential).map { it.id },
        )
    }

    private fun metric(id: String, label: String, value: String) = WidgetMetricPresentation(
        id = id,
        label = label,
        valueText = value,
    )
}
