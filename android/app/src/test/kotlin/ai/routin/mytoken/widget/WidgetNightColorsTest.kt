package ai.routin.mytoken.widget

import ai.routin.mytoken.R
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class WidgetNightColorsTest {

    @Test
    @Config(sdk = [34], qualifiers = "notnight")
    fun `light surface uses light resource`() {
        assertNotEquals(
            ApplicationProvider.getApplicationContext<Context>().getColor(R.color.widget_glance_surface),
            NIGHT_SURFACE,
        )
    }

    @Test
    @Config(sdk = [34], qualifiers = "night")
    fun `night surface uses dark resource`() {
        assertEquals(
            NIGHT_SURFACE,
            ApplicationProvider.getApplicationContext<Context>().getColor(R.color.widget_glance_surface),
        )
    }

    private companion object {
        const val NIGHT_SURFACE = 0xFF172033.toInt()
    }
}
