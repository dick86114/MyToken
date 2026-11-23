package ai.routin.mytoken.core.alerts

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure coverage of the dispatcher gate and threshold/override assembly. */
class AlertDispatchPolicyTest {

    private val credentialId = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @Test
    fun dispatchRequiresBothTheMasterSwitchAndSystemPermission() {
        assertTrue(AlertDispatchPolicy.shouldDispatch(true, true))
        assertFalse(AlertDispatchPolicy.shouldDispatch(false, true))
        assertFalse(AlertDispatchPolicy.shouldDispatch(true, false))
        assertFalse(AlertDispatchPolicy.shouldDispatch(false, false))
    }

    @Test
    fun settingsUseConfiguredThresholdsAndPerCredentialOverrides() {
        val override = AlertThresholds(lowPercent = 70, highPercent = 90)
        val settings = AlertDispatchPolicy.alertSettings(
            lowPercent = 60,
            highPercent = 85,
            perCredential = mapOf(credentialId to override),
        )

        assertEquals(AlertThresholds(60, 85), settings.thresholds)
        assertEquals(override, settings.perCredential[credentialId])
    }

    @Test
    fun invalidPersistedThresholdsFallBackToSharedDefaults() {
        val settings = AlertDispatchPolicy.alertSettings(lowPercent = 95, highPercent = 80)
        assertEquals(AlertThresholds.DEFAULT, settings.thresholds)

        val outOfRange = AlertDispatchPolicy.alertSettings(lowPercent = 0, highPercent = 100)
        assertEquals(AlertThresholds.DEFAULT, outOfRange.thresholds)
    }
}
