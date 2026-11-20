package ai.routin.mytoken.feature.credentials

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.core.ui.MyTokenSurface
import ai.routin.mytoken.core.ui.MyTokenAdaptiveContent
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenPalette
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.core.ui.ProviderLogoPlate
import androidx.compose.foundation.ExperimentalFoundationApi
import ai.routin.mytoken.core.ui.MyTokenVisualPolicy
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import ai.routin.mytoken.core.ui.MyTokenButton
import ai.routin.mytoken.core.ui.MyTokenButtonTone
import ai.routin.mytoken.core.ui.MyTokenHelpButton
import ai.routin.mytoken.core.ui.HelpRow
import ai.routin.mytoken.core.ui.HelpSection
import ai.routin.mytoken.core.ui.MyTokenSwitch
import ai.routin.mytoken.core.ui.MyTokenDialog
import ai.routin.mytoken.core.ui.MyTokenDialogRow
import ai.routin.mytoken.core.ui.MyTokenDialogSection
import ai.routin.mytoken.core.ui.MyTokenDialogStatus
import ai.routin.mytoken.core.ui.MyTokenDialogTone
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyGridState

@OptIn(ExperimentalMaterial3Api::class)
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
    Scaffold(
        containerColor = if (drawerMode) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.background
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(bottom = if (drawerMode) 0.dp else 62.dp)
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
            if (layoutMode == MyTokenLayoutMode.Expanded) {
                ExpandedCredentialGrid(
                    state = state,
                    padding = PaddingValues(),
                    onToggleEnabled = onToggleEnabled,
                    onMove = onMove,
                    onEditCredential = onEditCredential,
                    onRequestDelete = onRequestDelete,
                    modifier = Modifier.weight(1f),
                )
            } else {
                val listState = rememberLazyListState()
                val hapticView = LocalView.current
                val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
                    onMove(from.index, to.index)
                    hapticView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = if (drawerMode) 24.dp else 92.dp,
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
    drawerMode: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "凭证",
                    style = MyTokenType.pageTitle,
                )
                if (drawerMode) {
                    MyTokenHelpButton(
                        title = "凭证列表",
                        subtitle = "列表与首页共用同一组凭证",
                        sections = listOf(
                            HelpSection(
                                title = "操作",
                                rows = listOf(
                                    HelpRow("轻点凭证", "进入编辑"),
                                    HelpRow("长按拖动", "调整显示顺序"),
                                    HelpRow("右侧开关", "控制首页是否展示"),
                                ),
                            ),
                        ),
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .testTag("credential_help"),
                    )
                }
            }
            if (totalCount > 0) {
                Text(
                    text = "已启用 $enabledCount / $totalCount",
                    style = MyTokenType.metricLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .testTag("credential_enabled_summary"),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!drawerMode) {
                HeaderCircleButton(
                    contentDescription = "从 Mac 导入",
                    onClick = onImportFromMac,
                ) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
            HeaderCircleButton(
                contentDescription = "新增凭证",
                onClick = onAddManually,
                primary = true,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun HeaderCircleButton(
    contentDescription: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    content: @Composable () -> Unit,
) {
    val shape = if (primary) CircleShape else RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(if (primary) 40.dp else 34.dp)
            .clip(shape)
            .background(
                if (primary) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
                },
            )
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
            .testTag(if (primary) "credential_add_button" else "credential_import_button"),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides if (primary) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpandedCredentialGrid(
    state: CredentialListUiState,
    padding: PaddingValues,
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
        modifier = modifier.padding(padding).fillMaxSize(),
    ) {
        LazyVerticalGrid(
            state = listState,
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().testTag("credential_grid"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
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
    var menuExpanded by remember { mutableStateOf(false) }
    MyTokenSurface(
        modifier = Modifier
            .fillMaxWidth()
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
        shape = RoundedCornerShape(MyTokenVisualPolicy.outerRadiusDp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp),
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
            ) {
                Text(
                    text = credential.name,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
            MyTokenSwitch(
                checked = credential.isEnabled,
                onCheckedChange = { onToggleEnabled() },
                contentDescription = "启用 ${credential.name}",
            )
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("credential_more_${credential.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "更多操作 ${credential.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp),
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
                                color = MaterialTheme.colorScheme.error,
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
private fun ProviderLogoAvatar(
    providerId: ProviderId,
    providerName: String,
) {
    ProviderLogoPlate(
        painter = painterResource(providerLogoRes(providerId)),
        contentDescription = providerName,
        size = 30.dp,
        shape = RoundedCornerShape(9.dp),
        contentPadding = 3.dp,
    )
}

private fun providerLogoRes(providerId: ProviderId): Int = when (providerId) {
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
    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "暂无凭证", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(24.dp))
        MyTokenButton(
            onClick = onImportFromMac,
            tone = MyTokenButtonTone.Primary,
            text = "从 Mac 导入",
        )
        Spacer(modifier = Modifier.height(8.dp))
        MyTokenButton(
            onClick = onAddManually,
            text = "添加凭证",
        )
        }
    }
}
