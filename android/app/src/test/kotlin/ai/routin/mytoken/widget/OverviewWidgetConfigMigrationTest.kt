package ai.routin.mytoken.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OverviewWidgetConfigMigrationTest {

    @Test
    fun `new overview widget defaults to all enabled auto style`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("mytoken_widgets", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        val config = WidgetConfigStore(context).load(42, WidgetKind.Overview)

        assertEquals(WidgetKind.Overview, config.kind)
        assertEquals(WidgetOverviewSource.AllEnabled, config.overviewSource)
        assertEquals(WidgetDisplayStyle.Auto, config.style)
    }
}
