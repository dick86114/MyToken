package ai.routin.mytoken.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetConfigStoreTest {

    @Test
    fun `legacy credential selection migrates without losing widget id`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val credentialId = UUID.randomUUID()
        context.getSharedPreferences("mytoken_widgets", Context.MODE_PRIVATE)
            .edit()
            .putString("credential_7", credentialId.toString())
            .commit()
        val store = WidgetConfigStore(context)

        val config = store.load(appWidgetId = 7, kind = WidgetKind.Credential)

        assertEquals(WidgetKind.Credential, config.kind)
        assertEquals(credentialId, config.credentialId)
        assertEquals(WidgetDisplayStyle.Auto, config.style)
        assertEquals(null, config.primaryMetricId)
        assertNotNull(
            context.getSharedPreferences("mytoken_widgets", Context.MODE_PRIVATE)
                .getString("credential_7", null)
        )
    }

    @Test
    fun `manual overview is capped at six credentials and preserves order`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val ids = List(8) { UUID.randomUUID() }
        val store = WidgetConfigStore(context)

        store.save(
            9,
            WidgetConfig(
                kind = WidgetKind.Overview,
                overviewCredentialIds = ids,
                overviewSource = WidgetOverviewSource.Manual,
            ),
        )

        assertEquals(ids.take(6), store.load(9, WidgetKind.Overview).overviewCredentialIds)
    }

    @Test
    fun `corrupt v2 config falls back to automatic credential widget`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("mytoken_widgets", Context.MODE_PRIVATE)
            .edit()
            .putString("config_11", "{not-json")
            .commit()

        val config = WidgetConfigStore(context).load(11, WidgetKind.Credential)

        assertEquals(WidgetConfig(kind = WidgetKind.Credential), config)
    }
}
