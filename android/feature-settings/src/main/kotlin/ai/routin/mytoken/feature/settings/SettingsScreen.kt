package ai.routin.mytoken.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import ai.routin.mytoken.core.ui.SectionCard
import ai.routin.mytoken.core.ui.MyTokenAdaptiveContent
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenButton
import ai.routin.mytoken.core.ui.MyTokenButtonTone
import ai.routin.mytoken.core.ui.MyTokenBrandMark
import ai.routin.mytoken.core.ui.MyTokenRadius
import ai.routin.mytoken.core.ui.MyTokenRow
import ai.routin.mytoken.core.ui.MyTokenSection
import ai.routin.mytoken.core.ui.MyTokenSize
import ai.routin.mytoken.core.ui.MyTokenSpacing
import ai.routin.mytoken.core.ui.MyTokenSurface
import ai.routin.mytoken.core.ui.MyTokenThemePreviewColors
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.core.ui.MyTokenDialog
import ai.routin.mytoken.core.ui.MyTokenDialogRow
import ai.routin.mytoken.core.ui.MyTokenDialogSection
import ai.routin.mytoken.core.ui.MyTokenDialogStatus
import ai.routin.mytoken.core.ui.MyTokenDialogTone
import ai.routin.mytoken.core.ui.MyTokenVisualPolicy
import ai.routin.mytoken.core.ui.myTokenFilterChipBorder
import ai.routin.mytoken.core.ui.myTokenFilterChipColors
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    appVersion: String,
    updateState: AppUpdateUiState = AppUpdateUiState.Idle,
    releaseHistoryState: AppReleaseHistoryUiState = AppReleaseHistoryUiState.Idle,
    onOpenAppRefreshChange: (Boolean) -> Unit,
    onRetryOnFailureChange: (Boolean) -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onOpenTransfer: () -> Unit,
    onCredentialFailureAlertsChange: (Boolean) -> Unit = {},
    notificationPermissionGranted: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onLoadReleaseHistory: () -> Unit = {},
    onLoadCachedReleaseHistory: () -> Unit = {},
    onDownloadAndInstall: (String, String) -> Unit = { _, _ -> },
    onOpenInstallPermissionSettings: () -> Unit = {},
    onInstallDownloadedUpdate: () -> Unit = {},
    onMirrorBaseChange: (String) -> Unit = {},
    onGitHubTokenSave: (String) -> Unit = {},
    onGitHubTokenRemove: () -> Unit = {},
    logExportState: LogExportUiState = LogExportUiState.Idle,
    onExportLogs: () -> Unit = {},
    onShareLogs: () -> Unit = {},
    onDismissLogExport: () -> Unit = {},
    layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact,
    drawerMode: Boolean = false,
    onClose: (() -> Unit)? = null,
) {
    var showReleaseHistory by remember { mutableStateOf(false) }
    var releaseNotesExpanded by rememberSaveable { mutableStateOf(false) }
    var updateAdvancedExpanded by rememberSaveable { mutableStateOf(false) }
    LogExportDialog(
        state = logExportState,
        onShare = onShareLogs,
        onDismiss = onDismissLogExport,
    )
    val uriHandler = LocalUriHandler.current
    val currentRelease = (releaseHistoryState as? AppReleaseHistoryUiState.Loaded)
        ?.releases
        ?.firstOrNull { it.version == appVersion }

    // 打开设置页只读本地缓存；用户点击“获取”后才请求网络并回写缓存。
    LaunchedEffect(Unit) {
        onLoadCachedReleaseHistory()
    }

    Scaffold(
        containerColor = if (drawerMode) {
            androidx.compose.ui.graphics.Color.Transparent
        } else {
            MaterialTheme.colorScheme.background
        },
    ) { padding ->
        val columns = if (drawerMode || layoutMode == MyTokenLayoutMode.Compact) 1 else 2
        MyTokenAdaptiveContent(
            maxWidth = 1200.dp,
            horizontalPadding = 16.dp,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxSize()
                .then(if (drawerMode) Modifier.navigationBarsPadding() else Modifier)
                .testTag(if (drawerMode) "settings_drawer_content" else "settings_list"),
            contentPadding = PaddingValues(
                top = 12.dp,
                bottom = if (drawerMode) 24.dp else 96.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(MyTokenSpacing.settingsSection),
        ) {
            item(
                key = "settings_header",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                // 设计稿：页头与资料卡只隔 6dp，不占一个完整分组间距。
                Column {
                    SettingsHeader(drawerMode = drawerMode, onClose = onClose)
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingsProfileCard(appVersion = appVersion)
                }
            }
            item(key = "theme") {
                SectionCard(title = "外观", modifier = Modifier.testTag("settings_theme_cell")) {
                    ThemeSelector(
                        selected = state.display.themeMode,
                        onSelect = onThemeModeChange,
                    )
                }
            }
            item(key = "refresh") {
                Box(modifier = Modifier.testTag("settings_refresh_cell")) {
                    RefreshSettingsSection(
                        settings = state.refresh,
                        onOpenAppRefreshChange = onOpenAppRefreshChange,
                        onRetryOnFailureChange = onRetryOnFailureChange,
                    )
                }
            }
            item(key = "notification") {
                NotificationSettingsSection(
                    settings = state.notifications,
                    permissionGranted = notificationPermissionGranted,
                    onRequestPermission = onRequestNotificationPermission,
                    onCredentialFailureAlertsChange = onCredentialFailureAlertsChange,
                )
            }
            item(key = "data") {
                MyTokenSection(
                    title = "数据",
                    modifier = Modifier.testTag("settings_migration_cell"),
                ) {
                    MyTokenRow(
                        icon = Icons.Filled.QrCodeScanner,
                        title = "从 Mac 导入",
                        supportingText = "扫码接收 Mac 凭证",
                        iconPlate = true,
                        onClick = onOpenTransfer,
                        modifier = Modifier.testTag("settings_import_from_mac"),
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MyTokenThemeTokens.colors.muted,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                    MyTokenRow(
                        icon = Icons.Filled.Download,
                        title = "导出诊断日志",
                        supportingText = "生成已脱敏的本机日志",
                        iconPlate = true,
                        onClick = onExportLogs,
                        modifier = Modifier.testTag("settings_export_logs"),
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MyTokenThemeTokens.colors.muted,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }
            item(
                key = "about",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                MyTokenSection(
                    title = "关于",
                    modifier = Modifier.testTag("settings_about_span"),
                ) {
                    MyTokenRow(
                        icon = null,
                        title = "版本",
                        value = appVersion,
                        // 长按版本号导出诊断日志：真机不方便连 adb 时也能把日志发给开发者。
                        modifier = Modifier
                            .testTag("settings_about_version")
                            .pointerInput(Unit) {
                                detectTapGestures(onLongPress = { onExportLogs() })
                            },
                    )
                    MyTokenRow(
                        icon = null,
                        title = "检查更新",
                        value = updateRowStatus(updateState),
                        onClick = {
                            if (updateState.canCheckManually) onCheckForUpdates()
                        },
                        modifier = Modifier.testTag("settings_check_updates"),
                        trailing = { AboutChevron(expanded = false) },
                    )
                    if (updateState.showsUpdateActions) {
                        UpdateSection(
                            state = updateState,
                            currentVersion = appVersion,
                            onCheckForUpdates = onCheckForUpdates,
                            onDownloadAndInstall = onDownloadAndInstall,
                            onOpenInstallPermissionSettings = onOpenInstallPermissionSettings,
                            onInstallDownloadedUpdate = onInstallDownloadedUpdate,
                        )
                    }
                    AboutDisclosureRow(
                        title = "更新日志",
                        expanded = releaseNotesExpanded,
                        onToggle = { releaseNotesExpanded = !releaseNotesExpanded },
                        toggleTag = "settings_release_notes_toggle",
                    ) {
                        CurrentReleaseNotesSection(
                            state = releaseHistoryState,
                            currentVersion = appVersion,
                            onRetry = onLoadReleaseHistory,
                            onShowHistory = { showReleaseHistory = true },
                        )
                        Text(
                            text = "在 GitHub 查看",
                            modifier = Modifier
                                .clip(RoundedCornerShape(MyTokenRadius.pill))
                                .clickable(role = Role.Button) {
                                    uriHandler.openUri(
                                        currentRelease?.releaseUrl
                                            ?: "https://github.com/dick86114/MyToken/releases",
                                    )
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("about_github_link"),
                            style = MyTokenType.label,
                            color = MyTokenThemeTokens.colors.actionText,
                        )
                    }
                    AboutDisclosureRow(
                        title = "更新源与令牌",
                        supportingText = if (state.update.mirrorBase.isBlank()) "GitHub" else "CDN",
                        expanded = updateAdvancedExpanded,
                        onToggle = { updateAdvancedExpanded = !updateAdvancedExpanded },
                        toggleTag = "settings_update_advanced_toggle",
                    ) {
                        UpdateChannelSection(
                            mirrorBase = state.update.mirrorBase,
                            onMirrorBaseChange = onMirrorBaseChange,
                        )
                        GitHubTokenSection(
                            tokenMask = state.update.githubTokenMask,
                            error = state.update.githubTokenError,
                            onSave = onGitHubTokenSave,
                            onRemove = onGitHubTokenRemove,
                        )
                    }
                }
            }
        }
        }
    }

    if (showReleaseHistory) {
        ReleaseHistoryDialog(
            state = releaseHistoryState,
            onDismiss = { showReleaseHistory = false },
            onRetry = onLoadReleaseHistory,
        )
    }
}

@Composable
private fun CurrentReleaseNotesSection(
    state: AppReleaseHistoryUiState,
    currentVersion: String,
    onRetry: () -> Unit,
    onShowHistory: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onRetry,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("release_notes_refresh_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "获取当前版本日志",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onShowHistory,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("release_history_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = "查看历史版本",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        when (state) {
            AppReleaseHistoryUiState.Idle -> Text(
                text = "暂无更新日志",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppReleaseHistoryUiState.Loading -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        text = "正在加载...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            is AppReleaseHistoryUiState.Error -> Text(
                text = "日志获取失败",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            is AppReleaseHistoryUiState.Loaded -> {
                val current = state.releases.firstOrNull { it.version == currentVersion }
                if (current == null || current.releaseNotes.isBlank()) {
                    Text(
                        text = "暂无更新日志",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    MarkdownText(markdown = current.releaseNotes)
                }
            }
        }
    }
}

/** 「关于」长按导出的反馈：成功给出位置与分享入口，失败说明原因。 */
@Composable
private fun LogExportDialog(
    state: LogExportUiState,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (state is LogExportUiState.Idle) return
    val title = when (state) {
        is LogExportUiState.Working -> "正在导出日志"
        is LogExportUiState.Done -> "日志已导出"
        is LogExportUiState.Failed -> "导出失败"
        LogExportUiState.Idle -> ""
    }
    val tone = when (state) {
        is LogExportUiState.Working -> MyTokenDialogTone.Info
        is LogExportUiState.Done -> MyTokenDialogTone.Success
        is LogExportUiState.Failed -> MyTokenDialogTone.Danger
        LogExportUiState.Idle -> MyTokenDialogTone.Info
    }
    val actions: (@Composable RowScope.() -> Unit)? = when (state) {
        is LogExportUiState.Done -> {
            {
                MyTokenButton(
                    text = "关闭",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    tone = MyTokenButtonTone.Neutral,
                )
                MyTokenButton(
                    text = "分享",
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    tone = MyTokenButtonTone.Primary,
                )
            }
        }
        is LogExportUiState.Failed -> {
            {
                MyTokenButton(
                    text = "关闭",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    tone = MyTokenButtonTone.Neutral,
                )
            }
        }
        else -> null
    }
    MyTokenDialog(
        title = title,
        subtitle = when (state) {
            is LogExportUiState.Done -> "文件已保存在本机，可直接分享"
            is LogExportUiState.Failed -> "日志没有生成"
            else -> "正在收集必要信息"
        },
        icon = Icons.Filled.Code,
        tone = tone,
        onDismissRequest = onDismiss,
        content = {
            when (state) {
                LogExportUiState.Idle -> Unit
                LogExportUiState.Working -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Column {
                        Text(
                            text = "正在收集",
                            style = MyTokenType.body,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "应用日志、设备信息与最近记录",
                            style = MyTokenType.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                is LogExportUiState.Done -> {
                    MyTokenDialogSection(title = "导出结果") {
                        MyTokenDialogRow(
                            label = "文件位置",
                            detail = state.savedLocation,
                        )
                        MyTokenDialogRow(
                            label = "文件大小",
                            detail = String.format("%.1f KB", state.sizeBytes / 1024.0),
                        )
                    }
                    MyTokenDialogStatus(
                        message = "把日志用任意方式发给开发者即可。",
                        tone = MyTokenDialogTone.Success,
                    )
                }
                is LogExportUiState.Failed -> MyTokenDialogStatus(
                    message = state.message,
                    tone = MyTokenDialogTone.Danger,
                )
            }
        },
        actions = actions,
    )
}

@Composable
private fun ReleaseHistoryDialog(
    state: AppReleaseHistoryUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    val isError = state is AppReleaseHistoryUiState.Error
    val actions: @Composable RowScope.() -> Unit = {
        MyTokenButton(
            text = "关闭",
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            tone = MyTokenButtonTone.Neutral,
        )
        MyTokenButton(
            text = if (isError) "重试" else "完成",
            onClick = if (isError) onRetry else onDismiss,
            modifier = Modifier.weight(1f),
            tone = if (isError) MyTokenButtonTone.Primary else MyTokenButtonTone.Neutral,
        )
    }
    MyTokenDialog(
        title = "历史版本",
        subtitle = "Android 发布记录",
        icon = Icons.Filled.History,
        tone = if (isError) MyTokenDialogTone.Danger else MyTokenDialogTone.Info,
        onDismissRequest = onDismiss,
        content = {
            when (state) {
                AppReleaseHistoryUiState.Idle,
                AppReleaseHistoryUiState.Loading -> Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        text = "正在加载发布记录",
                        style = MyTokenType.label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is AppReleaseHistoryUiState.Error -> MyTokenDialogStatus(
                    message = "日志获取失败，请检查网络后重试。",
                    tone = MyTokenDialogTone.Danger,
                )
                is AppReleaseHistoryUiState.Loaded -> {
                    if (state.releases.isEmpty()) {
                        MyTokenDialogStatus("暂无更新日志")
                    } else {
                        ReleaseHistoryList(releases = state.releases)
                    }
                }
            }
        },
        actions = actions,
    )
}

@Composable
private fun ReleaseHistoryList(releases: List<AppReleaseHistoryItem>) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(releases, key = { it.version }) { release ->
            MyTokenDialogSection {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "v${release.version}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        release.publishedAt?.take(10)?.let { date ->
                            Text(
                                text = date,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (release.releaseNotes.isBlank()) {
                            Text(
                                text = "暂无更新日志",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            MarkdownText(
                                markdown = release.releaseNotes,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        IconButton(
                            onClick = { uriHandler.openUri(release.releaseUrl) },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "在 GitHub 查看",
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateSection(
    state: AppUpdateUiState,
    currentVersion: String,
    onCheckForUpdates: () -> Unit,
    onDownloadAndInstall: (String, String) -> Unit,
    onOpenInstallPermissionSettings: () -> Unit,
    onInstallDownloadedUpdate: () -> Unit,
) {
    when (state) {
        is AppUpdateUiState.Checking -> {
            Text(
                text = "正在检查更新",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MyTokenButton(
                onClick = onCheckForUpdates,
                enabled = false,
                tone = MyTokenButtonTone.Primary,
                text = "检测更新",
                icon = Icons.Filled.Refresh,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("check_updates_button"),
            )
        }

        is AppUpdateUiState.Downloading -> {
            val progress = state.progress
            Text(
                text = if (progress == null) "正在下载更新..." else "正在下载更新 ${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { ((progress ?: 0f).coerceIn(0f, 1f)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is AppUpdateUiState.Available -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(10.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    val updateAccent = MyTokenThemeTokens.colors.actionText
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(MyTokenThemeTokens.colors.actionFill, RoundedCornerShape(MyTokenRadius.pill)),
                        )
                        Text(
                            text = "发现新版本",
                            modifier = Modifier.padding(start = 5.dp),
                            style = MyTokenType.label,
                            color = updateAccent,
                        )
                    }
                    Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "v${state.version}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "v$currentVersion",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
            if (state.releaseNotes.isNotBlank()) {
                MarkdownText(
                    markdown = state.releaseNotes,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            MyTokenButton(
                onClick = { onDownloadAndInstall(state.version, state.downloadUrl) },
                modifier = Modifier.fillMaxWidth().testTag("download_update_button"),
                tone = MyTokenButtonTone.Primary,
                text = "下载并安装",
                icon = Icons.Filled.Download,
            )
        }

        is AppUpdateUiState.NeedsInstallPermission -> {
            var permissionRequestPending by rememberSaveable { mutableStateOf(false) }
            LifecycleResumeEffect(Unit) {
                if (permissionRequestPending) {
                    permissionRequestPending = false
                    onInstallDownloadedUpdate()
                }
                onPauseOrDispose {}
            }
            Text(
                text = "需要安装权限",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MyTokenButton(
                onClick = {
                    permissionRequestPending = true
                    onOpenInstallPermissionSettings()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("install_permission_button"),
                tone = MyTokenButtonTone.Primary,
                text = "授权并安装",
                icon = Icons.AutoMirrored.Filled.OpenInNew,
            )
        }

        is AppUpdateUiState.ReadyToInstall -> {
            Text(
                text = "v${state.version} 已下载完成。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MyTokenButton(
                onClick = onInstallDownloadedUpdate,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("install_update_button"),
                tone = MyTokenButtonTone.Primary,
                text = "安装更新",
                icon = Icons.Filled.ArrowUpward,
            )
        }

        is AppUpdateUiState.Error -> {
            Text(
                text = "更新失败，请重试",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            MyTokenButton(
                onClick = onCheckForUpdates,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("check_updates_button"),
                tone = MyTokenButtonTone.Primary,
                text = "重试",
                icon = Icons.Filled.Refresh,
            )
        }

        is AppUpdateUiState.UpToDate -> {
            Text(
                text = "已是最新版本",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MyTokenButton(
                onClick = onCheckForUpdates,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("check_updates_button"),
                tone = MyTokenButtonTone.Primary,
                text = "检测更新",
                icon = Icons.Filled.Refresh,
            )
        }

        AppUpdateUiState.Idle -> MyTokenButton(
            onClick = onCheckForUpdates,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("check_updates_button"),
            tone = MyTokenButtonTone.Primary,
            text = "检测更新",
            icon = Icons.Filled.Refresh,
        )
    }
}

/** 设计稿“关于”区：版本 / 检查更新 / 更新日志 / 更新源与令牌，四行常显。 */
private fun updateRowStatus(state: AppUpdateUiState): String = when (state) {
    AppUpdateUiState.Idle -> "未检查"
    AppUpdateUiState.Checking -> "检查中…"
    is AppUpdateUiState.UpToDate -> "已是最新"
    is AppUpdateUiState.Available -> "发现 v${state.version}"
    is AppUpdateUiState.Downloading -> "下载中"
    is AppUpdateUiState.ReadyToInstall -> "待安装"
    is AppUpdateUiState.NeedsInstallPermission -> "待授权"
    is AppUpdateUiState.Error -> "检查失败"
}

private val AppUpdateUiState.canCheckManually: Boolean
    get() = this is AppUpdateUiState.Idle ||
        this is AppUpdateUiState.UpToDate ||
        this is AppUpdateUiState.Error

private val AppUpdateUiState.showsUpdateActions: Boolean
    get() = this is AppUpdateUiState.Available ||
        this is AppUpdateUiState.Downloading ||
        this is AppUpdateUiState.ReadyToInstall ||
        this is AppUpdateUiState.NeedsInstallPermission

@Composable
private fun AboutChevron(expanded: Boolean) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MyTokenThemeTokens.colors.muted,
        modifier = Modifier
            .size(18.dp)
            .rotate(if (expanded) 90f else 0f),
    )
}

@Composable
private fun AboutDisclosureRow(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    toggleTag: String,
    supportingText: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MyTokenSize.settingsRowMin)
                .testTag(toggleTag)
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics {
                    stateDescription = if (expanded) "已展开" else "已收起"
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MyTokenType.body,
                    color = colors.ink,
                    maxLines = 1,
                )
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MyTokenType.caption,
                        color = colors.muted,
                        maxLines = 1,
                    )
                }
            }
            AboutChevron(expanded = expanded)
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun SettingsHeader(drawerMode: Boolean, onClose: (() -> Unit)?) {
    val colors = MyTokenThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MyTokenSize.touch)
            .testTag("settings_header"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "设置",
            style = MyTokenType.section,
            color = colors.ink,
            modifier = Modifier
                .weight(1f)
                .testTag("settings_title"),
        )
        if (drawerMode && onClose != null) {
            Spacer(modifier = Modifier.size(2.dp))
            Box(
                modifier = Modifier
                    .size(MyTokenSize.header)
                    .clip(RoundedCornerShape(MyTokenRadius.pill))
                    .clickable(onClick = onClose)
                    .semantics { contentDescription = "关闭设置" }
                    .testTag("settings_close"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = colors.ink,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsProfileCard(appVersion: String) {
    val colors = MyTokenThemeTokens.colors
    MyTokenSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_profile"),
        surfaceRole = MyTokenVisualPolicy.SurfaceRole.Card,
        shape = RoundedCornerShape(MyTokenRadius.card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MyTokenBrandMark(
                modifier = Modifier.testTag("settings_brand_logo"),
                size = 44.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MyToken",
                    style = MyTokenType.profileName,
                    color = colors.ink,
                )
                Text(
                    text = "v$appVersion · 凭证只在本机",
                    modifier = Modifier.padding(top = 2.dp),
                    style = MyTokenType.caption,
                    color = colors.muted,
                )
            }
        }
    }
}

@Composable
private fun ThemeSelector(
    selected: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    val options = listOf(
        "跟随系统" to AppThemeMode.SYSTEM,
        "浅色" to AppThemeMode.LIGHT,
        "深色" to AppThemeMode.DARK,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        options.forEach { (label, mode) ->
            val isSelected = mode == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .testTag("theme_option_${mode.name}")
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(mode) },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ThemePreviewScene(
                    mode = mode,
                    selected = isSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("theme_scene_${mode.name}"),
                )
                Text(
                    text = label,
                    modifier = Modifier.padding(top = 7.dp),
                    style = MyTokenType.caption,
                    fontWeight = if (isSelected) FontWeight(650) else FontWeight(550),
                    color = if (isSelected) colors.ink else colors.muted,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** 三张主题预览卡：固定展示浅色/深色场景，选中时用墨色描边。 */
@Composable
private fun ThemePreviewScene(
    mode: AppThemeMode,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    val preview = MyTokenThemePreviewColors
    val shape = RoundedCornerShape(10.dp)
    val isSystem = mode == AppThemeMode.SYSTEM
    val isDark = mode == AppThemeMode.DARK
    val baseColor = when (mode) {
        AppThemeMode.DARK -> preview.darkScene
        else -> preview.lightScene
    }
    val lineColor = when {
        isSystem -> preview.systemLine
        isDark -> preview.darkLine
        else -> preview.lightLine
    }
    val cardColor = when {
        isSystem -> preview.systemCard
        isDark -> preview.darkCard
        else -> preview.lightCard
    }
    val borderColor = when {
        selected -> colors.ink
        isDark -> preview.darkBorder
        else -> preview.sceneBorder
    }
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(shape)
            .background(baseColor)
            .border(if (selected) 2.dp else 1.dp, borderColor, shape),
    ) {
        if (isSystem) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(preview.lightScene),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(preview.darkScene),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 9.dp)
                .fillMaxWidth(0.36f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(lineColor),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, end = 8.dp, top = 21.dp)
                .fillMaxWidth()
                .height(11.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(cardColor),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, end = 8.dp, top = 36.dp)
                .fillMaxWidth()
                .height(11.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(cardColor),
        )
    }
}

@Composable
private fun GitHubTokenSection(
    tokenMask: String?,
    error: String?,
    onSave: (String) -> Unit,
    onRemove: () -> Unit,
) {
    var tokenInput by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "GitHub 令牌",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "检查更新时使用。带令牌每小时 5000 次请求，不带仅 60 次；公开仓库只读权限即可。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (tokenMask == null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("github_token_input"),
                    singleLine = true,
                    placeholder = { Text("ghp_ 或 github_pat_ 令牌") },
                    visualTransformation = PasswordVisualTransformation(),
                )
                MyTokenButton(
                    onClick = {
                        onSave(tokenInput)
                        tokenInput = ""
                    },
                    enabled = tokenInput.isNotBlank(),
                    tone = MyTokenButtonTone.Primary,
                    text = "保存",
                    modifier = Modifier.testTag("github_token_save_button"),
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = tokenMask,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("github_token_mask"),
                )
                TextButton(
                    onClick = onRemove,
                    modifier = Modifier.testTag("github_token_remove_button"),
                ) {
                    Text("移除")
                }
            }
        }

        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UpdateChannelSection(
    mirrorBase: String,
    onMirrorBaseChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "更新源",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("update_channel_selector"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = mirrorBase.isEmpty(),
                onClick = { onMirrorBaseChange("") },
                label = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Code,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Text("GitHub")
                    }
                },
                colors = myTokenFilterChipColors(selected = mirrorBase.isEmpty()),
                border = myTokenFilterChipBorder(selected = mirrorBase.isEmpty()),
            )
            FilterChip(
                selected = mirrorBase.isNotEmpty(),
                onClick = {
                    if (mirrorBase.isEmpty()) {
                        onMirrorBaseChange(DEFAULT_UPDATE_CDN_BASES.first())
                    }
                },
                label = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Cloud,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Text("CDN")
                    }
                },
                colors = myTokenFilterChipColors(selected = mirrorBase.isNotEmpty()),
                border = myTokenFilterChipBorder(selected = mirrorBase.isNotEmpty()),
            )
        }
        if (mirrorBase.isNotEmpty()) {
            Text(
                text = "镜像源",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DEFAULT_UPDATE_CDN_BASES.forEach { base ->
                    FilterChip(
                        selected = mirrorBase == base,
                        onClick = { onMirrorBaseChange(base) },
                        label = { Text(text = base.removePrefix("https://")) },
                        colors = myTokenFilterChipColors(selected = mirrorBase == base),
                        border = myTokenFilterChipBorder(selected = mirrorBase == base),
                    )
                }
            }
        }
    }
}

private val DEFAULT_UPDATE_CDN_BASES = listOf(
    "https://ghfast.top",
    "https://gh-proxy.com",
    "https://ghproxy.net",
)

@Composable
internal fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    SectionCard(title = title) { content() }
}
