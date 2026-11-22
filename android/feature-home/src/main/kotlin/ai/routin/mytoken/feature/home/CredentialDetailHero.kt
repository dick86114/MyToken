package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenRadius
import ai.routin.mytoken.core.ui.MyTokenReportBodyShape
import ai.routin.mytoken.core.ui.MyTokenRow
import ai.routin.mytoken.core.ui.MyTokenSection
import ai.routin.mytoken.core.ui.MyTokenStatus
import ai.routin.mytoken.core.ui.MyTokenStatusTone
import ai.routin.mytoken.core.ui.MyTokenSurface
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.core.ui.MyTokenVisualPolicy
import ai.routin.mytoken.core.ui.ProviderLogoPlate
import ai.routin.mytoken.domain.model.CredentialMetadataKey
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricUnit
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

private enum class WindowKind { Daily, FiveHour, Weekly, Monthly, Unknown }

private fun windowKind(metric: UsageMetric): WindowKind {
    val id = metric.id.lowercase()
    val label = metric.label
    return when {
        id.contains("today") || id.contains("day") || label.contains("日") -> WindowKind.Daily
        id.contains("five") || id.contains("5hour") || id.contains("5-hour") ||
            (label.contains("5") && label.contains("小时")) -> WindowKind.FiveHour
        id.contains("week") || label.contains("周") -> WindowKind.Weekly
        id.contains("month") || id.contains("credit-progress") || label.contains("月") ->
            WindowKind.Monthly
        else -> WindowKind.Unknown
    }
}

private fun windowDurationSeconds(metric: UsageMetric): Long {
    val start = metric.windowStart
    val end = metric.windowEnd
    if (start != null && end != null) {
        return Duration.between(start, end).seconds.coerceAtLeast(0)
    }
    return when (windowKind(metric)) {
        WindowKind.Daily -> 86_400
        WindowKind.FiveHour -> 5 * 3_600
        WindowKind.Weekly -> 7 * 86_400
        WindowKind.Monthly -> 30 * 86_400
        WindowKind.Unknown -> Long.MAX_VALUE / 2
    }
}

/** 详情页 Hero 默认展示窗口时长最短的进度指标；余额卡返回 null。 */
internal fun defaultHeroProgressMetric(metrics: List<UsageMetric>): UsageMetric? =
    metrics
        .filter { it.presentation == UsageMetricPresentation.Progress }
        .minByOrNull(::windowDurationSeconds)

/** 全部窗口顺序：日 → 5 小时 → 周 → 其他 → 月，月永远在最下面。 */
internal fun windowDisplayRank(metric: UsageMetric): Int = when (windowKind(metric)) {
    WindowKind.Daily -> 0
    WindowKind.FiveHour -> 1
    WindowKind.Weekly -> 2
    WindowKind.Unknown -> 3
    WindowKind.Monthly -> 4
}

/** 长 URL 插入零宽断点，窄卡片里可以完整换行而不是被截断。 */
private fun wrapUrlForDisplay(url: String): String =
    url.replace("/", "/\u200B")
        .replace("?", "?\u200B")
        .replace("&", "&\u200B")

/** 详情页 Hero：身份 + 主指标（配额环或余额）+ 更新时间页脚。 */
@Composable
internal fun DetailHero(card: CredentialCardUi, nextRefreshAt: Instant? = null) {
    val colors = MyTokenThemeTokens.colors
    val health = usageCardHealth(card)
    val metrics = card.snapshot?.metrics.orEmpty()
    val primary = defaultHeroProgressMetric(metrics)
    val balance = metrics.firstOrNull { it.presentation == UsageMetricPresentation.Balance }

    MyTokenSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("detail_hero"),
        surfaceRole = MyTokenVisualPolicy.SurfaceRole.Card,
        shape = RoundedCornerShape(MyTokenRadius.card),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProviderLogoPlate(
                    painter = painterResource(ProviderCatalog.logoRes(card.credential.providerId)),
                    contentDescription = null,
                    size = 48.dp,
                    shape = RoundedCornerShape(13.dp),
                    contentPadding = 5.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.credential.name,
                        style = MyTokenType.detailTitle,
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${ProviderCatalog.displayName(card.credential.providerId)} · ${planName(card)}",
                        modifier = Modifier.padding(top = 2.dp),
                        style = MyTokenType.caption,
                        color = colors.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                MyTokenStatus(
                    text = usageCardStatusText(health) ?: "正常",
                    tone = usageCardStatusTone(health),
                )
            }

            when {
                primary != null -> QuotaHero(metric = primary, health = health)
                balance != null -> BalanceHero(card = card, balance = balance)
                else -> Text(
                    text = "暂无用量数据",
                    style = MyTokenType.caption,
                    color = colors.muted,
                )
            }

            HeroFooter(card = card, nextRefreshAt = nextRefreshAt)
        }
    }
}

