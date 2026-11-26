package ai.routin.mytoken.core.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.concurrent.ConcurrentHashMap

/**
 * 供应商 logo 的明暗分类：解码一次资源像素，取不透明像素的平均亮度。
 *
 * 左右两套外观共用品牌原色，深色外观下纯黑 logo 落在卡片色底盘上会消失；
 * 这里只负责判断“这个 logo 是否需要浅色底盘”，不修改图片本身。
 * 结果按 drawable 资源 id 进程内缓存，旋转、重绘和重组都不会重复解码。
 */
object ProviderLogoInk {
    /** 平均亮度低于该值的 logo 视为深色 logo（0..1，Rec.709 luma）。 */
    const val DARK_LOGO_LUMINANCE_THRESHOLD = 0.35f

    /** 只统计不透明像素；半透明抗锯齿边缘不计入平均值。 */
    private const val OPAQUE_ALPHA_THRESHOLD = 128

    private val darkLogoCache = ConcurrentHashMap<Int, Boolean>()

    /** 深色外观下该 logo 是否需要中性浅色底盘。 */
    fun isDarkLogo(context: Context, @DrawableRes resId: Int): Boolean =
        darkLogoCache[resId] ?: analyze(context, resId).also { darkLogoCache[resId] = it }

    private fun analyze(context: Context, @DrawableRes resId: Int): Boolean {
        val bitmap = BitmapFactory.decodeResource(context.resources, resId) ?: return false
        return try {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            averageOpaqueLuminance(pixels) < DARK_LOGO_LUMINANCE_THRESHOLD
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * 不透明像素的平均亮度（Rec.709 luma，0..1）。
     * 没有不透明像素时返回 1，视为浅色 logo，避免给空图片垫浅色块。
     */
    internal fun averageOpaqueLuminance(pixels: IntArray): Float {
        var sum = 0.0
        var count = 0
        for (pixel in pixels) {
            val alpha = (pixel ushr 24) and 0xFF
            if (alpha < OPAQUE_ALPHA_THRESHOLD) continue
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            sum += 0.2126 * red + 0.7152 * green + 0.0722 * blue
            count++
        }
        if (count == 0) return 1f
        return (sum / count / 255.0).toFloat()
    }
}

/** 组合内读取 logo 明暗；同一资源 id 只解码一次。 */
@Composable
fun rememberProviderLogoIsDark(@DrawableRes resId: Int): Boolean {
    val context = LocalContext.current
    return remember(resId) { ProviderLogoInk.isDarkLogo(context, resId) }
}
