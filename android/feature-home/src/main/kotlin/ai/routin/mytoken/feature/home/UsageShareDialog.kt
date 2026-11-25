package ai.routin.mytoken.feature.home

import android.widget.Toast
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenSurface
import ai.routin.mytoken.core.ui.MyTokenTextField
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.core.ui.MyTokenVisualPolicy
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun UsageShareDialog(
    card: CredentialCardUi,
    onDismiss: () -> Unit,
    layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact,
) {
    val context = LocalContext.current
    val colors = MyTokenThemeTokens.colors
    val content = remember(card) { UsageShareContentBuilder.build(card) }
    LaunchedEffect(content) {
        if (content == null) onDismiss()
    }
    content ?: return

    var draft by remember(content) { mutableStateOf(UsageShareContentBuilder.makeDraft(content)) }
    var fieldsExpanded by remember(content) { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val rendered = remember(draft) { UsageShareContentBuilder.render(content, draft) }
    val currentRendered by rememberUpdatedState(rendered)

    val runExport: (UsageShareAction) -> Unit = { action ->
        scope.launch {
            if (isExporting) return@launch
            isExporting = true
            val message = exportUsageShareImage(context, currentRendered, action)
            isExporting = false
            message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        }
    }

    val header: @Composable () -> Unit = {
        ShareHeader(isExporting = isExporting, onAction = runExport)
    }

    val privacyNote: @Composable () -> Unit = {
        Text(
            text = "数字完全来自本地快照，真实用量不可篡改",
            style = MyTokenType.caption,
            color = colors.muted,
        )
    }

    val editorControls: @Composable ColumnScope.() -> Unit = {
        ShareControlSection(title = "模板风格") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                UsageShareTemplate.entries.forEach { template ->
                    ShareTemplateOption(
                        template = template,
                        selected = draft.template == template,
                        onClick = { draft = draft.copy(template = template) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_template_${template.name}"),
                    )
                }
            }
        }

        MyTokenTextField(
            value = draft.displayName,
            onValueChange = { draft = draft.copy(displayName = it) },
            label = "显示名称",
            supportingText = "仅用于本次图片展示",
            contentPadding = 8.dp,
        )
        MyTokenTextField(
            value = draft.subtitle,
            onValueChange = { draft = draft.copy(subtitle = it) },
            label = "套餐文案",
            supportingText = "可补充团队说明",
            contentPadding = 8.dp,
        )
        MyTokenTextField(
            value = draft.note,
            onValueChange = { draft = draft.copy(note = it) },
            label = "卡片附注说明（可选）",
            supportingText = "仅出现在分享图",
            contentPadding = 8.dp,
        )

        ShareControlSection(
            title = "展示字段",
            action = {
                TextButton(onClick = { draft = draft.showAll() }) {
                    Text(text = "全部显示")
                }
            },
        ) {
            val toggles = visibleToggles(content)
            val collapsible = toggles.size > 4
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("share_field_flow"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                maxLines = if (fieldsExpanded || !collapsible) Int.MAX_VALUE else 1,
                overflow = if (fieldsExpanded || !collapsible) {
                    FlowRowOverflow.Clip
                } else {
                    FlowRowOverflow.expandIndicator {
                        ShareFieldMoreChip(
                            testTag = "share_field_more",
                            onClick = { fieldsExpanded = true },
                        )
                    }
                },
            ) {
                toggles.forEachIndexed { index, item ->
                    ShareToggleChip(
                        title = item.title,
                        checked = item.isChecked(draft),
                        testTag = "share_field_chip_$index",
                        onToggle = { draft = item.toggle(draft) },
                    )
                }
            }
            if (fieldsExpanded && collapsible) {
                Text(
                    text = "收起",
                    style = MyTokenType.caption,
                    color = colors.muted,
                    modifier = Modifier
                        .testTag("share_field_collapse")
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(role = Role.Button) { fieldsExpanded = false }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }
    }

    val preview: @Composable ColumnScope.() -> Unit = {
        Text(
            text = "实时导出预览",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        UsageShareCardPreview(
            card = rendered,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .testTag("share_preview_pane"),
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.overlay,
        contentColor = colors.ink,
    ) {
        if (layoutMode == MyTokenLayoutMode.Compact) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 780.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                header()
                preview()
                editorControls()
                privacyNote()
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                header()
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
                        privacyNote()
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareHeader(
    isExporting: Boolean,
    onAction: (UsageShareAction) -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "分享用量",
            style = MyTokenType.section,
            color = colors.ink,
            modifier = Modifier.weight(1f),
        )
        Row(modifier = Modifier.testTag("share_actions_row")) {
            ShareActionButton(
                icon = Icons.Outlined.ContentCopy,
                description = "复制分享图",
                testTag = "share_action_copy",
                enabled = !isExporting,
                onClick = { onAction(UsageShareAction.Copy) },
            )
            ShareActionButton(
                icon = Icons.Outlined.SaveAlt,
                description = "保存分享图",
                testTag = "share_action_save",
                enabled = !isExporting,
                onClick = { onAction(UsageShareAction.Save) },
            )
            ShareActionButton(
                icon = Icons.AutoMirrored.Outlined.Forward,
                description = "分享用量",
                testTag = "share_action_share",
                enabled = !isExporting,
                onClick = { onAction(UsageShareAction.Share) },
            )
        }
    }
}

@Composable
private fun ShareActionButton(
    icon: ImageVector,
    description: String,
    testTag: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.testTag(testTag),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) colors.ink else colors.muted,
        )
    }
}

@Composable
private fun ShareTemplateOption(
    template: UsageShareTemplate,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.raisedSubtle else Color.Transparent)
            .border(1.dp, if (selected) colors.actionFill else colors.raisedLine, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = templateIcon(template),
            contentDescription = null,
            tint = if (selected) colors.actionText else colors.muted,
            modifier = Modifier.size(17.dp),
        )
        Text(
            text = template.title,
            style = MyTokenType.caption,
            color = if (selected) colors.ink else colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun templateIcon(template: UsageShareTemplate): ImageVector = when (template) {
    UsageShareTemplate.TicketLight -> Icons.Outlined.ConfirmationNumber
    UsageShareTemplate.Ticket -> Icons.Filled.ConfirmationNumber
    UsageShareTemplate.Light -> Icons.Outlined.LightMode
    UsageShareTemplate.Dark -> Icons.Outlined.DarkMode
}

@Composable
private fun ShareControlSection(
    title: String,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    MyTokenSurface(
        // 分享弹层（overlay）里的分组卡：使用浮层内的前景块。
        surfaceRole = MyTokenVisualPolicy.SurfaceRole.Raised,
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
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
private fun ShareToggleChip(
    title: String,
    checked: Boolean,
    testTag: String,
    onToggle: () -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    val shape = RoundedCornerShape(8.dp)
    Text(
        text = title,
        style = MyTokenType.caption,
        color = if (checked) colors.ink else colors.muted,
        maxLines = 1,
        modifier = Modifier
            .testTag(testTag)
            .clip(shape)
            .background(if (checked) colors.raisedSubtle else Color.Transparent)
            .border(1.dp, colors.raisedLine, shape)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun ShareFieldMoreChip(testTag: String, onClick: () -> Unit) {
    val colors = MyTokenThemeTokens.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = colors.raisedSubtle,
        border = BorderStroke(1.dp, colors.raisedLine),
        modifier = Modifier.testTag(testTag),
    ) {
        Text(
            text = "更多",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MyTokenType.caption,
            color = colors.actionText,
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
