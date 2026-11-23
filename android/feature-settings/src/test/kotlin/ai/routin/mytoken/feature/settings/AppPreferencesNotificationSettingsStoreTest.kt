package ai.routin.mytoken.feature.settings

import ai.routin.mytoken.data.preferences.AppPreferencesRepository
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Single-source-of-truth check: notification settings persist into the Task 5 DataStore. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppPreferencesNotificationSettingsStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repository = AppPreferencesRepository(context)
    private val store = AppPreferencesNotificationSettingsStore(repository)

    @Test
    fun defaultsThenPersistedChangesFlowThroughAppPreferencesRepository() = runTest {
        // Defaults (single ordered test: the DataStore instance is process-local).
        val defaults = store.settings.first()
        assertTrue(defaults.notificationsEnabled)
        assertTrue(defaults.credentialFailureAlertsEnabled)
        assertEquals(80, defaults.lowThresholdPercent)
        assertEquals(95, defaults.highThresholdPercent)

        // Writes persist and stay visible through the Task 5 repository (no second DataStore).
        store.setNotificationsEnabled(false)
        store.setThresholds(70, 90)
        store.setCredentialFailureAlertsEnabled(false)

        val settings = store.settings.first()
        assertFalse(settings.notificationsEnabled)
        assertFalse(settings.credentialFailureAlertsEnabled)
        assertEquals(70, settings.lowThresholdPercent)
        assertEquals(90, settings.highThresholdPercent)
        assertEquals(70, repository.preferences.first().alertLowThresholdPercent)
        assertEquals(90, repository.preferences.first().alertHighThresholdPercent)
    }

}
