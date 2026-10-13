package ai.routin.mytoken.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WidgetPresentationStateTest {

    @Test
    fun `widget states map to distinct copy and visual severity`() {
        val statuses = listOf(
            WidgetPresentationStatus.Ready,
            WidgetPresentationStatus.Loading,
            WidgetPresentationStatus.Stale,
            WidgetPresentationStatus.RefreshFailed,
            WidgetPresentationStatus.AuthenticationFailed,
            WidgetPresentationStatus.Disabled,
            WidgetPresentationStatus.MissingCredential,
            WidgetPresentationStatus.NoMetrics,
            WidgetPresentationStatus.FixedMetricUnavailable,
        )
        val copies = statuses.map(WidgetPresentationStateResolver::copy)

        assertEquals(statuses.size, copies.toSet().size)
        assertEquals("需要重新登录/更换凭证", WidgetPresentationStateResolver.copy(WidgetPresentationStatus.AuthenticationFailed))
        assertEquals("凭证不存在，请重新配置", WidgetPresentationStateResolver.copy(WidgetPresentationStatus.MissingCredential))
        assertEquals("该套餐暂无可用指标", WidgetPresentationStateResolver.copy(WidgetPresentationStatus.NoMetrics))
    }

    @Test
    fun `resolver prioritizes disabled auth failure and loading`() {
        assertEquals(
            WidgetPresentationStatus.Disabled,
            WidgetPresentationStateResolver.resolve(
                isEnabled = false,
                hasSnapshot = true,
                isStale = false,
                isRefreshing = false,
                failure = null,
                metricCount = 1,
                fixedMetricUnavailable = false,
            ),
        )
        assertEquals(
            WidgetPresentationStatus.AuthenticationFailed,
            WidgetPresentationStateResolver.resolve(
                isEnabled = true,
                hasSnapshot = true,
                isStale = false,
                isRefreshing = false,
                failure = WidgetFailureKind.Authentication,
                metricCount = 1,
                fixedMetricUnavailable = false,
            ),
        )
        assertEquals(
            WidgetPresentationStatus.Loading,
            WidgetPresentationStateResolver.resolve(
                isEnabled = true,
                hasSnapshot = false,
                isStale = false,
                isRefreshing = true,
                failure = null,
                metricCount = 0,
                fixedMetricUnavailable = false,
            ),
        )
    }

    @Test
    fun `stale data uses timestamp badge instead of high usage color`() {
        assertFalse(WidgetPresentationStateResolver.usesHighUsageColor(WidgetPresentationStatus.Stale))
    }
}
