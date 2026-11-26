package ai.routin.mytoken.feature.settings

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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.outlined.ArrowCircleUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.launch
import ai.routin.mytoken.core.ui.SectionCard
import ai.routin.mytoken.core.ui.MyTokenAdaptiveContent
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenButton
import ai.routin.mytoken.core.ui.MyTokenButtonTone
import ai.routin.mytoken.core.ui.MyTokenBrandMark
import ai.routin.mytoken.core.ui.MyTokenReportBodyShape
import ai.routin.mytoken.core.ui.MyTokenReportTicketShape
import ai.routin.mytoken.core.ui.MyTokenRadius
import ai.routin.mytoken.core.ui.MyTokenRow
import ai.routin.mytoken.core.ui.MyTokenSection
import ai.routin.mytoken.core.ui.MyTokenSize
import ai.routin.mytoken.core.ui.MyTokenSpacing
import ai.routin.mytoken.core.ui.MyTokenThemePreviewColors
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.core.ui.MyTokenIcons
import ai.routin.mytoken.core.ui.MyTokenUpdateDialog
import ai.routin.mytoken.core.ui.MyTokenDialog
import ai.routin.mytoken.core.ui.MyTokenDialogRow
import ai.routin.mytoken.core.ui.MyTokenDialogSection
import ai.routin.mytoken.core.ui.MyTokenDialogStatus
import ai.routin.mytoken.core.ui.MyTokenDialogTone
import ai.routin.mytoken.core.ui.myTokenFilterChipBorder
import ai.routin.mytoken.core.ui.myTokenFilterChipColors
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import ai.routin.mytoken.core.ui.rememberMyTokenMotionEnabled
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
    onNotificationsEnabledChange: (Boolean) -> Unit = {},
    onAlertThresholdsChange: (Int, Int) -> Unit = { _, _ -> },
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
    var showUpdateSettings by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateDialogDownloadStarted by remember { mutableStateOf(false) }
    val versionScale = remember { androidx.compose.animation.core.Animatable(1f) }
    val versionScope = androidx.compose.runtime.rememberCoroutineScope()
    LogExportDialog(
        state = logExportState,
        onShare = onShareLogs,
        onDismiss = onDismissLogExport,
    )
    val uriHandler = LocalUriHandler.current
    val updateDownloading = updateState is AppUpdateUiState.Downloading
    val updateDownloadProgress = (updateState as? AppUpdateUiState.Downloading)?.progress
    val updateDialogVersion = when (val update = updateState) {
        is AppUpdateUiState.Available -> update.version
        is AppUpdateUiState.Downloading -> update.version
        is AppUpdateUiState.ReadyToInstall -> update.version
        is AppUpdateUiState.NeedsInstallPermission -> update.version
        else -> null
    }
    val updateDialogNotes = (updateState as? AppUpdateUiState.Available)?.releaseNotes
    val updateDialogConfirmLabel = when (updateState) {
        is AppUpdateUiState.ReadyToInstall,
        is AppUpdateUiState.NeedsInstallPermission,
        -> "安装更新"
        else -> "下载并安装"
    }
    // 下载期间保持弹窗打开并展示进度；下载结束（成功或失败）后自动收起。
    LaunchedEffect(showUpdateDialog, updateDownloading) {
        if (!showUpdateDialog) {
            updateDialogDownloadStarted = false
            return@LaunchedEffect
        }
        if (updateDownloading) {
            updateDialogDownloadStarted = true
        } else if (updateDialogDownloadStarted) {
            // 下载结束但还缺安装权限时保留弹窗，引导用户去系统授权。
            if (updateState !is AppUpdateUiState.NeedsInstallPermission) {
                showUpdateDialog = false
            }
            updateDialogDownloadStarted = false
        }
    }
    val confirmUpdateDialog: () -> Unit = {
        when (val update = updateState) {
            is AppUpdateUiState.Available ->
                onDownloadAndInstall(update.version, update.downloadUrl)
            is AppUpdateUiState.ReadyToInstall,
            is AppUpdateUiState.NeedsInstallPermission,
            -> {
                onInstallDownloadedUpdate()
                showUpdateDialog = false
            }
            else -> Unit
        }
    }

    fun checkUpdateFromVersion() {
        versionScope.launch {
            versionScale.animateTo(
                targetValue = 0.94f,
                animationSpec = androidx.compose.animation.core.tween(90),
            )
            versionScale.animateTo(
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = 0.55f,
                    stiffness = 620f,
                ),
            )
        }
        if (updateState !is AppUpdateUiState.Checking) onCheckForUpdates()
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
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
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
            verticalArrangement = Arrangement.spacedBy(MyTokenSpacing.settingsSectionCompact),
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
                SectionCard(
                    title = "外观",
                    modifier = Modifier.testTag("settings_theme_cell"),
                    contentPadding = MyTokenSpacing.cardCompact,
                    contentSpacing = MyTokenSpacing.sm,
                ) {
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
                    onNotificationsEnabledChange = onNotificationsEnabledChange,
                    onThresholdsChange = onAlertThresholdsChange,
                )
            }
            item(key = "data") {
                MyTokenSection(
                    title = "数据",
                    modifier = Modifier.testTag("settings_migration_cell"),
                    contentPadding = MyTokenSpacing.cardCompact,
                    contentSpacing = MyTokenSpacing.sm,
                ) {
                    MyTokenRow(
                        icon = Icons.Outlined.QrCodeScanner,
                        title = "从 Mac 导入",
                        supportingText = "扫码接收 Mac 凭证",
                        iconTint = MyTokenThemeTokens.colors.actionFill,
                        iconSize = 22.dp,
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
                        icon = null,
                        iconContent = {
                            SettingsBadgedIcon(
                                base = Icons.Outlined.IosShare,
                                tint = MyTokenThemeTokens.colors.actionFill,
                                size = 22.dp,
                                modifier = Modifier.testTag("settings_export_logs_icon"),
                            )
                        },
                        title = "导出诊断日志",
                        supportingText = "生成已脱敏的本机日志",
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
                val colors = MyTokenThemeTokens.colors
                val chevron: @Composable () -> Unit = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
                MyTokenSection(
                    title = "关于",
                    modifier = Modifier.testTag("settings_about_span"),
                    contentPadding = MyTokenSpacing.cardCompact,
                    contentSpacing = MyTokenSpacing.sm,
                ) {
                    MyTokenRow(
                        icon = Icons.Outlined.Info,
                        title = "当前版本",
                        iconTint = colors.actionFill,
                        iconSize = 22.dp,
                        // 长按版本号仍可导出诊断日志，点击版本号则检测更新。
                        modifier = Modifier
                            .testTag("settings_about_version")
                            .pointerInput(Unit) {
                                detectTapGestures(onLongPress = { onExportLogs() })
                            },
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "v$appVersion",
                                    modifier = Modifier
                                        .testTag("settings_version_value")
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable(
                                            role = Role.Button,
                                            onClick = ::checkUpdateFromVersion,
                                        )
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                        .graphicsLayer {
                                            scaleX = versionScale.value
                                            scaleY = versionScale.value
                                        }
                                        .semantics {
                                            contentDescription =
                                                "当前版本 v$appVersion，点击检测更新"
                                        },
                                    style = MyTokenType.label,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.ink,
                                )
                                if (updateState is AppUpdateUiState.Checking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .testTag("settings_version_checking"),
                                        strokeWidth = 1.6.dp,
                                        color = colors.actionFill,
                                    )
                                }
                                if (updateDialogVersion != null) {
                                    BreathingUpdateArrow(
                                        onClick = { showUpdateDialog = true },
                                    )
                                }
                            }
                        },
                    )
                    MyTokenRow(
                        icon = Icons.Filled.History,
                        title = "查看历史版本",
                        iconTint = colors.actionFill,
                        iconSize = 22.dp,
                        onClick = {
                            showReleaseHistory = true
                            onLoadCachedReleaseHistory()
                            onLoadReleaseHistory()
                        },
                        modifier = Modifier.testTag("settings_release_history"),
                        trailing = chevron,
                    )
                    MyTokenRow(
                        icon = MyTokenIcons.GitHub,
                        title = "支持项目",
                        iconTint = colors.actionFill,
                        iconSize = 22.dp,
                        onClick = {
                            uriHandler.openUri("https://github.com/dick86114/MyToken")
                        },
                        modifier = Modifier.testTag("settings_support_project"),
                        trailing = {
                            Row(
                                modifier = Modifier
                                    .testTag("settings_support_star")
                                    .clip(RoundedCornerShape(MyTokenRadius.pill))
                                    .border(1.dp, colors.actionFill, RoundedCornerShape(MyTokenRadius.pill))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .semantics {
                                        contentDescription = "加星标，在 GitHub 支持项目"
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = colors.actionFill,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = "加星标",
                                    style = MyTokenType.caption,
                                    color = colors.actionText,
                                )
                            }
                        },
                    )
                    MyTokenRow(
                        icon = Icons.Outlined.Handyman,
                        title = "更新设置",
                        supportingText = if (state.update.mirrorBase.isBlank()) {
                            "GitHub 直连"
                        } else {
                            "CDN 镜像"
                        },
                        iconTint = colors.actionFill,
                        iconSize = 22.dp,
                        onClick = { showUpdateSettings = true },
                        modifier = Modifier.testTag("settings_update_settings"),
                        trailing = chevron,
                    )
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
    if (showUpdateDialog) {
        MyTokenUpdateDialog(
            version = updateDialogVersion,
            releaseNotes = updateDialogNotes,
            confirmLabel = updateDialogConfirmLabel,
            downloading = updateDownloading,
            downloadProgress = updateDownloadProgress,
            needsInstallPermission = updateState is AppUpdateUiState.NeedsInstallPermission,
            onConfirm = confirmUpdateDialog,
            onDismiss = { if (!updateDownloading) showUpdateDialog = false },
            modifier = Modifier.testTag("settings_update_dialog"),
            confirmTag = "settings_update_confirm",
        )
    }
    if (showUpdateSettings) {
        UpdateSettingsDialog(
            mirrorBase = state.update.mirrorBase,
            tokenMask = state.update.githubTokenMask,
            tokenError = state.update.githubTokenError,
            onSave = { mirror, token ->
                onMirrorBaseChange(mirror)
                if (!token.isNullOrBlank()) onGitHubTokenSave(token)
                showUpdateSettings = false
            },
            onRemoveToken = onGitHubTokenRemove,
            onDismiss = { showUpdateSettings = false },
        )
    }
}

