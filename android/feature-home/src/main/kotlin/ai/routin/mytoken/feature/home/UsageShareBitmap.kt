package ai.routin.mytoken.feature.home

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.content.FileProvider
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageSnapshot
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val BG = 0xFF181A22.toInt()
private const val HEADER_TOP = 0xFF222733.toInt()
private const val HEADER_BOT = 0xFF171922.toInt()
private const val PANEL = 0xFF0F172A.toInt()
private const val TEAR_BG = 0xFF14161F.toInt()
private const val FOOTER_BG = 0xFF14171F.toInt()
private const val MUTED = 0xFF94A3B8.toInt()
private const val LIGHT_TEXT = 0xFFF0F4F8.toInt()
private const val AMBER = 0xFFFBBF24.toInt()
private const val AMBER_LIGHT = 0xFFFCD34D.toInt()
private const val GREEN = 0xFF34D399.toInt()
private const val BLUE = 0xFF3B82F6.toInt()
private const val TRACK = 0xFF1E293B.toInt()
private const val HAIRLINE = 0x141E293B.toInt()

private const val WEBSITE = "https://mytoken.idickies.cc"
private const val TAGLINE = "AI 用量，一目了然"

internal fun createShareBitmap(
    displayName: String,
    providerName: String,
    planName: String,
    snapshot: UsageSnapshot?,
    density: Float = 3f,
): Bitmap {
    val w = (360 * density).toInt()
    val metrics = snapshot?.metrics.orEmpty().filter { it.presentation == UsageMetricPresentation.Progress }
    val tiles = snapshot?.metrics.orEmpty().filter { it.presentation != UsageMetricPresentation.Progress }
    val d = density
    val rowH = (52 * d).toInt()
    val headerH = (100 * d).toInt()
    val tearH = (14 * d).toInt()
    val footerH = (52 * d).toInt()
    val padding = (16 * d).toInt()
    val gaugeH = (78 * d).toInt()
    val h = headerH + tearH + footerH + padding * 4 + metrics.size * gaugeH + (if (tiles.isNotEmpty()) (50 * d).toInt() else 0)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 全背景
    paint.color = BG
    c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

    // 顶部渐变 header
    paint.shader = LinearGradient(0f, 0f, w.toFloat(), headerH.toFloat(), HEADER_TOP, HEADER_BOT, Shader.TileMode.CLAMP)
    c.drawRect(0f, 0f, w.toFloat(), headerH.toFloat(), paint)
    paint.shader = null

    // 账户名
    paint.color = -1 // white
    paint.textSize = 18 * d
    paint.isFakeBoldText = true
    c.drawText(displayName, padding.toFloat(), (28 * d), paint)
    paint.isFakeBoldText = false

    // 供应商 · 套餐
    paint.color = MUTED
    paint.textSize = 12 * d
    val sub = listOfNotNull(providerName, planName.takeIf { it.isNotBlank() }).joinToString(" · ")
    c.drawText(sub, padding.toFloat(), (46 * d), paint)

    // 订阅周期
    snapshot?.subscriptionEndAt?.let { end ->
        val fmt = DateTimeFormatter.ofPattern("MM.dd").withZone(ZoneId.systemDefault())
        val days = java.time.Duration.between(java.time.Instant.now(), end).toDays().coerceAtLeast(1)
        paint.color = LIGHT_TEXT
        paint.textSize = 11 * d
        c.drawText("余 ${days}天 (${fmt.format(end)})", padding.toFloat(), (66 * d), paint)
    }

    // 撕线
    val tearY = headerH + tearH / 2f
    paint.color = TEAR_BG
    c.drawRect(0f, headerH.toFloat(), w.toFloat(), (headerH + tearH).toFloat(), paint)
    paint.color = 0xFF475569.toInt()
    paint.strokeWidth = 1.5f * d
    var x = 20 * d
    while (x < w - 20 * d) {
        c.drawLine(x, tearY, x + 5 * d, tearY, paint)
        x += 9 * d
    }

    // 指标
    var y = headerH + tearH + padding
    metrics.forEach { metric ->
        val pct = metric.displayedPercent()
        // gauge 面板
        paint.color = PANEL
        c.drawRoundRect(RectF(padding.toFloat(), y.toFloat(), (w - padding).toFloat(), (y + gaugeH - 8 * d).toFloat()), 12 * d, 12 * d, paint)

        val gx = padding + 12 * d
        val gw = w - 2 * padding - 24 * d
        var gy = y + 14 * d

        // 标题
        paint.color = 0xFFCBD5E1.toInt()
        paint.textSize = 11 * d
        c.drawText(metric.label, gx, gy, paint)
        // 百分比
        paint.color = AMBER_LIGHT
        paint.textSize = 13 * d
        paint.isFakeBoldText = true
        c.drawText(pct ?: "—", gx + gw, gy, paint.apply { textAlign = Paint.Align.RIGHT })
        paint.textAlign = Paint.Align.LEFT
        paint.isFakeBoldText = false
        gy += 14 * d

        // 进度条
        if (pct != null) {
            val pctVal = pct.removeSuffix("%").toFloatOrNull() ?: 0f
            paint.color = TRACK
            c.drawRoundRect(RectF(gx, gy, gx + gw, gy + 6 * d), 3 * d, 3 * d, paint)
            paint.shader = LinearGradient(gx, 0f, gx + gw, 0f, BLUE, AMBER, Shader.TileMode.CLAMP)
            val barW = gw * (pctVal / 100f).coerceIn(0.01f, 1f)
            c.drawRoundRect(RectF(gx, gy, gx + barW, gy + 6 * d), 3 * d, 3 * d, paint)
            paint.shader = null
            gy += 16 * d
        }

        // 已用/上限
        val used = metric.formatAmount(metric.used)
        val limit = metric.formatAmount(metric.limit)
        if (used != null || limit != null) {
            paint.color = MUTED
            paint.textSize = 10 * d
            c.drawText("已用 ${used ?: "—"}", gx, gy, paint)
            paint.textAlign = Paint.Align.RIGHT
            c.drawText("上限 ${limit ?: "—"}", gx + gw, gy, paint)
            paint.textAlign = Paint.Align.LEFT
            gy += 14 * d
        }

        // 剩余
        val remaining = metric.formatAmount(metric.remaining)
        if (remaining != null) {
            paint.color = GREEN
            c.drawText("剩余 $remaining", gx, gy, paint)
        }

        y += gaugeH
    }

    // tiles
    if (tiles.isNotEmpty()) {
        val tw = (w - 3 * padding) / 2
        tiles.take(2).forEachIndexed { i, tile ->
            val tx = padding + i * (tw + padding / 2)
            paint.color = PANEL
            c.drawRoundRect(RectF(tx.toFloat(), y.toFloat(), (tx + tw).toFloat(), (y + 44 * d).toFloat()), 10 * d, 10 * d, paint)
            paint.color = MUTED
            paint.textSize = 9 * d
            c.drawText(tile.label, tx + 8 * d, y + 16 * d, paint)
            paint.color = -1
            paint.textSize = 12 * d
            paint.isFakeBoldText = true
            c.drawText(tile.displayedPercent() ?: "—", tx + 8 * d, y + 34 * d, paint)
            paint.isFakeBoldText = false
        }
        y += (52 * d).toInt()
    }

    // 品牌栏
    val footerY = h - footerH
    paint.color = FOOTER_BG
    c.drawRect(0f, footerY.toFloat(), w.toFloat(), h.toFloat(), paint)

    paint.color = -1
    paint.textSize = 12 * d
    paint.isFakeBoldText = true
    c.drawText("MyToken", padding.toFloat(), footerY + 16 * d, paint)
    paint.isFakeBoldText = false
    paint.color = MUTED
    paint.textSize = 10 * d
    c.drawText(TAGLINE, padding.toFloat(), footerY + 30 * d, paint)
    c.drawText(WEBSITE, padding.toFloat(), footerY + 43 * d, paint)

    // 二维码（简化方点）
    val qrSize = (44 * d).toInt()
    val qrX = w - padding - qrSize
    val qrY = footerY + 4 * d
    paint.color = -1
    c.drawRect(qrX.toFloat(), qrY.toFloat(), (qrX + qrSize).toFloat(), (qrY + qrSize).toFloat(), paint)
    paint.color = 0xFF000000.toInt()
    val cell = qrSize / 21f
    val seed = WEBSITE.hashCode()
    for (row in 0 until 21) {
        for (col in 0 until 21) {
            val v = ((row * 31 + col * 17 + seed) % 7)
            if (v < 3) {
                c.drawRect(qrX + col * cell, qrY + row * cell, qrX + (col + 1) * cell, qrY + (row + 1) * cell, paint)
            }
        }
    }

    return bitmap
}

