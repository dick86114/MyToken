package ai.routin.mytoken.feature.home

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

/**
 * 分享图的唯一绘制实现：Compose 预览和 PNG 导出都通过这里渲染，
 * 避免出现“界面一个样、保存出来另一个样”的偏差。
 */
internal object UsageShareCardRenderer {
    const val CANVAS_WIDTH = 430f

    fun height(card: UsageShareRenderedCard, widthPx: Int): Int {
        if (widthPx <= 0) return 0
        return (RootPainter(Canvas(), card, 1f).measure() * (widthPx / CANVAS_WIDTH)).toInt()
    }

    fun draw(canvas: Canvas, card: UsageShareRenderedCard, widthPx: Int) {
        if (widthPx <= 0) return
        val save = canvas.save()
        val scale = widthPx / CANVAS_WIDTH
        canvas.scale(scale, scale)
        RootPainter(canvas, card, 1f).draw()
        canvas.restoreToCount(save)
    }
}

private data class ShareColors(
    val background: Int,
    val panel: Int,
    val panelBorder: Int,
    val title: Int,
    val secondary: Int,
    val muted: Int,
    val accent: Int,
    val remain: Int,
    val reset: Int,
    val track: Int,
    val bar: Int?,
    val brandBackground: Int,
    val brandTitle: Int,
    val brandText: Int,
    val isLight: Boolean,
) {
    companion object {
        fun ticket() = ShareColors(
            background = 0xFF181A22.toInt(),
            panel = 0xD90F172A.toInt(),
            panelBorder = 0xFF1E293B.toInt(),
            title = Color.WHITE,
            secondary = 0xFFCBD5E1.toInt(),
            muted = 0xFF94A3B8.toInt(),
            accent = 0xFFFBBF24.toInt(),
            remain = 0xFF34D399.toInt(),
            reset = 0xFFFCD34D.toInt(),
            track = 0xFF1E293B.toInt(),
            bar = null,
            brandBackground = 0xFF14171F.toInt(),
            brandTitle = Color.WHITE,
            brandText = 0xFF94A3B8.toInt(),
            isLight = false,
        )

        fun compact(light: Boolean) = ShareColors(
            background = if (light) 0xFFFAFAFA.toInt() else 0xFF0F1117.toInt(),
            panel = if (light) Color.WHITE else 0xFF0B1220.toInt(),
            panelBorder = if (light) 0xFFE2E8F0.toInt() else 0xFF1E293B.toInt(),
            title = if (light) 0xFF0F172A.toInt() else Color.WHITE,
            secondary = if (light) 0xFF64748B.toInt() else 0xFF94A3B8.toInt(),
            muted = if (light) 0xFF8B95A5.toInt() else 0xFF64748B.toInt(),
            accent = if (light) 0xFF2563EB.toInt() else 0xFF60A5FA.toInt(),
            remain = if (light) 0xFF059669.toInt() else 0xFF34D399.toInt(),
            reset = if (light) 0xFFD97706.toInt() else 0xFFFBBF24.toInt(),
            track = if (light) 0xFFF1F5F9.toInt() else 0xFF1E293B.toInt(),
            bar = if (light) 0xFF2563EB.toInt() else 0xFF3B82F6.toInt(),
            brandBackground = if (light) 0xFFF1F3F7.toInt() else 0xFF14171F.toInt(),
            brandTitle = if (light) 0xFF1B1F2B.toInt() else Color.WHITE,
            brandText = if (light) 0xFF64748B.toInt() else 0xFF94A3B8.toInt(),
            isLight = light,
        )
    }
}

