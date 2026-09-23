package ai.routin.mytoken.widget

import android.view.LayoutInflater
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import ai.routin.mytoken.R
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetMetricLayoutTest {
    @Test
    fun 重置时间允许换行且不显示省略号() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val item = LayoutInflater.from(context).inflate(R.layout.widget_metric_item, null)
        val reset = item.findViewById<TextView>(R.id.widget_metric_reset)

        assertNull(reset.ellipsize)
        assertTrue(reset.maxLines > 1)

        val detail = item.findViewById<TextView>(R.id.widget_metric_detail)
        assertNull(detail.ellipsize)
        assertTrue(detail.maxLines > 1)

        val value = item.findViewById<TextView>(R.id.widget_metric_value)
        assertNull(value.ellipsize)
        assertTrue(value.maxLines > 1)
    }
}
