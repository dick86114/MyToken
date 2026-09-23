package ai.routin.mytoken.update

import ai.routin.mytoken.feature.settings.AppReleaseHistoryItem
import ai.routin.mytoken.feature.settings.AppReleaseHistoryUiState
import ai.routin.mytoken.feature.settings.ReleaseNotesCacheStore
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReleaseHistoryCacheRegressionTest {
    private val currentRelease = release("1.1.0", "当前版本")
    private val oldRelease = release("1.0.0", "旧版本")

    @Test
    fun 同版本缓存重启后不发起网络请求() = runBlocking {
        val requestCount = AtomicInteger()
        val cache = FakeReleaseNotesCacheStore(listOf(currentRelease, oldRelease))
        val controller = controller(cache, requestCount)

        controller.loadCachedReleaseHistory()
        withTimeout(5_000) {
            controller.releaseHistoryState.first { it is AppReleaseHistoryUiState.Loaded }
        }

        assertEquals(0, requestCount.get())
    }

    @Test
    fun 版本更新后的旧缓存会自动获取并回写() = runBlocking {
        val requestCount = AtomicInteger()
        val cache = FakeReleaseNotesCacheStore(listOf(oldRelease))
        val controller = controller(cache, requestCount)

        controller.loadCachedReleaseHistory()
        val state = withTimeout(5_000) {
            controller.releaseHistoryState.first {
                it is AppReleaseHistoryUiState.Loaded &&
                    (it as AppReleaseHistoryUiState.Loaded).releases.any { release ->
                        release.version == "1.1.0"
                    }
            }
        } as AppReleaseHistoryUiState.Loaded

        assertEquals(1, requestCount.get())
        assertTrue(state.releases.any { it.version == "1.1.0" })
        assertEquals(listOf(currentRelease, oldRelease), cache.snapshot.first())
    }

    private fun controller(
        cache: ReleaseNotesCacheStore,
        requestCount: AtomicInteger,
    ) = GitHubAppUpdateController(
        context = ApplicationProvider.getApplicationContext<Context>(),
        currentVersionName = "1.1.0",
        cacheStore = cache,
        network = { _, _ ->
            requestCount.incrementAndGet()
            UpdateResponse(
                """
                [
                  {"tag_name":"android-v1.1.0","html_url":"https://example.com/1.1.0","body":"当前版本"},
                  {"tag_name":"android-v1.0.0","html_url":"https://example.com/1.0.0","body":"旧版本"}
                ]
                """.trimIndent(),
            )
        },
    )

    private fun release(version: String, notes: String) = AppReleaseHistoryItem(
        version = version,
        releaseNotes = notes,
        releaseUrl = "https://example.com/$version",
        publishedAt = null,
    )

    private class FakeReleaseNotesCacheStore(
        initial: List<AppReleaseHistoryItem>,
    ) : ReleaseNotesCacheStore {
        private val state = MutableStateFlow(initial)
        override val cachedReleases: Flow<List<AppReleaseHistoryItem>?> = state
        val snapshot: Flow<List<AppReleaseHistoryItem>?> = state

        override suspend fun save(releases: List<AppReleaseHistoryItem>) {
            state.value = releases
        }
    }
}