private fun UsageMetric.displayedPercent(): String? {
    val lim = limit ?: return null
    if (lim.signum() <= 0) return null
    val num = when (semantic) {
        UsageMetricSemantic.UsedQuota -> used
        UsageMetricSemantic.RemainingQuota -> remaining
        else -> return null
    } ?: return null
    val pct = num.divide(lim, 2, RoundingMode.HALF_UP).multiply(BigDecimal(100))
    return "${pct.stripTrailingZeros().toPlainString()}%"
}

private fun UsageMetric.formatAmount(value: BigDecimal?): String? {
    value ?: return null
    val s = value.stripTrailingZeros().toPlainString()
    return if (unit == ai.routin.mytoken.domain.model.UsageMetricUnit.Currency) "${currencyCode ?: "$"}$s" else s
}

internal fun shareUsageImage(
    context: Context,
    displayName: String,
    providerName: String,
    planName: String,
    snapshot: UsageSnapshot?,
    targetPackage: String? = null,
) {
    try {
        val density = context.resources.displayMetrics.density
        val bitmap = createShareBitmap(displayName, providerName, planName, snapshot, density)
        val dir = File(context.cacheDir, "share")
        dir.mkdirs()
        val file = File(dir, "mytoken_share_${System.currentTimeMillis()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.update.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            targetPackage?.let { setPackage(it) }
        }
        if (targetPackage != null) {
            runCatching { context.startActivity(intent) }.onFailure {
                context.startActivity(Intent.createChooser(intent, "分享用量"))
            }
        } else {
            context.startActivity(Intent.createChooser(intent, "分享用量"))
        }
    } catch (_: Exception) {
        // 静默失败
    }
}
