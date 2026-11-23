package ai.routin.mytoken

import ai.routin.mytoken.core.alerts.AlertThresholds
import ai.routin.mytoken.domain.model.UsageHealthThresholdConfig
import ai.routin.mytoken.feature.settings.NotificationSettings
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageHealthThresholdSyncTest {

    @Test
    fun `source prefers per-credential overrides and falls back to the global config`() {
        val source = AppUsageHealthThresholdSource()
        val overridden = UUID.randomUUID()
        val global = UsageHealthThresholdConfig(lowPercent = 70, highPercent = 90)
        val custom = UsageHealthThresholdConfig(lowPercent = 90, highPercent = 97)

        source.update(
            AppliedHealthThresholds(
                global = global,
                perCredential = mapOf(overridden to custom),
            )
        )

        assertEquals(custom, source.thresholdsFor(overridden))
        assertEquals(global, source.thresholdsFor(UUID.randomUUID()))

        source.update(AppliedHealthThresholds(global = global, perCredential = emptyMap()))

        assertEquals(global, source.thresholdsFor(overridden))
    }

    @Test
    fun `invalid persisted global thresholds fall back to the shared defaults`() {
        val applied = appliedHealthThresholds(
            settings = NotificationSettings(
                lowThresholdPercent = 90,
                highThresholdPercent = 80,
            ),
            overrides = emptyMap(),
        )

        assertEquals(UsageHealthThresholdConfig.DEFAULT, applied.global)
    }

    @Test
    fun `per-credential overrides are mapped to health configs`() {
        val id = UUID.randomUUID()

        val applied = appliedHealthThresholds(
            settings = NotificationSettings(),
            overrides = mapOf(id to AlertThresholds(85, 96)),
        )

        assertEquals(
            UsageHealthThresholdConfig(lowPercent = 85, highPercent = 96),
            applied.perCredential[id],
        )
    }

    @Test
    fun `observer emits once for initial values and suppresses unrelated settings changes`() =
        runBlocking {
            val settings = MutableStateFlow(NotificationSettings())
            val overrides = MutableStateFlow<Map<UUID, AlertThresholds>>(emptyMap())
            val received = mutableListOf<AppliedHealthThresholds>()
            val job = launch(Dispatchers.Unconfined) {
                observeUsageHealthThresholdChanges(settings, overrides) { received += it }
            }

            assertEquals(1, received.size)

            settings.value = NotificationSettings(
                lowThresholdPercent = 85,
                highThresholdPercent = 96,
            )
            assertEquals(2, received.size)

            // Master switch / failure toggle do not alter health thresholds.
            settings.value = settings.value.copy(
                notificationsEnabled = false,
                credentialFailureAlertsEnabled = false,
            )
            assertEquals(2, received.size)

            overrides.value = mapOf(
                UUID.randomUUID() to AlertThresholds(90, 97),
            )
            assertEquals(3, received.size)

            job.cancel()
        }
}
