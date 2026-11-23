package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.alerts.AlertThresholds
import ai.routin.mytoken.domain.model.CredentialMetadataKey
import org.junit.Assert.assertEquals
import org.junit.Test

class CredentialDetailConfigTest {

    @Test
    fun `alert threshold display marks custom overrides`() {
        assertEquals(
            "低 10% · 高 20%（自定义）",
            alertThresholdDisplay(
                globalAlertThresholds = AlertThresholds(80, 95),
                alertThresholdOverride = AlertThresholds(10, 20),
            ),
        )
    }

    @Test
    fun `alert threshold display marks global fallback`() {
        assertEquals(
            "低 55% · 高 95%（跟随全局）",
            alertThresholdDisplay(
                globalAlertThresholds = AlertThresholds(55, 95),
                alertThresholdOverride = null,
            ),
        )
    }

    @Test
    fun `balance warning display formats numbers without trailing zeros`() {
        assertEquals("80", balanceWarningDisplay(metadata("80")))
        assertEquals("8.5", balanceWarningDisplay(metadata("8.5")))
    }

    @Test
    fun `balance warning display treats missing zero and invalid values as no reminder`() {
        assertEquals("不提醒", balanceWarningDisplay(emptyMap()))
        assertEquals("不提醒", balanceWarningDisplay(metadata("0")))
        assertEquals("不提醒", balanceWarningDisplay(metadata("-5")))
        assertEquals("不提醒", balanceWarningDisplay(metadata("not-a-number")))
    }

    private fun metadata(value: String) =
        mapOf(CredentialMetadataKey.BalanceWarningThreshold to value)
}
