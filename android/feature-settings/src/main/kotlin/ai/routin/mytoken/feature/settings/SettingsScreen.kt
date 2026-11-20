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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Surface
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
import androidx.compose.material3.HorizontalDivider
import ai.routin.mytoken.core.ui.SectionCard
import ai.routin.mytoken.core.ui.MyTokenAdaptiveContent
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.GlassButton
import ai.routin.mytoken.core.ui.GlassButtonTone
import ai.routin.mytoken.core.ui.GlassHelpButton
import ai.routin.mytoken.core.ui.HelpRow
import ai.routin.mytoken.core.ui.HelpSection
import ai.routin.mytoken.core.ui.MyTokenBrandMark
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.core.ui.MyTokenDialog
import ai.routin.mytoken.core.ui.MyTokenDialogRow
import ai.routin.mytoken.core.ui.MyTokenDialogSection
import ai.routin.mytoken.core.ui.MyTokenDialogStatus
import ai.routin.mytoken.core.ui.MyTokenDialogTone
import ai.routin.mytoken.core.ui.MyTokenPalette
import ai.routin.mytoken.core.ui.MyTokenVisualPolicy
import ai.routin.mytoken.core.ui.glassFilterChipBorder
import ai.routin.mytoken.core.ui.glassFilterChipColors
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
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
) {
    var showReleaseHistory by remember { mutableStateOf(false) }
    var migrationExpanded by rememberSaveable { mutableStateOf(false) }
    var aboutExpanded by rememberSaveable { mutableStateOf(false) }
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
    val aboutSummary = aboutSectionSummary(appVersion, updateState)
    val updateAdvancedSummary = updateAdvancedSectionSummary(state.update)

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
                .testTag(if (drawerMode) "settings_drawer_content" else "settings_list"),
            contentPadding = PaddingValues(
                top = 12.dp,
                bottom = if (drawerMode) 24.dp else 96.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(
                key = "settings_header",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                SettingsHeader()
            }
            item(
                key = "settings_profile",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                SettingsProfileCard(appVersion = appVersion)
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
            item(key = "migration") {
                ExpandableSettingsSection(
                    title = "数据迁移",
                    summary = "扫码导入",
                    expanded = migrationExpanded,
                    onExpandedChange = { migrationExpanded = it },
                    toggleTag = "settings_migration_toggle",
                    modifier = Modifier.testTag("settings_migration_cell"),
                ) {
                    GlassButton(
                        onClick = onOpenTransfer,
                        tone = GlassButtonTone.Primary,
                        text = "开始扫描",
                    )
                }
            }
            item(
                key = "about",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                ExpandableSettingsSection(
                    title = "关于与更新",
                    summary = aboutSummary,
                    expanded = aboutExpanded,
                    onExpandedChange = { aboutExpanded = it },
                    toggleTag = "settings_about_toggle",
                    modifier = Modifier.testTag("settings_about_span"),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                ) {
                                    append("MyToken")
                                }
                                withStyle(
                                    SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                ) {
                                    append(" $appVersion")
                                }
                            },
                            style = MaterialTheme.typography.titleMedium,
                            // 长按版本号导出诊断日志：真机不方便连 adb 时也能把日志发给开发者。
                            modifier = Modifier
                                .testTag("settings_about_version")
                                .pointerInput(Unit) {
                                    detectTapGestures(onLongPress = { onExportLogs() })
                                },
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = {
                                uriHandler.openUri(
                                    currentRelease?.releaseUrl
                                        ?: "https://github.com/dick86114/MyToken/releases",
                                )
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("github_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "打开 GitHub",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    UpdateSection(
                        state = updateState,
                        currentVersion = appVersion,
                        onCheckForUpdates = onCheckForUpdates,
                        onDownloadAndInstall = onDownloadAndInstall,
                        onOpenInstallPermissionSettings = onOpenInstallPermissionSettings,
                        onInstallDownloadedUpdate = onInstallDownloadedUpdate,
                    )
                    SettingsDisclosure(
                        title = "更新日志",
                        summary = "当前版本说明与历史版本",
                        expanded = releaseNotesExpanded,
                        onExpandedChange = { releaseNotesExpanded = it },
                        toggleTag = "settings_release_notes_toggle",
                    ) {
                        CurrentReleaseNotesSection(
                            state = releaseHistoryState,
                            currentVersion = appVersion,
                            onRetry = onLoadReleaseHistory,
                            onShowHistory = { showReleaseHistory = true },
                        )
                    }
                    SettingsDisclosure(
                        title = "更新源与令牌",
                        summary = updateAdvancedSummary,
                        expanded = updateAdvancedExpanded,
                        onExpandedChange = { updateAdvancedExpanded = it },
                        toggleTag = "settings_update_advanced_toggle",
                    ) {
                        UpdateChannelSection(
                            mirrorBase = state.update.mirrorBase,
                            onMirrorBaseChange = onMirrorBaseChange,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
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
                GlassButton(
                    text = "关闭",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    tone = GlassButtonTone.Neutral,
                )
                GlassButton(
                    text = "分享",
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    tone = GlassButtonTone.Primary,
                )
            }
        }
        is LogExportUiState.Failed -> {
            {
                GlassButton(
                    text = "关闭",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    tone = GlassButtonTone.Neutral,
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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "应用日志、设备信息与最近记录",
                            fontSize = 11.sp,
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
        GlassButton(
            text = "关闭",
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            tone = GlassButtonTone.Neutral,
        )
        GlassButton(
            text = if (isError) "重试" else "完成",
            onClick = if (isError) onRetry else onDismiss,
            modifier = Modifier.weight(1f),
            tone = if (isError) GlassButtonTone.Primary else GlassButtonTone.Neutral,
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
                        fontSize = 12.sp,
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
            GlassButton(
                onClick = onCheckForUpdates,
                enabled = false,
                tone = GlassButtonTone.Primary,
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
                    val updateAccent = MaterialTheme.colorScheme.primary
                    Text(
                        text = "发现新版本",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = updateAccent,
                        modifier = Modifier
                            .background(updateAccent.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                            .border(1.dp, updateAccent.copy(alpha = 0.30f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
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
            GlassButton(
                onClick = { onDownloadAndInstall(state.version, state.downloadUrl) },
                modifier = Modifier.fillMaxWidth().testTag("download_update_button"),
                tone = GlassButtonTone.Primary,
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
            GlassButton(
                onClick = {
                    permissionRequestPending = true
                    onOpenInstallPermissionSettings()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("install_permission_button"),
                tone = GlassButtonTone.Primary,
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
            GlassButton(
                onClick = onInstallDownloadedUpdate,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("install_update_button"),
                tone = GlassButtonTone.Primary,
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
            GlassButton(
                onClick = onCheckForUpdates,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("check_updates_button"),
                tone = GlassButtonTone.Primary,
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
            GlassButton(
                onClick = onCheckForUpdates,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("check_updates_button"),
                tone = GlassButtonTone.Primary,
                text = "检测更新",
                icon = Icons.Filled.Refresh,
            )
        }

        AppUpdateUiState.Idle -> GlassButton(
            onClick = onCheckForUpdates,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("check_updates_button"),
            tone = GlassButtonTone.Primary,
            text = "检测更新",
            icon = Icons.Filled.Refresh,
        )
    }
}

@Composable
private fun SettingsHeader() {
    Text(
        text = "设置",
        style = MyTokenType.pageTitle,
    )
}

@Composable
private fun SettingsProfileCard(appVersion: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MyTokenVisualPolicy.outerRadiusDp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MyTokenBrandMark(size = 36.dp)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        ) {
            Text(
                text = "MyToken $appVersion",
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        GlassHelpButton(
            title = "隐私与刷新",
            subtitle = "凭证只在本机处理",
            sections = listOf(
                HelpSection(
                    title = "本机数据",
                    rows = listOf(
                        HelpRow("凭证存储", "加密保存在本机 Keystore"),
                        HelpRow("后台刷新", "频率由“刷新”设置控制"),
                    ),
                ),
            ),
            modifier = Modifier.testTag("settings_privacy_help"),
        )
    }
}

@Composable
private fun ThemeSelector(
    selected: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit,
) {
    BoxWithConstraints {
        val availableWidth = this.maxWidth
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (availableWidth < 320.dp) {
                ThemeIconChip(
                    icon = Icons.Filled.BrightnessAuto,
                    label = "跟随系统",
                    selected = selected == AppThemeMode.SYSTEM,
                    onClick = { onSelect(AppThemeMode.SYSTEM) },
                )
                ThemeIconChip(
                    icon = Icons.Filled.LightMode,
                    label = "浅色",
                    selected = selected == AppThemeMode.LIGHT,
                    onClick = { onSelect(AppThemeMode.LIGHT) },
                )
                ThemeIconChip(
                    icon = Icons.Filled.DarkMode,
                    label = "深色",
                    selected = selected == AppThemeMode.DARK,
                    onClick = { onSelect(AppThemeMode.DARK) },
                )
            } else {
                ThemeChip("跟随系统", selected == AppThemeMode.SYSTEM) {
                    onSelect(AppThemeMode.SYSTEM)
                }
                ThemeChip("浅色", selected == AppThemeMode.LIGHT) {
                    onSelect(AppThemeMode.LIGHT)
                }
                ThemeChip("深色", selected == AppThemeMode.DARK) {
                    onSelect(AppThemeMode.DARK)
                }
            }
        }
    }
}

@Composable
private fun ThemeIconChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Icon(
                imageVector = icon,
                contentDescription = label,
            )
        },
        colors = glassFilterChipColors(selected = selected),
        border = glassFilterChipBorder(selected = selected),
    )
}

@Composable
private fun ThemeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) },
        colors = glassFilterChipColors(selected = selected),
        border = glassFilterChipBorder(selected = selected),
    )
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
                GlassButton(
                    onClick = {
                        onSave(tokenInput)
                        tokenInput = ""
                    },
                    enabled = tokenInput.isNotBlank(),
                    tone = GlassButtonTone.Primary,
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
                colors = glassFilterChipColors(selected = mirrorBase.isEmpty()),
                border = glassFilterChipBorder(selected = mirrorBase.isEmpty()),
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
                colors = glassFilterChipColors(selected = mirrorBase.isNotEmpty()),
                border = glassFilterChipBorder(selected = mirrorBase.isNotEmpty()),
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
                        colors = glassFilterChipColors(selected = mirrorBase == base),
                        border = glassFilterChipBorder(selected = mirrorBase == base),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandableSettingsSection(
    title: String,
    summary: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    toggleTag: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MyTokenVisualPolicy.outerRadiusDp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            DisclosureHeader(
                title = title,
                summary = summary,
                expanded = expanded,
                onToggle = { onExpandedChange(!expanded) },
                toggleTag = toggleTag,
                horizontalPadding = 16.dp,
                verticalPadding = 16.dp,
            )
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun SettingsDisclosure(
    title: String,
    summary: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    toggleTag: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f),
    ) {
        Column {
            DisclosureHeader(
                title = title,
                summary = summary,
                expanded = expanded,
                onToggle = { onExpandedChange(!expanded) },
                toggleTag = toggleTag,
                horizontalPadding = 12.dp,
                verticalPadding = 10.dp,
            )
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun DisclosureHeader(
    title: String,
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    toggleTag: String,
    horizontalPadding: Dp,
    verticalPadding: Dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(toggleTag)
            .clickable(role = Role.Button, onClick = onToggle)
            .semantics {
                stateDescription = if (expanded) "已展开" else "已收起"
            }
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (expanded) "收起$title" else "展开$title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun aboutSectionSummary(
    appVersion: String,
    updateState: AppUpdateUiState,
): String = when (updateState) {
    is AppUpdateUiState.Available ->
        "MyToken $appVersion · 发现新版本 v${updateState.version}"
    is AppUpdateUiState.NeedsInstallPermission ->
        "MyToken $appVersion · 更新待授权安装"
    is AppUpdateUiState.ReadyToInstall ->
        "MyToken $appVersion · 更新已下载，待安装"
    is AppUpdateUiState.Downloading ->
        "MyToken $appVersion · 正在下载更新"
    is AppUpdateUiState.Checking ->
        "MyToken $appVersion · 正在检查更新"
    is AppUpdateUiState.UpToDate ->
        "MyToken $appVersion · 已是最新版本"
    is AppUpdateUiState.Error ->
        "MyToken $appVersion · 更新检查失败"
    AppUpdateUiState.Idle ->
        "MyToken $appVersion · 更新日志与诊断"
}

private fun updateAdvancedSectionSummary(settings: UpdateSettings): String {
    val source = if (settings.mirrorBase.isBlank()) "GitHub" else "CDN"
    val token = if (settings.githubTokenMask == null) "未设置令牌" else "已设置令牌"
    return "更新源：$source · $token"
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
