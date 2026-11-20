package ai.routin.mytoken.feature.credentials

import ai.routin.mytoken.core.ui.MyTokenAdaptiveContent
import ai.routin.mytoken.core.ui.MyTokenButton
import ai.routin.mytoken.core.ui.MyTokenButtonTone
import ai.routin.mytoken.core.ui.MyTokenDialog
import ai.routin.mytoken.core.ui.MyTokenDialogRow
import ai.routin.mytoken.core.ui.MyTokenDialogSection
import ai.routin.mytoken.core.ui.MyTokenDialogStatus
import ai.routin.mytoken.core.ui.MyTokenDialogTone
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenRadius
import ai.routin.mytoken.core.ui.MyTokenSize
import ai.routin.mytoken.core.ui.MyTokenSurface
import ai.routin.mytoken.core.ui.MyTokenSwitch
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.core.ui.MyTokenVisualPolicy
import ai.routin.mytoken.core.ui.ProviderLogoPlate
import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.ProviderId
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import java.util.UUID
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun CredentialListScreen(
    state: CredentialListUiState,
    onToggleEnabled: (Credential) -> Unit,
    onMove: (Int, Int) -> Unit,
    onEditCredential: (UUID) -> Unit,
    onRequestDelete: (Credential) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onImportFromMac: () -> Unit,
    onAddManually: () -> Unit,
    layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact,
    drawerMode: Boolean = false,
) {
    val colors = MyTokenThemeTokens.colors
    Scaffold(
        containerColor = if (drawerMode) Color.Transparent else colors.canvas,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .then(if (drawerMode) Modifier.navigationBarsPadding() else Modifier)
                .fillMaxSize()
                .testTag(if (drawerMode) "credential_drawer_content" else "credential_list_content"),
        ) {
            CredentialPageHeader(
                enabledCount = state.items.count { it.isEnabled },
                totalCount = state.items.size,
                onImportFromMac = onImportFromMac,
                onAddManually = onAddManually,
                drawerMode = drawerMode,
            )
            if (state.items.isEmpty()) {
                EmptyState(
                    modifier = Modifier.weight(1f),
                    onImportFromMac = onImportFromMac,
                    onAddManually = onAddManually,
                )
            } else {
                SectionRow()
                // 抽屉始终单列：屏宽再大也不铺双列。
                if (!drawerMode && layoutMode == MyTokenLayoutMode.Expanded) {
                    ExpandedCredentialGrid(
                        state = state,
                        onToggleEnabled = onToggleEnabled,
                        onMove = onMove,
                        onEditCredential = onEditCredential,
                        onRequestDelete = onRequestDelete,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    CredentialRowList(
                        state = state,
                        drawerMode = drawerMode,
                        onToggleEnabled = onToggleEnabled,
                        onMove = onMove,
                        onEditCredential = onEditCredential,
                        onRequestDelete = onRequestDelete,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (!drawerMode) {
                    PrivacyFooter()
                }
            }
        }
    }

    state.pendingDeletion?.let { target ->
        MyTokenDialog(
            title = "删除凭证",
            subtitle = "只影响本机数据",
            icon = Icons.Outlined.Delete,
            tone = MyTokenDialogTone.Danger,
            onDismissRequest = onDismissDelete,
            content = {
                MyTokenDialogSection {
                    MyTokenDialogRow(
                        label = "「${target.name}」",
                        detail = "将从首页和凭证列表移除",
                    )
                }
                MyTokenDialogStatus(
                    message = "不会删除或修改供应商账户中的任何内容。",
                    tone = MyTokenDialogTone.Info,
                )
            },
            actions = {
                MyTokenButton(
                    text = "取消",
                    onClick = onDismissDelete,
                    modifier = Modifier.weight(1f),
                    tone = MyTokenButtonTone.Neutral,
                )
                MyTokenButton(
                    text = "删除",
                    onClick = onConfirmDelete,
                    modifier = Modifier.weight(1f),
                    tone = MyTokenButtonTone.Destructive,
                )
            },
        )
    }
}

@Composable
private fun CredentialPageHeader(
    enabledCount: Int,
    totalCount: Int,
    onImportFromMac: () -> Unit,
    onAddManually: () -> Unit,
    drawerMode: Boolean,
) {
    val colors = MyTokenThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "凭证",
                style = MyTokenType.title,
                color = colors.ink,
            )
            if (totalCount > 0) {
                Text(
                    text = "$totalCount 个凭证 · $enabledCount 个启用",
                    style = MyTokenType.label,
                    color = colors.muted,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .testTag("credential_enabled_summary"),
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!drawerMode) {
                ImportButton(onClick = onImportFromMac)
            }
            AddButton(onClick = onAddManually)
        }
    }
}