@Composable
private fun QuotaHero(metric: UsageMetric, health: UsageCardHealth) {
    val colors = MyTokenThemeTokens.colors
    val percent = progressPercent(metric)
    Row(verticalAlignment = Alignment.CenterVertically) {
        UsageArcGauge(
            percent = percent,
            tone = usageCardRingTone(health),
            diameter = 88.dp,
            inactive = usageCardRingInactive(health),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = windowLabel(metric),
                style = MyTokenType.label,
                color = colors.muted,
                maxLines = 1,
                modifier = Modifier.testTag("detail_hero_metric_label"),
            )
            Text(
                text = "剩余 ${formatRemaining(metric)}",
                style = MyTokenType.detailRemaining,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = metric.windowEnd?.let { "${formatRemainingDuration(it, Instant.now())} 后重置" }
                    ?: quotaEndText(null, "重置") ?: "—",
                style = MyTokenType.caption,
                color = colors.muted,
                maxLines = 1,
            )
            if (metric.resetCards.isNotEmpty()) {
                Box(modifier = Modifier.padding(top = 7.dp)) {
                    ResetCardsBadge(metric = metric, style = ResetCardsBadgeStyle.Detail)
                }
            }
        }
    }
}

@Composable
private fun BalanceHero(card: CredentialCardUi, balance: UsageMetric) {
    val colors = MyTokenThemeTokens.colors
    val toppedUp = findBalanceMetric(card, balance, "toppedUpBalance")
    val granted = findBalanceMetric(card, balance, "grantedBalance")
    val primaryValue = toppedUp?.value ?: BigDecimal.ZERO
    val secondaryValue = granted?.value ?: BigDecimal.ZERO
    val total = primaryValue + secondaryValue
    val primaryWeight = if (total.signum() > 0) primaryValue.toDouble() / total.toDouble() else 0.0

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "账户余额 · ${balance.currencyCode?.uppercase().orEmpty()}".trimEnd(' ', '·'),
            style = MyTokenType.caption,
            color = colors.muted,
        )
        Text(
            text = formatCurrency(balance.value, balance.currencyCode),
            style = MyTokenType.display,
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        UsageCompositionMeter(
            primaryWeight = primaryWeight.takeIf { total.signum() > 0 },
            height = 9.dp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BalanceLegend("充值", toppedUp?.value, balance.currencyCode, colors.ink)
            BalanceLegend("赠金", granted?.value, balance.currencyCode, colors.actionFill)
        }
    }
}

@Composable
private fun BalanceLegend(
    label: String,
    value: BigDecimal?,
    currencyCode: String?,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(color, CircleShape),
        )
        Text(
            text = "$label ${formatCurrency(value, currencyCode)}",
            style = MyTokenType.caption,
            color = MyTokenThemeTokens.colors.muted,
            maxLines = 1,
        )
    }
}

@Composable
private fun HeroFooter(card: CredentialCardUi, nextRefreshAt: Instant?) {
    val colors = MyTokenThemeTokens.colors
    val health = usageCardHealth(card)
    val fetchedAt = card.snapshot?.fetchedAt
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (health == UsageCardHealth.Failed) {
            Text(
                text = "更新失败 · 上次成功 ${fetchedAt?.let(::formatResetTime) ?: "—"}",
                style = MyTokenType.caption,
                color = colors.criticalText,
                maxLines = 1,
            )
        }
        val updateText = fetchedAt?.let { "${formatResetTime(it)} 更新" } ?: "尚未成功更新"
        Text(
            text = if (nextRefreshAt != null) {
                "$updateText · 下次刷新 ${formatResetTime(nextRefreshAt)}"
            } else {
                updateText
            },
            style = MyTokenType.caption,
            color = colors.muted,
            maxLines = 1,
        )
    }
}