private class RootPainter(
    private val canvas: Canvas,
    private val card: UsageShareRenderedCard,
    private val d: Float,
) {
    private val colors = when (card.template) {
        UsageShareTemplate.Ticket -> ShareColors.ticket()
        UsageShareTemplate.Dark -> ShareColors.compact(false)
        UsageShareTemplate.Light -> ShareColors.compact(true)
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
    private val width = UsageShareCardRenderer.CANVAS_WIDTH
    private val ticket = card.template == UsageShareTemplate.Ticket
    private val marginX = if (ticket) 12f else 0f
    private val marginY = if (ticket) 10f else 0f
    private val cardWidth = width - marginX * 2f
    private val contentWidth = cardWidth - 32f
    private val left = marginX + 16f

    fun measure(): Float {
        if (ticket) {
            val bodyHeight = ticketBody(0f, true)
            return bodyHeight + marginY * 2f
        }
        return compactBody(0f, true)
    }

    fun draw() {
        val height = measure()
        paint.color = if (ticket) 0xFF14171F.toInt() else colors.background
        canvas.drawRect(0f, 0f, width, height, paint)

        if (ticket) {
            val rect = RectF(marginX, marginY, marginX + cardWidth, height - marginY)
            val path = Path().apply { addRoundRect(rect, 16f * d, 16f * d, Path.Direction.CW) }
            val save = canvas.save()
            canvas.clipPath(path)
            ticketBody(height - marginY * 2f, false)
            canvas.restoreToCount(save)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.2f * d
            paint.color = 0x4DF59E0B.toInt()
            canvas.drawRoundRect(rect, 16f * d, 16f * d, paint)
            paint.style = Paint.Style.FILL
            val centerY = height / 2f
            drawNotch(0f, centerY, 0xFF14171F.toInt())
            drawNotch(width, centerY, 0xFF14171F.toInt())
        } else {
            val rect = RectF(0f, 0f, width, height)
            val path = Path().apply { addRoundRect(rect, 16f * d, 16f * d, Path.Direction.CW) }
            val save = canvas.save()
            canvas.clipPath(path)
            compactBody(height, false)
            canvas.restoreToCount(save)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f * d
            paint.color = colors.panelBorder
            canvas.drawRoundRect(rect, 16f * d, 16f * d, paint)
            paint.style = Paint.Style.FILL
        }
    }

    private fun ticketBody(bodyHeight: Float, measureOnly: Boolean): Float {
        if (!measureOnly) {
            paint.color = colors.background
            canvas.drawRect(marginX, marginY, marginX + cardWidth, marginY + bodyHeight, paint)
            val headerHeight = header(false, marginY, marginY, marginY)
            drawTear(marginY + headerHeight)
            val y = marginY + headerHeight + 24f
            return metricsSection(y, false)
                .let { watermark(it) }
                .let { brandFooter(it) + marginY }
        }
        val headerHeight = header(true, 0f, 0f, 0f)
        val y = headerHeight + 24f
        return metricsSection(y, true)
            .let { watermark(it) }
            .let { brandFooter(it) }
    }

    private fun compactBody(bodyHeight: Float, measureOnly: Boolean): Float {
        if (!measureOnly) {
            paint.color = colors.background
            canvas.drawRect(0f, 0f, width, bodyHeight, paint)
        }
        var y = header(false, 0f, 16f, 0f)
        y = metricsSection(y + 12f, measureOnly)
        y = watermark(y)
        return brandFooter(y)
    }

    private fun header(measureOnly: Boolean, globalTop: Float, contentTop: Float, offset: Float): Float {
        // measureOnly 使用局部坐标；真实绘制通过 offset 转换到整张画布。
        fun y(local: Float) = local + offset
        val headerHeight = measureHeader()
        if (!measureOnly) {
            val rect = RectF(marginX, y(0f), marginX + cardWidth, y(headerHeight))
            if (ticket) {
                paint.shader = LinearGradient(
                    rect.left,
                    rect.top,
                    rect.right,
                    rect.bottom,
                    0xFF222733.toInt(),
                    0xFF171922.toInt(),
                    android.graphics.Shader.TileMode.CLAMP,
                )
                canvas.drawRoundRect(rect, 16f * d, 16f * d, paint)
                canvas.drawRect(rect.left, rect.top + rect.height() / 2f, rect.right, rect.bottom, paint)
                paint.shader = null
            } else {
                paint.color = colors.background
                canvas.drawRoundRect(rect, 16f * d, 16f * d, paint)
                canvas.drawRect(rect.left, rect.top + rect.height() / 2f, rect.right, rect.bottom, paint)
            }
        }

        var localY = 16f
        drawAvatar(left, y(localY), 36f)
        val supplierWidth = 72f
        val nameWidth = contentWidth - 46f - supplierWidth
        val badgeLocal = localY + 4f
        var badgeX = left + 46f
        if (measureOnly || true) {
            badgeX += drawPassBadge(badgeX, y(badgeLocal), measureOnly)
            if (card.showsStatus) {
                drawStatusBadge(badgeX + 6f, y(badgeLocal), measureOnly)
            }
        }
        val nameLayout = makeLayout(card.displayName, 16f, colors.title, bold = true, width = nameWidth, maxLines = 2)
        drawLayout(nameLayout, left + 46f, y(badgeLocal + 20f), measureOnly)

        drawText("SUPPLIER", left + contentWidth, y(localY + 9f), 9f, colors.muted, align = Paint.Align.RIGHT, mono = true)
        drawText(
            card.providerName,
            left + contentWidth,
            y(localY + 25f),
            12f,
            if (ticket) 0xFFFDE68A.toInt() else colors.title,
            bold = true,
            align = Paint.Align.RIGHT,
        )

        localY = maxOf(66f, 22f + textHeight(nameLayout)) + 18f
        var planHeight = 0f
        if (card.subtitle.isNotEmpty() || card.cycleRemainingText != null) {
            drawHairline(y(localY - 10f), if (ticket) 0x14FFFFFF.toInt() else if (colors.isLight) 0xFFE2E8F0.toInt() else 0x14FFFFFF.toInt())
            val half = contentWidth / 2f - 6f
            if (card.subtitle.isNotEmpty()) {
                drawText("套餐规格", left, y(localY + 8f), 10f, colors.muted)
                val layout = makeLayout(card.subtitle, 12f, colors.title, bold = true, width = half, maxLines = 2)
                drawLayout(layout, left, y(localY + 21f), measureOnly)
                planHeight = maxOf(planHeight, 21f + textHeight(layout))
            }
            card.cycleRemainingText?.let { cycle ->
                val x = left + contentWidth / 2f + 6f
                drawText("订阅周期 / 剩余", x, y(localY + 8f), 10f, colors.muted)
                val layout = makeLayout(cycle, 11f, if (ticket || !colors.isLight) 0xFFE2E8F0.toInt() else colors.title, mono = true, width = half, maxLines = 2)
                drawLayout(layout, x, y(localY + 21f), measureOnly)
                planHeight = maxOf(planHeight, 21f + textHeight(layout))
            }
            localY += planHeight + 14f
        }

        card.note?.let { note ->
            localY += 10f
            val layout = makeLayout(note, 11f, if (ticket) 0xFFFDE68A.toInt() else colors.title, width = contentWidth - 38f, maxLines = 4)
            val boxHeight = textHeight(layout) + 16f
            if (!measureOnly) {
                paint.color = if (ticket || !colors.isLight) 0x1AF59E0B.toInt() else 0x14D97706.toInt()
                canvas.drawRoundRect(
                    RectF(left, y(localY), left + contentWidth, y(localY + boxHeight)),
                    6f * d,
                    6f * d,
                    paint,
                )
                if (ticket || !colors.isLight) {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = d
                    paint.color = 0x33F59E0B.toInt()
                    canvas.drawRoundRect(
                        RectF(left, y(localY), left + contentWidth, y(localY + boxHeight)),
                        6f * d,
                        6f * d,
                        paint,
                    )
                    paint.style = Paint.Style.FILL
                }
            }
            drawInfoIcon(left + 10f, y(localY + boxHeight / 2f), colors.accent)
            drawLayout(layout, left + 28f, y(localY + 8f), measureOnly)
            localY += boxHeight + 6f
        }
        return headerHeight + if (card.note != null) 0f else 0f
    }

    private fun measureHeader(): Float {
        val nameLayout = makeLayout(card.displayName, 16f, colors.title, bold = true, width = contentWidth - 118f, maxLines = 2)
        var height = maxOf(66f, 22f + textHeight(nameLayout)) + 18f
        if (card.subtitle.isNotEmpty() || card.cycleRemainingText != null) {
            val half = contentWidth / 2f - 6f
            var section = 21f
            if (card.subtitle.isNotEmpty()) section = maxOf(section, 21f + layoutHeight(card.subtitle, 12f, half, 2))
            card.cycleRemainingText?.let { section = maxOf(section, 21f + layoutHeight(it, 11f, half, 2)) }
            height += section + 14f
        }
        card.note?.let { note ->
            height += 16f + layoutHeight(note, 11f, contentWidth - 38f, 4) + 22f
        }
        return height
    }

    private fun metricsSection(startY: Float, measureOnly: Boolean): Float {
        val progress = card.metrics.filter { it.percent != null }
        val tiles = allTiles()
        var y = startY + 16f
        progress.forEach { item ->
            val height = gaugeHeight(item)
            drawGauge(item, y, measureOnly)
            y += height + 12f
        }
        tiles.chunked(2).forEach { row ->
            val tileWidth = contentWidth / 2f - 4f
            val height = row.maxOf { tileHeight(it, tileWidth) }
            row.forEachIndexed { index, item ->
                drawTile(item, left + index * (tileWidth + 8f), y, tileWidth, height, measureOnly)
            }
            y += height + 8f
        }
        return y + 4f
    }

    private fun drawGauge(item: UsageShareMetricItem, y: Float, measureOnly: Boolean) {
        val height = gaugeHeight(item)
        paint.color = colors.panel
        canvas.drawRoundRect(RectF(left, y, left + contentWidth, y + height), 12f * d, 12f * d, paint)
        if (ticket) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = d
            paint.color = colors.panelBorder
            canvas.drawRoundRect(RectF(left, y, left + contentWidth, y + height), 12f * d, 12f * d, paint)
            paint.style = Paint.Style.FILL
        }
        val gx = left + 12f
        val gw = contentWidth - 24f
        var gy = y + 18f
        val titleLayout = makeLayout(item.title, 11f, colors.secondary, width = gw - 46f, maxLines = 2)
        drawLayout(titleLayout, gx, gy, measureOnly)
        drawText(item.headline, gx + gw, gy + 10f, 12f, colors.accent, bold = true, align = Paint.Align.RIGHT, mono = true)
        gy += maxOf(textHeight(titleLayout), 12f) + 8f
        item.percent?.let { percent ->
            drawBar(gx, gy, gw, 8f, percent, gradient = ticket)
            gy += 14f
        }
        if (item.usedText != null || item.limitText != null) {
            drawText("已用：${item.usedText ?: "—"}", gx, gy + 8f, 10f, colors.muted, mono = true)
            drawText("上限：${item.limitText ?: "—"}", gx + gw, gy + 8f, 10f, colors.muted, align = Paint.Align.RIGHT, mono = true)
            gy += 18f
        }
        if (item.remainingAmountText != null || item.resetBadgeText != null) {
            if (item.remainingAmountText != null) {
                drawText("剩余：${item.remainingAmountText}", gx, gy + 8f, 10f, colors.remain, mono = true)
            }
            item.resetBadgeText?.let { badge ->
                textPaint.textSize = 10f * d
                textPaint.typeface = Typeface.MONOSPACE
                val width = textPaint.measureText(badge) + 12f * d
                if (!measureOnly) {
                    paint.color = 0x1FF59E0B.toInt()
                    canvas.drawRoundRect(RectF(gx + gw - width, gy - 2f, gx + gw, gy + 14f), 4f * d, 4f * d, paint)
                }
                drawText(badge, gx + gw, gy + 8f, 10f, colors.reset, align = Paint.Align.RIGHT, mono = true)
            }
            gy += 18f
        }
        item.companionText?.let {
            drawText(it, gx, gy + 8f, 10f, colors.muted, mono = true)
        }
    }

    private fun gaugeHeight(item: UsageShareMetricItem): Float {
        var height = 24f
        if (item.percent != null) height += 14f
        if (item.usedText != null || item.limitText != null) height += 18f
        if (item.remainingAmountText != null || item.resetBadgeText != null) height += 18f
        if (item.companionText != null) height += 14f
        return height + 12f
    }

    private fun drawTile(item: UsageShareMetricItem, x: Float, y: Float, tileWidth: Float, height: Float, measureOnly: Boolean) {
        paint.color = if (ticket) 0x990F172A.toInt() else colors.panel
        canvas.drawRoundRect(RectF(x, y, x + tileWidth, y + height), 10f * d, 10f * d, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = d
        paint.color = if (ticket) 0x0FFFFFFF.toInt() else colors.panelBorder
        canvas.drawRoundRect(RectF(x, y, x + tileWidth, y + height), 10f * d, 10f * d, paint)
        paint.style = Paint.Style.FILL
        val titleLayout = makeLayout(item.title, 9f, colors.muted, width = tileWidth - 20f, maxLines = 2)
        drawLayout(titleLayout, x + 10f, y + 10f, measureOnly)
        var localY = 10f + maxOf(textHeight(titleLayout), 11f) + 5f
        val headlineColor = if (item.id.contains("group") || item.title.contains("分组")) 0xFFC084FC.toInt() else colors.title
        val headline = makeLayout(item.headline, 12f, headlineColor, bold = true, mono = true, width = tileWidth - 20f, maxLines = 2)
        drawLayout(headline, x + 10f, y + localY, measureOnly)
        localY += textHeight(headline) + 5f
        val companion = item.companionText ?: item.amountDetails.firstOrNull()
        companion?.let {
            val layout = makeLayout(it, 9f, colors.muted, mono = true, width = tileWidth - 20f, maxLines = 2)
            drawLayout(layout, x + 10f, y + localY, measureOnly)
        }
    }

    private fun tileHeight(item: UsageShareMetricItem, tileWidth: Float): Float {
        var height = 15f + 5f
        height += layoutHeight(item.headline, 12f, tileWidth - 20f, 2) + 5f
        val companion = item.companionText ?: item.amountDetails.firstOrNull()
        if (companion != null) height += layoutHeight(companion, 9f, tileWidth - 20f, 2)
        return height + 20f
    }

    private fun allTiles(): List<UsageShareMetricItem> {
        val tiles = card.metrics.filter { it.percent == null }.toMutableList()
        return tiles
    }

    private fun watermark(y: Float): Float {
        if (!card.showsWatermark) return y
        drawText("✓ MyToken 本地快照 · 无凭据", left, y + 8f, 9f, colors.muted, mono = true)
        drawText(
            card.capturedAtText.replace(".", "-"),
            left + contentWidth,
            y + 8f,
            9f,
            colors.muted,
            align = Paint.Align.RIGHT,
            mono = true,
        )
        return y + 20f
    }

    private fun brandFooter(y: Float): Float {
        if (!card.showsWatermark) return y
        val top = y + 4f
        paint.shader = LinearGradient(
            left,
            top,
            left + contentWidth,
            top,
            0x1F000000.toInt(),
            0x0A000000.toInt(),
            android.graphics.Shader.TileMode.CLAMP,
        )
        canvas.drawRect(left, top, left + contentWidth, top + d, paint)
        paint.shader = null
        paint.color = colors.brandBackground
        canvas.drawRect(left, top + d, left + contentWidth, top + 62f, paint)
        drawBrandLogo(left + 16f, top + 20f, 22f)
        drawText("MyToken", left + 48f, top + 27f, 11f, colors.brandTitle, bold = true)
        drawText("AI 用量，一目了然", left + 48f, top + 40f, 9f, colors.brandText)
        drawText("https://mytoken.idickies.cc", left + 48f, top + 51f, 9f, colors.brandText, mono = true)
        val qr = UsageShareBitmapFactory.qrCode(128)
        canvas.drawBitmap(qr, null, RectF(left + contentWidth - 60f, top + 10f, left + contentWidth - 16f, top + 54f), paint)
        qr.recycle()
        return top + 62f
    }

    private fun drawTear(y: Float) {
        paint.color = 0xFF14161F.toInt()
        canvas.drawRect(marginX, y, marginX + cardWidth, y + 12f, paint)
        paint.color = 0xFF475569.toInt()
        paint.strokeWidth = 1.5f * d
        val padding = 20f
        var x = marginX + padding
        val end = marginX + cardWidth - padding
        while (x < end) {
            canvas.drawLine(x, y + 6f, minOf(end, x + 4f * d), y + 6f, paint)
            x += 8f * d
        }
        paint.strokeWidth = 0f
    }

    private fun drawNotch(centerX: Float, centerY: Float, fill: Int) {
        val radius = 12f
        paint.color = fill
        canvas.drawCircle(centerX, centerY, radius * d, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = d
        paint.color = 0x4DF59E0B.toInt()
        canvas.drawCircle(centerX, centerY, radius * d, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawAvatar(x: Float, y: Float, size: Float) {
        val rect = RectF(x, y, x + size, y + size)
        paint.color = if (ticket) 0x26F59E0B.toInt() else if (colors.isLight) 0xFF0F172A.toInt() else 0xFF2563EB.toInt()
        canvas.drawRoundRect(rect, 10f * d, 10f * d, paint)
        if (ticket) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = d
            paint.color = 0x4DF59E0B.toInt()
            canvas.drawRoundRect(rect, 10f * d, 10f * d, paint)
            paint.style = Paint.Style.FILL
        }
        drawText(card.avatarLetter, x + size / 2f, y + size / 2f + 7f, 15f, if (ticket) 0xFFFBBF24.toInt() else Color.WHITE, bold = true, align = Paint.Align.CENTER)
    }

    private fun drawPassBadge(x: Float, y: Float, measureOnly: Boolean): Float {
        textPaint.textSize = 10f * d
        textPaint.typeface = Typeface.MONOSPACE
        val width = textPaint.measureText(card.passCode) + 12f
        if (!measureOnly) {
            paint.color = if (ticket) {
                0x33F59E0B.toInt()
            } else if (colors.isLight) {
                0xFFE2E8F0.toInt()
            } else {
                0x14FFFFFF.toInt()
            }
            canvas.drawRoundRect(RectF(x, y, x + width, y + 16f), 4f * d, 4f * d, paint)
        }
        drawText(
            card.passCode,
            x + 6f,
            y + 11f,
            10f,
            if (ticket) 0xFFFCD34D.toInt() else if (colors.isLight) 0xFF334155.toInt() else 0xFFCBD5E1.toInt(),
            mono = true,
        )
        return width
    }

    private fun drawStatusBadge(x: Float, y: Float, measureOnly: Boolean) {
        val color = when {
            card.isAvailable -> colors.remain
            ticket -> 0xFFF87171.toInt()
            else -> 0xFFDC2626.toInt()
        }
        if (!measureOnly) {
            paint.color = color
            canvas.drawCircle(x + 3f, y + 8f, 3f, paint)
        }
        drawText(if (card.isAvailable) "可用" else "不可用", x + 9f, y + 12f, 10f, color)
    }

    private fun drawBar(x: Float, y: Float, width: Float, height: Float, percent: Double, gradient: Boolean) {
        paint.color = colors.track
        canvas.drawRoundRect(RectF(x, y, x + width, y + height), height / 2f, height / 2f, paint)
        val clamped = (percent.coerceIn(0.0, 100.0) / 100.0).toFloat()
        val fillWidth = maxOf(if (percent == 0.0) 0f else 0.02f * width, width * clamped)
        if (fillWidth > 0f) {
            if (gradient) {
                paint.shader = LinearGradient(x, y, x + width, y, 0xFF3B82F6.toInt(), 0xFFFBBF24.toInt(), android.graphics.Shader.TileMode.CLAMP)
            } else {
                paint.color = colors.bar ?: 0xFF3B82F6.toInt()
            }
            canvas.drawRoundRect(RectF(x, y, x + fillWidth, y + height), height / 2f, height / 2f, paint)
            paint.shader = null
        }
    }

    private fun drawBrandLogo(cx: Float, cy: Float, size: Float) {
        paint.color = 0xFFF59E0B.toInt()
        canvas.drawRoundRect(RectF(cx, cy, cx + size, cy + size), 6f * d, 6f * d, paint)
        drawText("M", cx + size / 2f, cy + size / 2f + 6f, 13f, Color.BLACK, bold = true, align = Paint.Align.CENTER)
    }

    private fun drawHairline(y: Float, color: Int) {
        paint.color = color
        canvas.drawRect(left, y, left + contentWidth, y + d, paint)
    }

    private fun drawInfoIcon(x: Float, centerY: Float, color: Int) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f * d
        paint.color = color
        canvas.drawCircle(x + 5f, centerY, 5f, paint)
        canvas.drawLine(x + 5f, centerY - 2f, x + 5f, centerY + 0.5f, paint)
        canvas.drawPoint(x + 5f, centerY + 2.5f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawText(
        text: String,
        x: Float,
        baseline: Float,
        size: Float,
        color: Int,
        bold: Boolean = false,
        mono: Boolean = false,
        align: Paint.Align = Paint.Align.LEFT,
    ) {
        textPaint.color = color
        textPaint.textSize = size * d
        textPaint.textAlign = align
        textPaint.isFakeBoldText = bold
        textPaint.typeface = when {
            mono -> Typeface.MONOSPACE
            bold -> Typeface.DEFAULT_BOLD
            else -> Typeface.DEFAULT
        }
        canvas.drawText(text, x, baseline, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.isFakeBoldText = false
    }

    private fun makeLayout(
        text: String,
        size: Float,
        color: Int,
        bold: Boolean = false,
        mono: Boolean = false,
        width: Float,
        maxLines: Int,
    ): StaticLayout {
        textPaint.color = color
        textPaint.textSize = size * d
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.isFakeBoldText = bold
        textPaint.typeface = when {
            mono -> Typeface.MONOSPACE
            bold -> Typeface.DEFAULT_BOLD
            else -> Typeface.DEFAULT
        }
        val widthPx = (width * d).toInt().coerceAtLeast(1)
        return StaticLayout.Builder.obtain(text, 0, text.length, textPaint, widthPx)
            .setMaxLines(maxLines)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .setLineSpacing(0f, 1.08f)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
    }

    private fun drawLayout(layout: StaticLayout, x: Float, y: Float, measureOnly: Boolean) {
        if (measureOnly) return
        val save = canvas.save()
        canvas.translate(x * d, y * d)
        layout.draw(canvas)
        canvas.restoreToCount(save)
    }

    private fun textHeight(layout: StaticLayout): Float = layout.height / d

    private fun layoutHeight(text: String, size: Float, width: Float, maxLines: Int): Float {
        val color = colors.title
        return textHeight(makeLayout(text, size, color, width = width, maxLines = maxLines))
    }
}
