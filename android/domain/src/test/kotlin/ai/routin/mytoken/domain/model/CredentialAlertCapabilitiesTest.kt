package ai.routin.mytoken.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CredentialAlertCapabilitiesTest {

    @Test
    fun `deepseek hides percent thresholds but exposes balance warning`() {
        assertFalse(
            CredentialAlertCapabilities.supportsPercentAlertThreshold(ProviderId.DeepSeek, "api")
        )
        assertTrue(
            CredentialAlertCapabilities.supportsBalanceWarningThreshold(ProviderId.DeepSeek, "api")
        )
    }

    @Test
    fun `xiaomi visibility follows the usage kind`() {
        assertFalse(
            CredentialAlertCapabilities.supportsPercentAlertThreshold(ProviderId.Xiaomi, "api")
        )
        assertTrue(
            CredentialAlertCapabilities.supportsBalanceWarningThreshold(ProviderId.Xiaomi, "api")
        )
        assertTrue(
            CredentialAlertCapabilities.supportsPercentAlertThreshold(ProviderId.Xiaomi, "plan")
        )
        assertFalse(
            CredentialAlertCapabilities.supportsBalanceWarningThreshold(ProviderId.Xiaomi, "plan")
        )
    }

    @Test
    fun `xiaomi without usage kind exposes neither setting`() {
        assertFalse(
            CredentialAlertCapabilities.supportsPercentAlertThreshold(ProviderId.Xiaomi, null)
        )
        assertFalse(
            CredentialAlertCapabilities.supportsBalanceWarningThreshold(ProviderId.Xiaomi, null)
        )
    }

    @Test
    fun `glm and newapi expose both settings`() {
        listOf(ProviderId.Glm, ProviderId.NewAPI).forEach { providerId ->
            assertTrue(
                CredentialAlertCapabilities.supportsPercentAlertThreshold(providerId, "api"),
                "$providerId should support percent thresholds",
            )
            assertTrue(
                CredentialAlertCapabilities.supportsBalanceWarningThreshold(providerId, "api"),
                "$providerId should support balance warnings",
            )
        }
    }

    @Test
    fun `quota only providers expose percent thresholds and hide balance warnings`() {
        listOf(
            ProviderId.Routin,
            ProviderId.CommandCode,
            ProviderId.OpenCode,
            ProviderId.Volcengine,
        ).forEach { providerId ->
            assertTrue(
                CredentialAlertCapabilities.supportsPercentAlertThreshold(providerId, "api"),
                "$providerId should support percent thresholds",
            )
            assertFalse(
                CredentialAlertCapabilities.supportsBalanceWarningThreshold(providerId, "api"),
                "$providerId should not support balance warnings",
            )
        }
    }

    @Test
    fun `usage kind matching is trimmed and case insensitive`() {
        assertTrue(
            CredentialAlertCapabilities.supportsPercentAlertThreshold(ProviderId.Xiaomi, " PLAN ")
        )
        assertTrue(
            CredentialAlertCapabilities.supportsBalanceWarningThreshold(ProviderId.Xiaomi, " API ")
        )
    }
}
