package ai.routin.mytoken.core.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MyTokenBrandMarkTest {

    @Test
    fun `brand logo inverts with dark appearance`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val light = opaqueLuminance(context, brandLogoResource(darkTheme = false))
        val dark = opaqueLuminance(context, brandLogoResource(darkTheme = true))

        assertNotEquals(brandLogoResource(darkTheme = false), brandLogoResource(darkTheme = true))
        assertTrue("浅色外观应使用深色 logo（实际亮度 $light）", light < 0.2)
        assertTrue("深色外观应使用反色 logo（实际亮度 $dark）", dark > 0.8)
    }

    private fun opaqueLuminance(context: Context, drawableRes: Int): Double {
        val bitmap = BitmapFactory.decodeResource(context.resources, drawableRes)
            ?: error("logo asset could not be decoded")
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var sum = 0.0
        var count = 0
        pixels.forEach { argb ->
            val alpha = argb ushr 24
            if (alpha < 200) return@forEach
            val r = (argb shr 16) and 0xFF
            val g = (argb shr 8) and 0xFF
            val b = argb and 0xFF
            sum += (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
            count += 1
        }
        require(count > 0) { "logo asset has no opaque pixels" }
        return sum / count
    }
}
