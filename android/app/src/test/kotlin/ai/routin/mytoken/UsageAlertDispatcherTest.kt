package ai.routin.mytoken

import ai.routin.mytoken.core.alerts.AlertNotificationState
import ai.routin.mytoken.core.alerts.AlertStateStore
import ai.routin.mytoken.core.alerts.AlertThresholds
import ai.routin.mytoken.core.alerts.CredentialAlertThresholdStore
import ai.routin.mytoken.core.notifications.NotificationChannels
import ai.routin.mytoken.domain.model.AppError
import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import ai.routin.mytoken.domain.model.UsageSnapshot
import ai.routin.mytoken.domain.repository.CredentialRepository
import ai.routin.mytoken.domain.usage.CredentialUsageState
import ai.routin.mytoken.domain.usage.RefreshStatus
import ai.routin.mytoken.feature.settings.NotificationSettings
import ai.routin.mytoken.feature.settings.NotificationSettingsStore
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Robolectric coverage of the notification gate, threshold assembly and delivery. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UsageAlertDispatcherTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val credentialId = UUID.fromString("00000000-0000-0000-0000-000000000001")

    private val repository = FakeCredentialRepository()
    private val settingsStore = FakeNotificationSettingsStore()
    private val thresholdStore = FakeCredentialAlertThresholdStore()
    private val alertStateStore = InMemoryAlertStateStore()
    private val states = MutableStateFlow<Map<UUID, CredentialUsageState>>(emptyMap())

    private lateinit var dispatcher: UsageAlertDispatcher

    @Before
    fun setUp() {
        shadowOf(manager).setNotificationsEnabled(true)
        repository.credentials.value = listOf(
            Credential(
                id = credentialId,
                providerId = ProviderId.Glm,
                credentialKind = CredentialKind.ApiKey,
                name = "GLM Coding",
            ),
        )
        dispatcher = UsageAlertDispatcher(
            context = context,
            repository = repository,
            states = states,
            alertStateStore = alertStateStore,
            notificationSettingsStore = settingsStore,
            credentialAlertThresholdStore = thresholdStore,
        )
    }

    private fun readyState(usedPercent: Int): CredentialUsageState = CredentialUsageState(
        status = RefreshStatus.Ready,
        snapshot = UsageSnapshot(
            credentialId = credentialId,
            fetchedAt = Instant.parse("2026-09-07T12:00:00Z"),
            metrics = listOf(
                UsageMetric(
                    id = "fiveHour",
                    label = "5 小时用量",
                    used = BigDecimal(usedPercent),
                    limit = BigDecimal(100),
                    remaining = BigDecimal(100 - usedPercent),
                    unit = UsageMetricUnit.Request,
                    presentation = UsageMetricPresentation.Progress,
                    semantic = UsageMetricSemantic.UsedQuota,
                    healthState = ai.routin.mytoken.domain.model.UsageMetricHealthState.Normal,
                ),
            ),
        ),
    )

    private fun notifications(): List<Notification> =
        shadowOf(manager).allNotifications.toList()

    @Test
    fun postsUsageAlertWithConfiguredThresholdsAndNotificationCopy() = runTest {
        states.value = mapOf(credentialId to readyState(usedPercent = 85))
        settingsStore.state.value = NotificationSettings(
            lowThresholdPercent = 80,
            highThresholdPercent = 95,
        )

        dispatcher.dispatchAfterRefresh()

        val notification = notifications().single()
        assertEquals(
            "用量接近上限",
            notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString(),
        )
        assertEquals(
            "GLM Coding · 5 小时用量 · 85% 已用",
            notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString(),
        )
        assertEquals(NotificationChannels.USAGE_ALERTS_ID, notification.channelId)
        assertTrue(alertStateStore.state.triggeredWindows.isNotEmpty())
    }

    @Test
    fun savedThresholdsReplaceTheSharedDefaults() = runTest {
        states.value = mapOf(credentialId to readyState(usedPercent = 60))
        settingsStore.state.value = NotificationSettings(
            lowThresholdPercent = 50,
            highThresholdPercent = 90,
        )

        dispatcher.dispatchAfterRefresh()

        assertEquals(
            "用量接近上限",
            notifications().single().extras.getCharSequence(Notification.EXTRA_TITLE).toString(),
        )
    }

    @Test
    fun perCredentialOverrideWinsOverGlobalThresholds() = runTest {
        states.value = mapOf(credentialId to readyState(usedPercent = 30))
        thresholdStore.state.value = mapOf(credentialId to AlertThresholds(20, 90))

        dispatcher.dispatchAfterRefresh()

        assertEquals(1, notifications().size)
    }

    @Test
    fun masterSwitchOffSuppressesAllNotifications() = runTest {
        states.value = mapOf(credentialId to readyState(usedPercent = 99))
        settingsStore.state.value = NotificationSettings(notificationsEnabled = false)

        dispatcher.dispatchAfterRefresh()

        assertTrue(notifications().isEmpty())
        assertTrue(alertStateStore.state.triggeredWindows.isEmpty())
    }

    @Test
    fun systemPermissionDeniedSuppressesAllNotifications() = runTest {
        states.value = mapOf(credentialId to readyState(usedPercent = 99))
        shadowOf(manager).setNotificationsEnabled(false)

        dispatcher.dispatchAfterRefresh()

        assertTrue(notifications().isEmpty())
    }

    @Test
    fun credentialFailureAlertsHonorTheInAppSwitch() = runTest {
        states.value = mapOf(
            credentialId to CredentialUsageState(
                status = RefreshStatus.Failed,
                error = AppError.Authentication("凭证已失效"),
            ),
        )
        settingsStore.state.value = NotificationSettings(credentialFailureAlertsEnabled = false)

        dispatcher.dispatchAfterRefresh()
        assertTrue(notifications().isEmpty())

        settingsStore.state.value = NotificationSettings(credentialFailureAlertsEnabled = true)
        dispatcher.dispatchAfterRefresh()

        val notification = notifications().single()
        assertEquals("凭证已失效", notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals(NotificationChannels.CREDENTIAL_ERRORS_ID, notification.channelId)
    }
}

