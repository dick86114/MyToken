package ai.routin.mytoken.data.alerts

import ai.routin.mytoken.core.alerts.AlertThresholds
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataStoreCredentialAlertThresholdStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = DataStoreCredentialAlertThresholdStore(context)

    private val first = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val second = UUID.fromString("00000000-0000-0000-0000-000000000002")

    @Test
    fun overridesPersistAndClearIndividually() = runTest {
        assertTrue(store.overrides.first().isEmpty())

        store.setOverride(first, AlertThresholds(70, 90))
        store.setOverride(second, AlertThresholds(60, 85))

        assertEquals(
            mapOf(
                first to AlertThresholds(70, 90),
                second to AlertThresholds(60, 85),
            ),
            store.overrides.first(),
        )

        store.clearOverride(first)

        assertEquals(mapOf(second to AlertThresholds(60, 85)), store.overrides.first())
    }

    @Test
    fun codecRoundTripsAndDropsMalformedEntries() {
        val encoded = CredentialAlertThresholdCodec.encode(
            mapOf(first to AlertThresholds(70, 90)),
        )
        assertEquals(mapOf(first to AlertThresholds(70, 90)), CredentialAlertThresholdCodec.decode(encoded))

        // Unknown UUIDs, non-object values and invalid thresholds are ignored on read.
        val damaged = """
            {
              "not-a-uuid": {"low": 70, "high": 90},
              "$second": "oops",
              "00000000-0000-0000-0000-000000000003": {"low": 90, "high": 80},
              "00000000-0000-0000-0000-000000000004": {"low": 0, "high": 90}
            }
        """.trimIndent()
        assertTrue(CredentialAlertThresholdCodec.decode(damaged).isEmpty())
        assertTrue(CredentialAlertThresholdCodec.decode("not json").isEmpty())
        assertTrue(CredentialAlertThresholdCodec.decode("").isEmpty())
    }
}