@Composable
private fun ImportButton(onClick: () -> Unit) {
    val colors = MyTokenThemeTokens.colors
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "从 Mac 导入" }
            .testTag("credential_import_button"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.QrCodeScanner,
            contentDescription = null,
            tint = colors.ink,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** 34dp 视觉胶囊包在 48dp 触控区内。 */
@Composable
private fun AddButton(onClick: () -> Unit) {
    val colors = MyTokenThemeTokens.colors
    Box(
        modifier = Modifier
            .size(width = 84.dp, height = MyTokenSize.touch)
            .clip(RoundedCornerShape(MyTokenRadius.pill))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "新增凭证" }
            .testTag("credential_add_button"),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(MyTokenRadius.pill))
                .background(colors.actionFill)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = colors.onAction,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "添加",
                style = MyTokenType.label.copy(fontWeight = FontWeight(650)),
                color = colors.onAction,
            )
        }
    }
}

@Composable
private fun SectionRow() {
    val colors = MyTokenThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "全部凭证",
            style = MyTokenType.sectionLabel,
            color = colors.muted,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "长按拖动排序",
            style = MyTokenType.micro.copy(fontWeight = FontWeight.Normal),
            color = colors.muted,
        )
    }
}

@Composable
private fun PrivacyFooter() {
    val colors = MyTokenThemeTokens.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(top = 22.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "凭证只保存在本机，不上传",
            style = MyTokenType.caption,
            color = colors.muted,
            modifier = Modifier.testTag("credential_privacy_footer"),
        )
    }
}

@Composable
private fun CredentialRowList(
    state: CredentialListUiState,
    drawerMode: Boolean,
    onToggleEnabled: (Credential) -> Unit,
    onMove: (Int, Int) -> Unit,
    onEditCredential: (UUID) -> Unit,
    onRequestDelete: (Credential) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val hapticView = LocalView.current
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        onMove(from.index, to.index)
        hapticView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = if (drawerMode) 24.dp else 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(
            state.items,
            key = { _, item -> item.id },
            contentType = { _, _ -> "credential-row" },
        ) { _, credential ->
            ReorderableItem(reorderableState, key = credential.id) { isDragging ->
                CredentialRow(
                    credential = credential,
                    providerName = ProviderNames.displayName(credential.providerId),
                    isDragging = isDragging,
                    dragHandleModifier = Modifier.longPressDraggableHandle(
                        onDragStarted = {
                            hapticView.performHapticFeedback(
                                HapticFeedbackConstants.KEYBOARD_TAP,
                            )
                        },
                        onDragStopped = {
                            hapticView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        },
                    ),
                    onToggleEnabled = { onToggleEnabled(credential) },
                    onEdit = { onEditCredential(credential.id) },
                    onRequestDelete = { onRequestDelete(credential) },
                )
            }
        }
    }
}

