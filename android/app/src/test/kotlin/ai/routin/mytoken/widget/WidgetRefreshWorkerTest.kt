package ai.routin.mytoken.widget

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.usage.RefreshStatus
import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
    fun `manual widget refresh updates every installed widget after one pass`() = runTest {
        val refreshed = mutableListOf<UUID>()
        var updateAllCalls = 0
        val coordinator = WidgetRefreshCoordinator(
            refreshCredential = { credential ->
                refreshed += credential.id
                RefreshStatus.Ready
            },
            updateAll = { updateAllCalls++ },
        )
        val first = credential()
        val second = credential()

        val success = coordinator.refresh(listOf(first, first, second))

        assertEquals(true, success)
        assertEquals(listOf(first.id, second.id), refreshed)
        assertEquals(1, updateAllCalls)
    }

    private fun credential() = Credential(
        id = UUID.randomUUID(),
        providerId = ProviderId.Routin,
        credentialKind = CredentialKind.BearerApiKey,
        name = "测试",
    )
}
