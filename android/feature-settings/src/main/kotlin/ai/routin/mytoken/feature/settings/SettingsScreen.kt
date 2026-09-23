package ai.routin.mytoken.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import ai.routin.mytoken.core.ui.glassFilterChipBorder
import ai.routin.mytoken.core.ui.glassFilterChipColors
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

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
    layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact,
) {
    var showReleaseHistory by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val currentRelease = (releaseHistoryState as? AppReleaseHistoryUiState.Loaded)
        ?.releases
        ?.firstOrNull { it.version == appVersion }

    // 打开设置页只读本地缓存；用户点击“获取”后才请求网络并回写缓存。
    LaunchedEffect(Unit) {
        onLoadCachedReleaseHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        },
    ) { padding ->
        val columns = if (layoutMode == MyTokenLayoutMode.Compact) 1 else 2
        MyTokenAdaptiveContent(
            maxWidth = 1200.dp,
            horizontalPadding = 16.dp,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize().testTag("settings_list"),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "theme") {
                SectionCard(title = "主题", modifier = Modifier.testTag("settings_theme_cell")) {
                    ThemeSelector(
                        selected = state.display.themeMode,
                        onSelect = onThemeModeChange,
                    )
                }
            }
            item(key = "migration") {
                SectionCard(
                    title = "数据迁移",
                    modifier = Modifier.testTag("settings_migration_cell"),
                ) {
                    Text(
                        text = "从 Mac 扫码迁移凭证，密钥端到端加密传输。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    GlassButton(
                        onClick = onOpenTransfer,
                        tone = GlassButtonTone.Primary,
                        text = "从 Mac 导入",
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
            item(
                key = "about",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                Box(modifier = Modifier.testTag("settings_about_span")) {
                SectionCard(title = "关于") {
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
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = onLoadReleaseHistory,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("release_notes_refresh_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "获取当前版本日志",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

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
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(14.dp))
                    CurrentReleaseNotesSection(
                        state = releaseHistoryState,
                        currentVersion = appVersion,
                        onRetry = onLoadReleaseHistory,
                        onShowHistory = { showReleaseHistory = true },
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(14.dp))
                    UpdateChannelSection(
                        mirrorBase = state.update.mirrorBase,
                        onMirrorBaseChange = onMirrorBaseChange,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    UpdateSection(
                        state = updateState,
                        currentVersion = appVersion,
                        onCheckForUpdates = onCheckForUpdates,
                        onDownloadAndInstall = onDownloadAndInstall,
                        onOpenInstallPermissionSettings = onOpenInstallPermissionSettings,
                        onInstallDownloadedUpdate = onInstallDownloadedUpdate,
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
            Text(
                text = "更新日志",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

@Composable
private fun ReleaseHistoryDialog(
    state: AppReleaseHistoryUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("历史版本") },
        text = {
            when (state) {
                AppReleaseHistoryUiState.Idle,
                AppReleaseHistoryUiState.Loading -> Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("正在加载...")
                }
                is AppReleaseHistoryUiState.Error -> Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                        Text("日志获取失败", color = MaterialTheme.colorScheme.error)
                    GlassButton(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                        tone = GlassButtonTone.Primary,
                        text = "重试",
                        icon = Icons.Filled.Refresh,
                    )
                }
                is AppReleaseHistoryUiState.Loaded -> {
                    if (state.releases.isEmpty()) {
                        Text("暂无更新日志")
                    } else {
                        ReleaseHistoryList(releases = state.releases)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        },
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
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
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
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("update_channel_selector"),
        ) {
            SegmentedButton(
                selected = mirrorBase.isEmpty(),
                onClick = { onMirrorBaseChange("") },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = {},
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
            )
            SegmentedButton(
                selected = mirrorBase.isNotEmpty(),
                onClick = {
                    if (mirrorBase.isEmpty()) {
                        onMirrorBaseChange(DEFAULT_UPDATE_CDN_BASES.first())
                    }
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = {},
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
