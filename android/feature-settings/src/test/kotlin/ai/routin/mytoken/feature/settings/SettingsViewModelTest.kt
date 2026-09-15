package ai.routin.mytoken.feature.settings

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var refreshStore: FakeRefreshSettingsStore
    private lateinit var displayStore: FakeDisplaySettingsStore
    private lateinit var notificationStore: FakeNotificationSettingsStore
    private lateinit var updateController: FakeAppUpdateController
    private lateinit var updateStore: FakeUpdateSettingsStore

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        refreshStore = FakeRefreshSettingsStore()
        displayStore = FakeDisplaySettingsStore()
        notificationStore = FakeNotificationSettingsStore()
        updateController = FakeAppUpdateController()
        updateStore = FakeUpdateSettingsStore()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SettingsViewModel(
        refreshStore,
        displayStore,
        notificationStore,
        updateStore,
        updateController,
    )

    @Test
    fun initialStateLoadsDefaults() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertTrue(state.refresh.openAppRefresh)
        assertTrue(state.refresh.retryOnFailure)
        assertTrue(state.display.showDisabledCredentials)
    }

    @Test
    fun releaseHistoryLoadForwardsToController() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.loadReleaseHistory()

        assertEquals(1, updateController.releaseHistoryLoaded)
    }

    @Test
    fun refreshTogglesPersistToStore() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.setOpenAppRefresh(false)
        vm.setRetryOnFailure(false)
        advanceUntilIdle()

        val refresh = refreshStore.state.value
        assertFalse(refresh.openAppRefresh)
        assertFalse(refresh.retryOnFailure)
    }

    @Test
    fun displayChangesPersistToStore() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.setShowDisabledCredentials(false)
        vm.setDefaultExpandGroups(false)
        vm.setShowUsageProgress(false)
        vm.setShowBalance(false)
        vm.setShowResetTime(false)
        advanceUntilIdle()

        val display = displayStore.state.value
        assertFalse(display.showDisabledCredentials)
        assertFalse(display.defaultExpandGroups)
        assertFalse(display.showUsageProgress)
        assertFalse(display.showBalance)
        assertFalse(display.showResetTime)
    }

    @Test
    fun initialStateLoadsNotificationDefaults() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        val notifications = vm.state.value.notifications
        assertTrue(notifications.credentialFailureAlertsEnabled)
    }

    @Test
    fun notificationTogglesPersistToStore() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.setCredentialFailureAlertsEnabled(false)
        advanceUntilIdle()

        assertFalse(notificationStore.state.value.credentialFailureAlertsEnabled)
    }

}
