package ai.routin.mytoken.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

object WidgetRingRenderer {
    private const val DEFAULT_SIZE_PX = 192
    private val cache = mutableMapOf<Long, Bitmap>()

    fun sweepAngle(percent: Int?): Float =
        ((percent ?: 0).coerceIn(0, 100) / 100f) * 360f

    @Synchronized
    fun bitmap(
        percent: Int?,
        color: Int,
        trackColor: Int,
        sizePx: Int = DEFAULT_SIZE_PX,
    ): Bitmap {
        val key = (percent?.toLong() ?: -1L) shl 40 or
            (color.toLong() and 0xFFFFFFFFL) shl 8 or
            (trackColor.toLong() and 0xFFL)
        cache[key]?.let { return it }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val stroke = sizePx * 0.12f
        val inset = stroke / 2f
        val rect = RectF(inset, inset, sizePx - inset, sizePx - inset)
        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            this.color = trackColor
        }
        val progress = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            this.color = color
        }
        canvas.drawArc(rect, 0f, 360f, false, track)
        val sweep = sweepAngle(percent)
        if (sweep > 0f) {
            canvas.drawArc(rect, -90f, sweep, false, progress)
        }
        cache[key] = bitmap
        return bitmap
    }
}
