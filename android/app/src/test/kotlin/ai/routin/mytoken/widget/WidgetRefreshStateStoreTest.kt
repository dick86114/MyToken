package ai.routin.mytoken.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetRefreshStateStoreTest {

    @Test
    fun `refreshing flag is only true while the pass is recent`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val start = Instant.parse("2026-01-01T00:00:00Z")
        val store = WidgetRefreshStateStore(context, Clock.fixed(start, ZoneOffset.UTC))

        store.setRefreshing(true)
        assertTrue(store.isRefreshing())

        // 进程被杀或网络约束一直不满足时，刷新态必须在窗口后自动失效。
        val later = WidgetRefreshStateStore(
            context,
            Clock.fixed(start.plusSeconds(60 * 6), ZoneOffset.UTC),
        )
        assertFalse(later.isRefreshing())
    }

    @Test
    fun `clearing refresh state stops the spinner immediately`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = WidgetRefreshStateStore(context)

        store.setRefreshing(true)
        store.setRefreshing(false)

        assertFalse(store.isRefreshing())
    }
}
