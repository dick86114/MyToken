package ai.routin.mytoken.feature.home

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageSnapshot
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import java.math.BigDecimal
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val BrandDark = Color(0xFF14171F)
private val BrandCardBg = Color(0xFF181A22)
private val BrandAmber = Color(0xFFF59E0B)
private val BrandAmberLight = Color(0xFFFCD34D)
private val BrandMuted = Color(0xFF94A3B8)
private val BrandTear = Color(0xFF14161F)

/** 票根风格分享图，窄屏自适应宽度。 */
@Composable
internal fun UsageShareCard(
    displayName: String,
    providerName: String,
    planName: String,
    snapshot: UsageSnapshot?,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 360.dp,
) {
    val width = maxWidth
    Box(modifier = modifier.widthIn(max = width)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BrandCardBg),
        ) {
            shareHeader(displayName, providerName, planName, snapshot)
            shareTearLine()
            shareMetrics(snapshot)
            brandFooterRow()
        }
    }
}

@Composable
private fun shareHeader(name: String, provider: String, plan: String, snapshot: UsageSnapshot?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(Color(0xFF222733), Color(0xFF171922))))
            .padding(16.dp),
    ) {
        Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        val sub = listOfNotNull(provider, plan.takeIf { it.isNotBlank() }).joinToString(" · ")
        if (sub.isNotBlank()) {
            Text(sub, style = MaterialTheme.typography.bodySmall, color = BrandMuted)
        }
        val cycle = snapshot?.subscriptionEndAt?.let {
            val fmt = DateTimeFormatter.ofPattern("MM.dd").withZone(ZoneId.systemDefault())
            val days = java.time.Duration.between(java.time.Instant.now(), it).toDays().coerceAtLeast(1)
            "余 $days 天 (${fmt.format(it)})"
        }
        if (cycle != null) {
            Spacer(Modifier.height(8.dp))
            Text("订阅周期 / 剩余", style = MaterialTheme.typography.labelSmall, color = BrandMuted)
            Text(cycle, style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
    }
}

@Composable
private fun shareTearLine() {
    Canvas(Modifier.fillMaxWidth().height(14.dp).background(BrandTear)) {
        val y = size.height / 2
        val dash = 8f
        var x = 40f
        while (x < size.width - 40f) {
            drawLine(Color(0xFF475569), Offset(x, y), Offset(x + dash * 0.6f, y), strokeWidth = 2f)
            x += dash
        }
        drawCircle(BrandTear, radius = 12.dp.toPx() / 2, center = Offset(0f, y))
        drawCircle(BrandTear, radius = 12.dp.toPx() / 2, center = Offset(size.width, y))
    }
}

@Composable
private fun shareMetrics(snapshot: UsageSnapshot?) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val metrics = snapshot?.metrics.orEmpty().filter { it.presentation == UsageMetricPresentation.Progress }
        metrics.forEach { item -> shareGauge(item) }
        val tiles = snapshot?.metrics.orEmpty().filter { it.presentation != UsageMetricPresentation.Progress }
        if (tiles.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tiles.take(2).forEach { item -> shareTile(item, Modifier.weight(1f)) }
            }
        }
        Text(
            "✓ MyToken 本地快照 · 无凭据",
            style = MaterialTheme.typography.labelSmall,
            color = BrandMuted,
        )
    }
}

@Composable
private fun shareGauge(metric: UsageMetric) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF0F172A)).padding(12.dp)) {
        val pct = metric.displayedPercent()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(metric.label, style = MaterialTheme.typography.labelMedium, color = Color(0xFFCBD5E1))
            Text(pct ?: "—", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = BrandAmberLight)
        }
        if (pct != null) {
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF1E293B))) {
                Box(
                    Modifier
                        .fillMaxWidth(pct.toFloat() / 100f)
                        .fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(Color(0xFF3B82F6), BrandAmber))),
                )
            }
        }
        val used = metric.formattedAmount(metric.used)
        val limit = metric.formattedAmount(metric.limit)
        val remaining = metric.formattedAmount(metric.remaining)
        if (used != null || limit != null) {
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("已用 $used", style = MaterialTheme.typography.labelSmall, color = BrandMuted)
                Text("上限 $limit", style = MaterialTheme.typography.labelSmall, color = BrandMuted)
            }
        }
        if (remaining != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("剩余 $remaining", style = MaterialTheme.typography.labelSmall, color = Color(0xFF34D399))
            }
        }
    }
}

@Composable
private fun shareTile(metric: UsageMetric, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF0F172A)).padding(10.dp)) {
        Text(metric.label, style = MaterialTheme.typography.labelSmall, color = BrandMuted)
        Text(metric.displayedPercent() ?: "—", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun brandFooterRow() {
    Row(
        Modifier.fillMaxWidth().background(BrandDark).padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("MyToken", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text("AI 用量，一目了然", style = MaterialTheme.typography.labelSmall, color = BrandMuted)
            Text("https://mytoken.idickies.cc", style = MaterialTheme.typography.labelSmall, color = BrandMuted)
        }
    }
}

private fun UsageMetric.displayedPercent(): String? {
    val lim = limit ?: return null
    if (lim.signum() <= 0) return null
    val num = when (semantic) {
        ai.routin.mytoken.domain.model.UsageMetricSemantic.UsedQuota -> used
        ai.routin.mytoken.domain.model.UsageMetricSemantic.RemainingQuota -> remaining
        else -> return null
    } ?: return null
    val pct = num.divide(lim, 2, java.math.RoundingMode.HALF_UP).multiply(BigDecimal(100))
    return "${pct.stripTrailingZeros().toPlainString()}%"
}

private fun UsageMetric.formattedAmount(value: BigDecimal?): String? {
    value ?: return null
    val s = value.stripTrailingZeros().toPlainString()
    return if (unit == ai.routin.mytoken.domain.model.UsageMetricUnit.Currency) {
        "${currencyCode ?: "$"}$s"
    } else {
        s
    }
}

/** 将分享卡片渲染为 Bitmap 用于分享。 */
internal fun renderShareBitmap(
    widthPx: Int,
    heightPx: Int,
    density: Float,
    content: (Canvas) -> Unit,
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.argb(0, 0, 0, 0))
    content(canvas)
    return bitmap
}