private class FakeCredentialRepository : CredentialRepository {
    val credentials = MutableStateFlow<List<Credential>>(emptyList())

    override fun observeCredentials(): Flow<List<Credential>> = credentials
    override suspend fun save(credential: Credential, secret: ai.routin.mytoken.domain.model.CredentialSecret) = Unit
    override suspend fun delete(id: UUID) = Unit
    override suspend fun readSecret(id: UUID): ai.routin.mytoken.domain.model.CredentialSecret? = null
    override suspend fun cacheSnapshot(snapshot: UsageSnapshot) = Unit
    override suspend fun cachedSnapshot(id: UUID): UsageSnapshot? = null
}

private class FakeNotificationSettingsStore(
    initial: NotificationSettings = NotificationSettings(),
) : NotificationSettingsStore {
    val state = MutableStateFlow(initial)
    override val settings: StateFlow<NotificationSettings> = state

    override suspend fun setNotificationsEnabled(enabled: Boolean) =
        state.update { it.copy(notificationsEnabled = enabled) }

    override suspend fun setThresholds(lowPercent: Int, highPercent: Int) =
        state.update { it.copy(lowThresholdPercent = lowPercent, highThresholdPercent = highPercent) }

    override suspend fun setCredentialFailureAlertsEnabled(enabled: Boolean) =
        state.update { it.copy(credentialFailureAlertsEnabled = enabled) }
}

private class FakeCredentialAlertThresholdStore : CredentialAlertThresholdStore {
    val state = MutableStateFlow<Map<UUID, AlertThresholds>>(emptyMap())
    override val overrides: Flow<Map<UUID, AlertThresholds>> = state

    override suspend fun setOverride(credentialId: UUID, thresholds: AlertThresholds) {
        state.update { it + (credentialId to thresholds) }
    }

    override suspend fun clearOverride(credentialId: UUID) {
        state.update { it - credentialId }
    }
}

private class InMemoryAlertStateStore : AlertStateStore {
    var state = AlertNotificationState()

    override suspend fun load(): AlertNotificationState = state
    override suspend fun save(state: AlertNotificationState) {
        this.state = state
    }
}
