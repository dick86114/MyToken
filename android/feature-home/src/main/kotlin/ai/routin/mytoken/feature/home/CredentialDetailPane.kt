package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.alerts.AlertThresholds
import ai.routin.mytoken.core.ui.MyTokenButton
import ai.routin.mytoken.core.ui.MyTokenButtonTone
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenRadius
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas

/**
 * 大屏右侧详情：直接复用当前详情页的 Hero、全部窗口、用量明细、套餐与订阅、
 * 配置五个区块，以及分享 / 删除 / 编辑 / 立即刷新四个操作；只改造外壳为侧栏。
 */
@Composable
internal fun CredentialDetailPane(
    card: CredentialCardUi,
    layoutMode: MyTokenLayoutMode,
    globalAlertThresholds: AlertThresholds,
    alertThresholdOverride: AlertThresholds?,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    var showShare by remember(card.credential.id) { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        DetailPaneHeader(
            onClose = onClose,
            onShare = { showShare = true },
            onDelete = onDelete,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .testTag("detail_content"),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            DetailHero(card = card, nextRefreshAt = null)
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
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

    if (showShare) {
        UsageShareDialog(
            card = card,
            onDismiss = { showShare = false },
            layoutMode = layoutMode,
        )
    }
}

@Composable
private fun DetailPaneHeader(
    onClose: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "详情",
            modifier = Modifier.weight(1f),
            style = MyTokenType.section,
            color = colors.ink,
            maxLines = 1,
        )
        PaneIconButton(
            contentDescription = "分享用量",
            onClick = onShare,
            tag = "detail_share_icon",
        ) {
            PaneAirplaneTicketGlyph(
                tint = colors.ink,
                modifier = Modifier.size(20.dp),
            )
        }
        PaneIconButton(
            contentDescription = "删除凭证",
            onClick = onDelete,
            tag = "detail_delete",
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = colors.criticalFill,
                modifier = Modifier.size(20.dp),
            )
        }
        PaneIconButton(
            contentDescription = "关闭详情",
            onClick = onClose,
            tag = "home_wide_detail_close",
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun PaneIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    tag: String,
    icon: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(MyTokenRadius.inner))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

/** 与详情页顶栏一致的机票图标，保持分享入口的视觉语义。 */
@Composable
private fun PaneAirplaneTicketGlyph(
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
