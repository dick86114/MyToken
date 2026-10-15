package ai.routin.mytoken.widget

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.usage.RefreshStatus
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ExistingWorkPolicy
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetRefreshWorkerTest {

    @Test
    fun `widget refresh unions credentials instead of refreshing per widget`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = WidgetConfigStore(context)
        val first = UUID.randomUUID()
        val second = UUID.randomUUID()
        store.save(1, WidgetConfig(kind = WidgetKind.Credential, credentialId = first))
        store.save(2, WidgetConfig(kind = WidgetKind.Credential, credentialId = second))
        store.save(
            3,
            WidgetConfig(
                kind = WidgetKind.Overview,
                overviewSource = WidgetOverviewSource.Manual,
                overviewCredentialIds = listOf(first, second),
            ),
        )

        assertEquals(setOf(first, second), store.requiredCredentialIds())
    }

    @Test
    fun `manual widget refresh touches every credential only once`() = runTest {
        val refreshed = mutableListOf<UUID>()
        val coordinator = WidgetRefreshCoordinator(
            refreshCredential = { credential ->
                refreshed += credential.id
                RefreshStatus.Ready
            },
        )
        val first = credential()
        val second = credential()

        val success = coordinator.refresh(listOf(first, first, second))

        assertEquals(true, success)
        assertEquals(listOf(first.id, second.id), refreshed)
    }

    @Test
    fun `failed credential is reported so the worker can retry`() = runTest {
        val coordinator = WidgetRefreshCoordinator(
            refreshCredential = { RefreshStatus.Failed },
        )

        assertEquals(false, coordinator.refresh(listOf(credential())))
    }

    @Test
    fun `manual refresh button marks state, enqueues once and redraws all widget kinds`() = runTest {
        val calls = mutableListOf<String>()

        WidgetRefreshTrigger.trigger(
            setRefreshing = { refreshing -> calls += "refreshing=$refreshing" },
            enqueueRefresh = { calls += "enqueue" },
            redrawAll = { calls += "redrawAll" },
        )

        // 只走全量重绘入口，避免用点击组件的 glanceId 去重绘另一种组件。
        assertEquals(listOf("refreshing=true", "enqueue", "redrawAll"), calls)
    }

    @Test
    fun `refresh work is kept instead of replaced while a refresh is running`() {
        assertEquals(ExistingWorkPolicy.KEEP, WidgetRefresher.existingWorkPolicy)
    }

    @Test
    fun `refresh retries a few times and then gives up so new taps can enqueue`() {
        assertEquals(WidgetRefreshOutcome.Success, widgetRefreshOutcome(succeeded = true, runAttemptCount = 0))
        assertEquals(WidgetRefreshOutcome.Retry, widgetRefreshOutcome(succeeded = false, runAttemptCount = 0))
        assertEquals(WidgetRefreshOutcome.Retry, widgetRefreshOutcome(succeeded = false, runAttemptCount = 1))
        assertEquals(
            WidgetRefreshOutcome.GiveUp,
            widgetRefreshOutcome(succeeded = false, runAttemptCount = MAX_REFRESH_ATTEMPTS),
        )
    }

    private fun credential() = Credential(
        id = UUID.randomUUID(),
        providerId = ProviderId.Routin,
        credentialKind = CredentialKind.BearerApiKey,
        name = "测试",
    )
}