@Composable
private fun ExpandedCredentialGrid(
    state: CredentialListUiState,
    onToggleEnabled: (Credential) -> Unit,
    onMove: (Int, Int) -> Unit,
    onEditCredential: (UUID) -> Unit,
    onRequestDelete: (Credential) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyGridState()
    val hapticView = LocalView.current
    val reorderableState = rememberReorderableLazyGridState(listState) { from, to ->
        onMove(from.index, to.index)
        hapticView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
    MyTokenAdaptiveContent(
        maxWidth = 960.dp,
        horizontalPadding = 0.dp,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyVerticalGrid(
            state = listState,
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().testTag("credential_grid"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            gridItemsIndexed(state.items, key = { _, item -> item.id }) { _, credential ->
                ReorderableItem(reorderableState, key = credential.id) { isDragging ->
                    CredentialRow(
                        credential = credential,
                        providerName = ProviderNames.displayName(credential.providerId),
                        isDragging = isDragging,
                        dragHandleModifier = Modifier.longPressDraggableHandle(
                            onDragStarted = {
                                hapticView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            },
                            onDragStopped = {
                                hapticView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            },
                        ),
                        onToggleEnabled = { onToggleEnabled(credential) },
                        onEdit = { onEditCredential(credential.id) },
                        onRequestDelete = { onRequestDelete(credential) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CredentialRow(
    credential: Credential,
    providerName: String,
    isDragging: Boolean,
    dragHandleModifier: Modifier,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    var menuExpanded by remember { mutableStateOf(false) }
    MyTokenSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("credential_row_${credential.id}")
            .clickable(role = Role.Button, onClick = onEdit)
            .then(dragHandleModifier)
            .zIndex(if (isDragging) 1f else 0f)
            .then(
                if (isDragging) {
                    Modifier.graphicsLayer {
                        scaleX = 1.02f
                        scaleY = 1.02f
                        clip = false
                    }
                } else {
                    Modifier
                }
            ),
        surfaceRole = MyTokenVisualPolicy.SurfaceRole.Card,
        shape = RoundedCornerShape(MyTokenRadius.row),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MyTokenSize.listRow)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProviderLogoAvatar(
                providerId = credential.providerId,
                providerName = providerName,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = credential.name,
                        style = MyTokenType.listTitle,
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (!credential.isEnabled) {
                        DisabledChip(credential.id)
                    }
                }
                Text(
                    text = "$providerName · ${kindLabel(credential.credentialKind)}",
                    style = MyTokenType.caption,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            MyTokenSwitch(
                checked = credential.isEnabled,
                onCheckedChange = { onToggleEnabled() },
                contentDescription = "启用 ${credential.name}",
            )
            Box {
                Box(
                    modifier = Modifier
                        .size(MyTokenSize.touch)
                        .clickable { menuExpanded = true }
                        .testTag("credential_more_${credential.id}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "更多操作 ${credential.name}",
                        tint = colors.muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("编辑 ${credential.name}") },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "删除 ${credential.name}",
                                color = colors.criticalText,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onRequestDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DisabledChip(id: UUID) {
    val colors = MyTokenThemeTokens.colors
    Text(
        text = "已停用",
        style = MyTokenType.micro.copy(fontWeight = FontWeight(650), lineHeight = 13.sp),
        color = colors.muted,
        modifier = Modifier
            .testTag("credential_disabled_$id")
            .clip(RoundedCornerShape(5.dp))
            .background(colors.subtle)
            .border(1.dp, colors.line, RoundedCornerShape(5.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

internal fun kindLabel(kind: CredentialKind): String = when (kind) {
    CredentialKind.BearerApiKey -> "Bearer Token"
    CredentialKind.ApiKey -> "API Key"
    CredentialKind.AccessKeyPair -> "Access Key Pair"
}

@Composable
private fun ProviderLogoAvatar(
    providerId: ProviderId,
    providerName: String,
) {
    ProviderLogoPlate(
        painter = painterResource(providerLogoRes(providerId)),
        contentDescription = providerName,
        size = 36.dp,
        shape = RoundedCornerShape(10.dp),
        contentPadding = 4.dp,
    )
}

internal fun providerLogoRes(providerId: ProviderId): Int = when (providerId) {
    ProviderId.Routin -> R.drawable.share_provider_routin_logo
    ProviderId.DeepSeek -> R.drawable.share_provider_deepseek_logo
    ProviderId.Glm -> R.drawable.share_provider_glm_logo
    ProviderId.Volcengine -> R.drawable.share_provider_volcengine_logo
    ProviderId.NewAPI -> R.drawable.share_provider_newapi_logo
    ProviderId.CommandCode -> R.drawable.share_provider_commandcode_logo
    ProviderId.Xiaomi -> R.drawable.share_provider_xiaomi_logo
    ProviderId.OpenCode -> R.drawable.share_provider_opencode_logo
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    onImportFromMac: () -> Unit,
    onAddManually: () -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .testTag("credential_empty_mark"),
                contentAlignment = Alignment.Center,
            ) {
                ProviderLogoPlate(
                    painter = painterResource(ai.routin.mytoken.core.ui.R.drawable.mytoken_brand_logo),
                    contentDescription = null,
                    size = 54.dp,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = 0.dp,
                )
            }
            Text(
                text = "还没有凭证",
                modifier = Modifier.padding(top = 16.dp),
                style = MyTokenType.title,
                color = colors.ink,
            )
            Text(
                text = "从 Mac 扫码迁移，或手动录入一条密钥",
                modifier = Modifier.padding(top = 6.dp),
                style = MyTokenType.caption,
                color = colors.muted,
            )
            Column(
                modifier = Modifier.padding(top = 24.dp).width(220.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MyTokenButton(
                    onClick = onImportFromMac,
                    tone = MyTokenButtonTone.Primary,
                    text = "从 Mac 导入",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                MyTokenButton(
                    onClick = onAddManually,
                    text = "手动添加",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