/** 全部窗口：每条进度指标一行；余额卡改为按币种分组展示。 */
@Composable
internal fun DetailWindowsSection(card: CredentialCardUi) {
    val metrics = card.snapshot?.metrics.orEmpty()
    MyTokenSection(
        title = "全部窗口",
        modifier = Modifier.testTag("detail_windows"),
    ) {
        val progress = metrics.filter { it.presentation == UsageMetricPresentation.Progress }
        if (progress.isEmpty()) {
            val balances = metrics.filter { it.presentation == UsageMetricPresentation.Balance }
            if (balances.isEmpty()) {
                Text(
                    text = "暂无窗口数据",
                    style = MyTokenType.caption,
                    color = MyTokenThemeTokens.colors.muted,
                )
            } else {
                val primaries = balances.filterNot { isBalanceComponentId(it.id) }
                    .ifEmpty { balances }
                primaries.forEach { balance ->
                    CurrencyBalanceRow(card = card, balance = balance)
                }
            }
        } else {
            progress.sortedBy(::windowDisplayRank).forEach { metric ->
                DetailWindowRow(metric)
            }
        }
    }
}

/** 详情页兜底：把接口返回、但未进入窗口/余额/Facts 的指标全部列出来。 */
@Composable
internal fun DetailMetricsSection(card: CredentialCardUi) {
    // 这些指标已经在「套餐与订阅」里以专用文案展示，避免详情页出现重复行。
    val factsHandledIds = buildSet {
        add("purchased-remaining")
        add("free-remaining")
        if (card.credential.providerId == ProviderId.CommandCode) add("request-count")
    }
    val metrics = card.snapshot?.metrics.orEmpty().filter {
        (it.presentation == UsageMetricPresentation.Value ||
            it.presentation == UsageMetricPresentation.Status) &&
            it.id !in factsHandledIds
    }
    if (metrics.isEmpty()) return

    MyTokenSection(
        title = "用量明细",
        modifier = Modifier.testTag("detail_metrics"),
    ) {
        FactGrid(
            rows = metrics.map { metric ->
                metric.label.ifBlank { metric.id } to formatDetailMetricValue(metric)
            },
        )
    }
}

