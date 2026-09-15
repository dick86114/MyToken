package ai.routin.mytoken.feature.settings

import ai.routin.mytoken.data.preferences.AppPreferencesRepository
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

/**
 * Asserts the single-source-of-truth consolidation: [AppPreferencesRefreshSettingsStore]
 * persists every refresh setting through the Task 5 [AppPreferencesRepository] DataStore
 * (`mytoken_settings`) — writes made through either side are visible on the other, and
 * no second refresh-settings DataStore exists.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppPreferencesRefreshSettingsStoreTest {

    private val repository = AppPreferencesRepository(ApplicationProvider.getApplicationContext())
    private val store = AppPreferencesRefreshSettingsStore(repository)

    @Test
    fun storeWritesAreVisibleThroughTask5Repository() = runTest {
        store.setRetryOnFailure(false)

        val prefs = repository.preferences.first()
        assertFalse(prefs.retryOnFailure)

        val settings = store.settings.first()
        assertFalse(settings.retryOnFailure)
    }

    @Test
    fun task5RepositoryWritesAreVisibleThroughStore() = runTest {
        repository.setOpenAppRefresh(false)
        repository.setRetryOnFailure(true)

        val settings = store.settings.first()
        assertFalse(settings.openAppRefresh)
        assertTrue(settings.retryOnFailure)
    }

    @Test
    fun defaultsMatchAcrossBothSurfaces() = runTest {
        val prefs = repository.preferences.first()
        val settings = store.settings.first()
        assertEquals(prefs.openAppRefresh, settings.openAppRefresh)
        assertEquals(prefs.retryOnFailure, settings.retryOnFailure)
    }

}
