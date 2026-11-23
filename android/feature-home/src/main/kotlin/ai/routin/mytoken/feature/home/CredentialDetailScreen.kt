package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.alerts.AlertThresholds
import ai.routin.mytoken.core.ui.MyTokenAdaptiveContent
import ai.routin.mytoken.core.ui.MyTokenButton
import ai.routin.mytoken.core.ui.MyTokenButtonTone
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenSize
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.CredentialMetadataKey
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.usage.RefreshStatus
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flight
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val fullTimeFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss")

internal fun formatFullTime(instant: Instant?): String {
    if (instant == null) return "-"
    return fullTimeFormatter.format(instant.atZone(ZoneId.systemDefault()))
}

internal fun credentialKindLabel(kind: CredentialKind, provider: ProviderId): String = when (kind) {
    CredentialKind.BearerApiKey -> if (provider == ProviderId.Routin) "Plan Key" else "Bearer Token"
    CredentialKind.ApiKey -> "API Key"
    CredentialKind.AccessKeyPair -> "Access Key Pair"
}

@Composable
fun CredentialDetailScreen(
    card: CredentialCardUi?,
    globalAlertThresholds: AlertThresholds = AlertThresholds.DEFAULT,
    alertThresholdOverride: AlertThresholds? = null,
    layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact,
    nextRefreshAt: Instant? = null,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    var showShare by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            DetailTopBar(
                cardExists = card != null,
                onBack = onBack,
                onShare = { showShare = true },
                onDelete = onDelete,
            )
        },
        bottomBar = {
            if (card != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.canvas),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 840.dp)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MyTokenButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("detail_edit"),
                            tone = MyTokenButtonTone.Neutral,
                            text = "编辑",
                        )
                        MyTokenButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("detail_refresh_now"),
                            tone = MyTokenButtonTone.Primary,
                            text = "立即刷新",
                        )
                    }
                }
            }
        },
    ) { padding ->
        if (card == null) {
            Text(
                text = "凭证不存在或已被删除",
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp),
            )
            return@Scaffold
        }

        MyTokenAdaptiveContent(
            maxWidth = 840.dp,
            horizontalPadding = 16.dp,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp)
                    .testTag("detail_content"),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                DetailHero(card = card, nextRefreshAt = nextRefreshAt)
                DetailWindowsSection(card = card)
                DetailMetricsSection(card = card)
                DetailFactsSection(card = card)
                DetailConfigSection(
                    metadata = card.credential.metadata,
                    providerId = card.credential.providerId,
                    credentialKind = card.credential.credentialKind,
                    globalAlertThresholds = globalAlertThresholds,
                    alertThresholdOverride = alertThresholdOverride,
                )
            }
        }
    }
    if (showShare && card != null) {
        UsageShareDialog(
            card = card,
            onDismiss = { showShare = false },
            layoutMode = layoutMode,
        )
    }
}

@Composable
private fun DetailTopBar(
    cardExists: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.canvas)
                .statusBarsPadding()
                .heightIn(min = MyTokenSize.touch),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopBarIconButton(
                contentDescription = "返回",
                onClick = onBack,
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = colors.ink,
                        modifier = Modifier.size(20.dp),
                    )
                },
            )
            Text(
                text = "详情",
                style = MyTokenType.section,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            if (cardExists) {
                TopBarIconButton(
                    contentDescription = "分享用量",
                    onClick = onShare,
                    icon = {
                        AirplaneTicketGlyph(
                            tint = colors.ink,
                            modifier = Modifier
                                .size(20.dp)
                                .testTag("detail_share_icon"),
                        )
                    },
                )
                TopBarIconButton(
                    contentDescription = "删除凭证",
                    onClick = onDelete,
                    modifier = Modifier.padding(end = 16.dp).testTag("detail_delete"),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = colors.criticalFill,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
            }
        }
    }
}

/** SF 的 airplane.ticket：票据轮廓 + 虚线撕裂线 + 小飞机。 */
@Composable
private fun AirplaneTicketGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 1.4.dp.toPx()
            val insetX = 1.4.dp.toPx()
            val insetY = 2.5.dp.toPx()
            drawRoundRect(
                color = tint,
                topLeft = Offset(insetX, insetY),
                size = Size(size.width - insetX * 2f, size.height - insetY * 2f),
                cornerRadius = CornerRadius(2.6.dp.toPx()),
                style = Stroke(width = stroke),
            )
            val perforationX = size.width * 0.38f
            drawLine(
                color = tint,
                start = Offset(perforationX, insetY + 1.dp.toPx()),
                end = Offset(perforationX, size.height - insetY - 1.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(1.1.dp.toPx(), 1.1.dp.toPx()),
                ),
            )
        }
        Icon(
            imageVector = Icons.Filled.Flight,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .padding(start = 6.dp)
                .size(8.5.dp),
        )
    }
}

@Composable
private fun TopBarIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(MyTokenSize.touch)
            .then(modifier)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

internal fun planName(card: CredentialCardUi): String {
    card.snapshot?.planName?.takeIf { it.isNotEmpty() }?.let { return it }
    return when (card.credential.providerId) {
        ProviderId.Routin -> "Plan Key"
        ProviderId.DeepSeek -> "API 余额"
        ProviderId.Glm -> "Coding Plan"
        ProviderId.Volcengine ->
            if (card.credential.metadata[CredentialMetadataKey.PlanType] == "coding") "Coding Plan" else "Agent Plan"
        ProviderId.NewAPI -> "API 额度"
        ProviderId.CommandCode -> "Command Code"
        ProviderId.Xiaomi ->
            if (card.credential.metadata[CredentialMetadataKey.UsageKind] == "plan") "Token Plan" else "API 按量"
        ProviderId.OpenCode -> "OpenCode Go"
    }
}

internal fun usageType(kind: String?): String = when (kind) {
    "periodic" -> "周期订阅"
    "tokenPack" -> "Token 资源包"
    "api" -> "API 按量"
    "plan" -> "Token Plan"
    "balance" -> "余额查询"
    "codingPlan" -> "Coding Plan"
    "agentPlan" -> "Agent Plan"
    "usageStats" -> "用量统计"
    else -> "-"
}

internal fun subscriptionStatus(card: CredentialCardUi, status: Int?): String = when {
    card.status == RefreshStatus.Failed -> card.error?.message ?: "更新失败"
    card.snapshot == null -> "暂无数据"
    card.isStale -> "已过期"
    status == null || status == 1 -> "正常"
    else -> "状态 $status"
}

internal fun metadataLabel(key: CredentialMetadataKey): String = when (key) {
    CredentialMetadataKey.BaseURL -> "接口地址"
    CredentialMetadataKey.UserID -> "用户 ID"
    CredentialMetadataKey.Region -> "区域"
    CredentialMetadataKey.PlanType -> "计划类型"
    CredentialMetadataKey.UsageKind -> "用量类型"
    CredentialMetadataKey.WebsiteURL -> "官网地址"
    CredentialMetadataKey.BalanceWarningThreshold -> "低余额预警值"
}

internal fun metadataValue(key: CredentialMetadataKey, value: String): String = when (key) {
    CredentialMetadataKey.PlanType -> if (value == "coding") "Coding Plan" else "Agent Plan"
    CredentialMetadataKey.UsageKind -> when (value) {
        "tokenPack" -> "Token 资源包"
        "api" -> "API 按量"
        "plan" -> "Token Plan"
        else -> value
    }
    else -> value
}