internal fun formatDetailMetricValue(metric: UsageMetric): String = when {
    metric.id.contains("duration") -> formatUsageDuration(metric.value)
    metric.unit == UsageMetricUnit.Currency ->
        formatCurrency(metric.value, metric.currencyCode)
    metric.unit == UsageMetricUnit.Token -> "${formatCompact(metric.value)} tokens"
    metric.unit == UsageMetricUnit.Request -> "${formatCompact(metric.value)} 次"
    metric.unit == UsageMetricUnit.Boolean_ ->
        if ((metric.value ?: BigDecimal.ZERO).signum() > 0) "是" else "否"
    else -> formatDecimal(metric.value)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailWindowRow(metric: UsageMetric) {
    val colors = MyTokenThemeTokens.colors
    val percent = progressPercent(metric)
    val tone = CompactUsageCardPresentation.tone(metric)
    val end = metric.windowEnd
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = metric.label.ifBlank { windowLabel(metric) },
                style = MyTokenType.label.copy(fontWeight = FontWeight(600)),
                color = colors.ink,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Text(
                text = formatPercent(percent),
                style = MyTokenType.label.copy(
                    fontWeight = FontWeight(700),
                    fontFeatureSettings = "tnum",
                ),
                color = when (tone) {
                    UsageMetricTone.Warning -> colors.warningText
                    UsageMetricTone.Critical -> colors.criticalText
                    UsageMetricTone.Normal -> colors.ink
                },
            )
        }
        UsageSegmentMeter(
            percent = percent,
            tone = tone,
            modifier = Modifier.padding(top = 7.dp),
            height = 11.dp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "剩余 ${formatRemaining(metric)}",
                style = MyTokenType.caption,
                color = colors.muted,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Text(
                text = end?.let { "${formatRemainingDuration(it, Instant.now())} 后重置" } ?: "—",
                style = MyTokenType.caption,
                color = colors.muted,
                maxLines = 1,
            )
        }
        if (metric.resetCards.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                metric.resetCards.forEachIndexed { index, card ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(colors.surface, MyTokenReportBodyShape)
                            .border(1.dp, colors.line, MyTokenReportBodyShape)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("detail_reset_ticket"),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            ResetCardsGlyph(
                                color = colors.muted,
                                modifier = Modifier.size(width = 9.dp, height = 8.dp),
                            )
                            Text(
                                text = "重置卡 ${index + 1}",
                                style = MyTokenType.caption,
                                color = colors.muted,
                            )
                        }
                        Text(
                            text = formatResetCardCompact(card.expiresAt),
                            modifier = Modifier.padding(top = 4.dp),
                            style = MyTokenType.caption.copy(fontFamily = FontFamily.Monospace),
                            color = colors.ink,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyBalanceRow(card: CredentialCardUi, balance: UsageMetric) {
    val colors = MyTokenThemeTokens.colors
    val toppedUp = findBalanceMetric(card, balance, "toppedUpBalance")
    val granted = findBalanceMetric(card, balance, "grantedBalance")
    val primaryValue = toppedUp?.value ?: BigDecimal.ZERO
    val secondaryValue = granted?.value ?: BigDecimal.ZERO
    val total = primaryValue + secondaryValue
    val primaryWeight = if (total.signum() > 0) primaryValue.toDouble() / total.toDouble() else 0.0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(colors.surface, MyTokenReportBodyShape)
            .border(1.dp, colors.line, MyTokenReportBodyShape)
            .padding(12.dp)
            .testTag("detail_balance_ticket"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${balance.currencyCode ?: "余额"} 账户",
                style = MyTokenType.label,
                color = colors.muted,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatCurrency(balance.value, balance.currencyCode),
                style = MyTokenType.numericValue,
                color = colors.ink,
                maxLines = 1,
            )
        }
        UsageCompositionMeter(
            primaryWeight = primaryWeight.takeIf { total.signum() > 0 },
            height = 9.dp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BalanceLegend("充值", toppedUp?.value, balance.currencyCode, colors.ink)
            BalanceLegend("赠金", granted?.value, balance.currencyCode, colors.actionFill)
        }
    }
}

/** 套餐与订阅事实网格；Command Code 的额度构成与累计请求也在此保留。 */
@Composable
internal fun DetailFactsSection(card: CredentialCardUi) {
    val colors = MyTokenThemeTokens.colors
    val snapshot = card.snapshot ?: return
    val metrics = snapshot.metrics
    val byId = metrics.associateBy(UsageMetric::id)

    MyTokenSection(
        title = "套餐与订阅",
        modifier = Modifier.testTag("detail_facts"),
    ) {
        FactGrid(
            rows = listOf(
                "套餐" to snapshot.planName.ifEmpty { planName(card) },
                "类型" to if (card.credential.providerId == ProviderId.Volcengine) {
                    "周期订阅"
                } else {
                    usageType(snapshot.usageKind)
                },
                "状态" to (snapshot.statusText ?: subscriptionStatus(card, snapshot.status)),
                "计费" to (snapshot.billingMode ?: "接口未返回"),
                "开始" to (snapshot.subscriptionStartAt?.let(::formatFullTime) ?: "接口未返回"),
                "结束" to (snapshot.subscriptionEndAt?.let(::formatFullTime) ?: "接口未返回"),
            ),
        )
        if (card.credential.providerId == ProviderId.CommandCode) {
            val composition = listOfNotNull(
                byId["monthly-remaining"]?.let { "月度 ${formatCurrency(it.value, it.currencyCode)}" },
                byId["purchased-remaining"]?.let { "购买 ${formatCurrency(it.value, it.currencyCode)}" },
                byId["free-remaining"]?.let { "赠送 ${formatCurrency(it.value, it.currencyCode)}" },
            )
            if (composition.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "额度构成",
                        style = MyTokenType.caption,
                        color = colors.muted,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = composition.joinToString(" · "),
                        style = MyTokenType.caption,
                        color = colors.ink,
                        maxLines = 1,
                    )
                }
            }
            byId["request-count"]?.let { requests ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "累计请求",
                        style = MyTokenType.caption,
                        color = colors.muted,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${formatCompact(requests.value)} 次",
                        style = MyTokenType.caption,
                        color = colors.ink,
                    )
                }
            }
        }
        // 购买剩余/赠送剩余在这一页仍然可读。
        listOf("purchased-remaining" to "购买剩余", "free-remaining" to "赠送剩余").forEach { (id, label) ->
            byId[id]?.let { metric ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = label, style = MyTokenType.caption, color = colors.muted)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = formatCurrency(metric.value, metric.currencyCode),
                        style = MyTokenType.caption,
                        color = colors.ink,
                    )
                }
            }
        }
    }

    MyTokenSection(
        title = "账户与模型",
        modifier = Modifier.testTag("detail_models"),
    ) {
        AllowedModelsSection(models = snapshot.allowedModels)
    }
}

