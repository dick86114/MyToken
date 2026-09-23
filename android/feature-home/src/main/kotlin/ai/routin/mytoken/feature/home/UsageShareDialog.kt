package ai.routin.mytoken.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.routin.mytoken.core.ui.GlassButton
import ai.routin.mytoken.core.ui.GlassButtonTone
import ai.routin.mytoken.core.ui.GlassSwitch
import ai.routin.mytoken.core.ui.GlassTextField
import ai.routin.mytoken.core.ui.LiquidGlassSurface
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenVisualPolicy
import ai.routin.mytoken.core.ui.glassFilterChipBorder
import ai.routin.mytoken.core.ui.glassFilterChipColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun UsageShareDialog(
    card: CredentialCardUi,
    onDismiss: () -> Unit,
    layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact,
) {
    val context = LocalContext.current
    val content = remember(card) { UsageShareContentBuilder.build(card) }
    LaunchedEffect(content) {
        if (content == null) onDismiss()
    }
    content ?: return

    var draft by remember(content) { mutableStateOf(UsageShareContentBuilder.makeDraft(content)) }
    var actionStatus by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val rendered = remember(draft) { UsageShareContentBuilder.render(content, draft) }
    val currentRendered by rememberUpdatedState(rendered)

    val exportActions: @Composable ColumnScope.() -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("share_actions_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassButton(
                text = "复制",
                icon = Icons.Outlined.ContentCopy,
                tone = GlassButtonTone.Primary,
                enabled = !isExporting,
                onClick = {
                    scope.launch {
                        isExporting = true
                        actionStatus = exportUsageShareImage(context, currentRendered, UsageShareAction.Copy)
                        isExporting = false
                    }
                },
                modifier = Modifier.weight(1f),
            )
            GlassButton(
                text = "保存",
                icon = Icons.Outlined.SaveAlt,
                enabled = !isExporting,
                onClick = {
                    scope.launch {
                        isExporting = true
                        actionStatus = exportUsageShareImage(context, currentRendered, UsageShareAction.Save)
                        isExporting = false
                    }
                },
                modifier = Modifier.weight(1f),
            )
            GlassButton(
                text = "更多",
                icon = Icons.Outlined.Share,
                enabled = !isExporting,
                onClick = {
                    scope.launch {
                        isExporting = true
                        exportUsageShareImage(context, currentRendered, UsageShareAction.Share)
                        isExporting = false
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }

    val statusText: @Composable ColumnScope.() -> Unit = {
        Text(
            text = actionStatus ?: "数字完全来自本地快照，真实用量不可篡改",
            style = MaterialTheme.typography.bodySmall,
            color = if (actionStatus == null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }

    val editorControls: @Composable ColumnScope.() -> Unit = {
        ShareControlSection(
            title = "模板风格",
            supportingText = draft.template.previewTag,
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                UsageShareTemplate.entries.forEach { template ->
                    FilterChip(
                        selected = draft.template == template,
                        onClick = { draft = draft.copy(template = template) },
                        label = { Text(text = template.title) },
                        leadingIcon = if (template == UsageShareTemplate.Ticket || template == UsageShareTemplate.TicketLight) {
                            {
                                Icon(
                                    imageVector = if (template == UsageShareTemplate.Ticket) {
                                        Icons.Filled.ConfirmationNumber
                                    } else {
                                        Icons.Outlined.ConfirmationNumber
                                    },
                                    contentDescription = null,
                                )
                            }
                        } else {
                            null
                        },
                        colors = glassFilterChipColors(selected = draft.template == template),
                        border = glassFilterChipBorder(selected = draft.template == template),
                    )
                }
            }
        }

        GlassTextField(
            value = draft.displayName,
            onValueChange = { draft = draft.copy(displayName = it) },
            label = "显示名称",
            supportingText = "仅用于本次图片展示",
            contentPadding = 8.dp,
        )
        GlassTextField(
            value = draft.subtitle,
            onValueChange = { draft = draft.copy(subtitle = it) },
            label = "套餐文案",
            supportingText = "可补充团队说明",
            contentPadding = 8.dp,
        )
        GlassTextField(
            value = draft.note,
            onValueChange = { draft = draft.copy(note = it) },
            label = "卡片附注说明（可选）",
            supportingText = "仅出现在分享图",
            contentPadding = 8.dp,
        )

        ShareControlSection(
            title = "展示字段开关",
            supportingText = "按票面顺序排列，隐藏即不导出",
            action = {
                TextButton(onClick = { draft = draft.showAll() }) {
                    Text(text = "全部显示")
                }
            },
        ) {
            visibleToggles(content).forEach { item ->
                ShareToggleRow(
                    title = item.title,
                    checked = item.isChecked(draft),
                    onToggle = { draft = item.toggle(draft) },
                )
            }
        }
    }

    val preview: @Composable ColumnScope.() -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "实时导出预览",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = draft.template.previewTag,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        UsageShareCardPreview(
            card = rendered,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .testTag("share_preview_pane"),
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (layoutMode == MyTokenLayoutMode.Compact) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 780.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "分享用量",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                preview()
                editorControls()
                exportActions()
                statusText()
                Text(
                    text = " ",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "分享用量",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.6f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .testTag("share_wide_preview"),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        preview()
                    }
                    Column(
                        modifier = Modifier
                            .width(340.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .testTag("share_settings_pane"),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        editorControls()
                        exportActions()
                        statusText()
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareControlSection(
    title: String,
    supportingText: String? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    LiquidGlassSurface(
        surfaceRole = MyTokenVisualPolicy.SurfaceRole.Card,
        shape = RoundedCornerShape(MyTokenVisualPolicy.outerRadiusDp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (supportingText != null) {
                        Text(
                            text = supportingText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                action?.invoke()
            }
            content()
        }
    }
}

internal data class ShareToggle(
    val title: String,
    val isChecked: (UsageShareDraft) -> Boolean,
    val toggle: (UsageShareDraft) -> UsageShareDraft,
)

@Composable
private fun ShareToggleRow(
    title: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MyTokenVisualPolicy.innerRadiusDp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        GlassSwitch(
            checked = checked,
            onCheckedChange = null,
            contentDescription = title,
        )
    }
}

internal fun visibleToggles(content: UsageShareContent): List<ShareToggle> = buildList {
    add(
        ShareToggle(
            "可用状态徽章",
            { it.showsStatus },
            { draft -> draft.copy(showsStatus = !draft.showsStatus) },
        ),
    )
    if (content.planName.isNotEmpty() || content.subtitle.isNotEmpty()) {
        add(
            ShareToggle(
                "套餐规格",
                { it.showsSubtitle },
                { draft -> draft.copy(showsSubtitle = !draft.showsSubtitle) },
            ),
        )
    }
    if (content.subscriptionStartText != null || content.subscriptionEndText != null || content.cycleRemainingText != null) {
        add(
            ShareToggle(
                "订阅周期/到期",
                { it.showsSubscriptionDates },
                { draft -> draft.copy(showsSubscriptionDates = !draft.showsSubscriptionDates) },
            ),
        )
    }
    if (content.cycleRemainingText != null) {
        add(
            ShareToggle(
                "周期剩余",
                { it.showsCycleRemaining },
                { draft -> draft.copy(showsCycleRemaining = !draft.showsCycleRemaining) },
            ),
        )
    }
    add(ShareToggle("附加备注框", { it.showsNote }, { draft -> draft.copy(showsNote = !draft.showsNote) }))
    if (content.metrics.any { it.resetBadgeText != null || it.timeDetails.isNotEmpty() }) {
        add(
            ShareToggle(
                "重置时间与倒计时",
                { it.showsResetTimes },
                { draft -> draft.copy(showsResetTimes = !draft.showsResetTimes) },
            ),
        )
    }
    val progressMetrics = content.metrics.filter { it.percent != null }
    val valueMetrics = content.metrics.filter { it.percent == null }
    (progressMetrics + valueMetrics).forEach { item ->
        add(
            ShareToggle(
                item.title,
                { draft -> draft.isMetricVisible(item.id) },
                { draft -> draft.setMetricVisible(item.id, !draft.isMetricVisible(item.id)) },
            ),
        )
    }
    if (content.groupMultiplierText != null) {
        add(
            ShareToggle(
                "分组倍率",
                { it.showsGroupMultiplier },
                { draft -> draft.copy(showsGroupMultiplier = !draft.showsGroupMultiplier) },
            ),
        )
    }
    add(ShareToggle("快照水印与防伪", { it.showsWatermark }, { draft -> draft.copy(showsWatermark = !draft.showsWatermark) }))
}