/** 有可用更新时，版本号后方的向上箭头；整枚图标连续呼吸闪烁。 */
@Composable
private fun BreathingUpdateArrow(onClick: () -> Unit) {
    val colors = MyTokenThemeTokens.colors
    val motionEnabled = rememberMyTokenMotionEnabled()
    val breathe = if (motionEnabled) {
        val transition = rememberInfiniteTransition(label = "settings-update-arrow")
        val value by transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "settings-update-arrow-breathe",
        )
        value
    } else {
        1f
    }
    Box(
        modifier = Modifier
            .size(32.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "有新版本，查看更新" }
            .testTag("settings_update_available_arrow"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.ArrowCircleUp,
            contentDescription = null,
            tint = colors.actionFill,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    alpha = if (motionEnabled) 0.45f + 0.55f * breathe else 1f
                    val scale = if (motionEnabled) 0.9f + 0.1f * breathe else 1f
                    scaleX = scale
                    scaleY = scale
                },
        )
    }
}

/** 更新设置弹窗：选择 GitHub 直连（可选令牌）或 CDN 镜像，并统一保存。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UpdateSettingsDialog(
    mirrorBase: String,
    tokenMask: String?,
    tokenError: String?,
    onSave: (String, String?) -> Unit,
    onRemoveToken: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    var github by remember { mutableStateOf(mirrorBase.isBlank()) }
    var mirror by remember {
        mutableStateOf(mirrorBase.ifBlank { DEFAULT_UPDATE_CDN_BASES.first() })
    }
    var tokenInput by rememberSaveable { mutableStateOf("") }

    MyTokenDialog(
        title = "更新设置",
        subtitle = "选择安装包来源与下载方式",
        icon = Icons.Outlined.Handyman,
        tone = MyTokenDialogTone.Info,
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("settings_update_settings_dialog"),
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "更新来源",
                    style = MyTokenType.label,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ink,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = github,
                        onClick = { github = true },
                        label = { Text("GitHub 直连") },
                        colors = myTokenFilterChipColors(selected = github),
                        border = myTokenFilterChipBorder(selected = github),
                        modifier = Modifier.testTag("update_source_github"),
                    )
                    FilterChip(
                        selected = !github,
                        onClick = { github = false },
                        label = { Text("CDN 镜像") },
                        colors = myTokenFilterChipColors(selected = !github),
                        border = myTokenFilterChipBorder(selected = !github),
                        modifier = Modifier.testTag("update_source_cdn"),
                    )
                }
                if (github) {
                    Text(
                        text = "GitHub 令牌（可选）",
                        style = MyTokenType.label,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink,
                    )
                    Text(
                        text = "公开仓库可留空；填写令牌可提高 API 请求额度。",
                        style = MyTokenType.caption,
                        color = colors.muted,
                    )
                    if (tokenMask != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "已保存 $tokenMask",
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("github_token_mask"),
                                style = MyTokenType.caption,
                                color = colors.muted,
                            )
                            TextButton(
                                onClick = onRemoveToken,
                                modifier = Modifier.testTag("github_token_remove_button"),
                            ) {
                                Text("移除")
                            }
                        }
                    }
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("github_token_input"),
                        singleLine = true,
                        placeholder = { Text("ghp_ 或 github_pat_ 令牌") },
                        visualTransformation = PasswordVisualTransformation(),
                    )
                } else {
                    Text(
                        text = "镜像源",
                        style = MyTokenType.label,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DEFAULT_UPDATE_CDN_BASES.forEachIndexed { index, base ->
                            FilterChip(
                                selected = mirror == base,
                                onClick = { mirror = base },
                                label = { Text(base.removePrefix("https://")) },
                                colors = myTokenFilterChipColors(selected = mirror == base),
                                border = myTokenFilterChipBorder(selected = mirror == base),
                                modifier = Modifier.testTag("update_mirror_$index"),
                            )
                        }
                    }
                }
                if (tokenError != null) {
                    Text(
                        text = tokenError,
                        style = MyTokenType.caption,
                        color = colors.criticalText,
                    )
                }
            }
        },
        actions = {
            MyTokenButton(
                text = "取消",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                tone = MyTokenButtonTone.Neutral,
            )
            MyTokenButton(
                text = "保存",
                onClick = {
                    onSave(
                        if (github) "" else mirror,
                        if (github) tokenInput.takeIf { it.isNotBlank() } else null,
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("update_settings_save"),
                tone = MyTokenButtonTone.Primary,
            )
        },
    )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_profile")
            // 与凭证报告同款票据形状：左上、右下圆角，右上、左下切角。
            .background(colors.ticketStubSurface, MyTokenReportBodyShape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.reportTicket, MyTokenReportTicketShape)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MyTokenBrandMark(
                modifier = Modifier.testTag("settings_brand_logo"),
                size = 44.dp,
                darkTheme = false,
            )
            Text(
                text = "MyToken",
                style = MyTokenType.profileName,
                color = colors.reportTicketInk,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "v$appVersion",
                style = MyTokenType.label.copy(fontWeight = FontWeight.SemiBold),
                color = colors.ticketStubInk,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "凭证只在本机",
                style = MyTokenType.caption,
                color = colors.ticketStubMuted,
            )
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
            // 外观选项只保留描边反馈，不叠默认的灰色按压遮罩。
            val interactionSource = remember { MutableInteractionSource() }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .testTag("theme_option_${mode.name}")
                    .selectable(
                        selected = isSelected,
                        interactionSource = interactionSource,
                        indication = null,
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

/** 三张主题预览卡：固定展示浅色/深色场景，选中时用黄色强调描边。 */
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
        selected -> colors.actionFill
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
    SectionCard(
        title = title,
        contentPadding = MyTokenSpacing.cardCompact,
        contentSpacing = MyTokenSpacing.sm,
    ) { content() }
}