@Composable
private fun FactGrid(rows: List<Pair<String, String>>) {
    val colors = MyTokenThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { (label, value) ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = label, style = MyTokenType.caption, color = colors.muted)
                        Text(
                            text = value,
                            style = MyTokenType.caption,
                            color = colors.ink,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** 配置：官网链接、凭证类型与低额度预警值。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DetailConfigSection(
    metadata: Map<CredentialMetadataKey, String>,
    providerId: ProviderId,
    credentialKind: ai.routin.mytoken.domain.model.CredentialKind,
    lowThresholdPercent: Int?,
) {
    val showThreshold = providerId == ProviderId.DeepSeek && lowThresholdPercent != null
    if (metadata.isEmpty() && !showThreshold) return
    val colors = MyTokenThemeTokens.colors
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    MyTokenSection(
        title = "配置",
        modifier = Modifier.testTag("detail_config"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "凭证类型", style = MyTokenType.caption, color = colors.muted)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = credentialKindLabel(credentialKind, providerId),
                style = MyTokenType.caption,
                color = colors.ink,
            )
        }
        metadata.entries.sortedBy { it.key.rawValue }.forEach { (key, value) ->
            if (key == CredentialMetadataKey.WebsiteURL) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "官网地址",
                        style = MyTokenType.caption,
                        color = colors.muted,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = wrapUrlForDisplay(value),
                        style = MyTokenType.caption.copy(lineBreak = LineBreak.Paragraph),
                        color = colors.actionText,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("detail_website_url")
                            .combinedClickable(
                                role = Role.Button,
                                onClick = {
                                    runCatching { uriHandler.openUri(value) }
                                },
                                onLongClick = {
                                    clipboard.setText(AnnotatedString(value))
                                    Toast.makeText(context, "复制成功", Toast.LENGTH_SHORT).show()
                                },
                            ),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = colors.actionText,
                        modifier = Modifier
                            .padding(start = 4.dp, top = 2.dp)
                            .size(14.dp),
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = metadataLabel(key), style = MyTokenType.caption, color = colors.muted)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = metadataValue(key, value),
                        style = MyTokenType.caption,
                        color = colors.ink,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
        if (showThreshold) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "低额度预警值", style = MyTokenType.caption, color = colors.muted)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "$lowThresholdPercent", style = MyTokenType.caption, color = colors.ink)
            }
        }
    }
}

/** 充值/赠金等构成项由余额卡内部的图例展示，不再单独占一张票据。 */
private fun isBalanceComponentId(id: String): Boolean = listOf(
    "toppedUpBalance",
    "grantedBalance",
    "cash-balance",
    "gift-balance",
).any(id::startsWith)

private fun findBalanceMetric(
    card: CredentialCardUi,
    balance: UsageMetric,
    prefix: String,
): UsageMetric? {
    val metrics = card.snapshot?.metrics.orEmpty()
    val currency = balance.currencyCode?.uppercase().orEmpty()
    return metrics.firstOrNull { it.id == "$prefix-$currency" }
        ?: metrics.firstOrNull { it.id == prefix }
}

internal fun formatDetailAmount(value: BigDecimal?, metric: UsageMetric): String = when (metric.unit) {
    UsageMetricUnit.Currency -> formatCurrency(value, metric.currencyCode)
    UsageMetricUnit.Request -> "${formatGrouped(value)} 次"
    else -> formatGrouped(value)
}
