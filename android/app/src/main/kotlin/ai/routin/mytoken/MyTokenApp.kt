package ai.routin.mytoken

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.UsageCardDensity
import ai.routin.mytoken.feature.credentials.CredentialEditorScreen
import ai.routin.mytoken.feature.credentials.CredentialEditorViewModel
import ai.routin.mytoken.feature.credentials.CredentialListScreen
import ai.routin.mytoken.feature.credentials.CredentialListViewModel
import ai.routin.mytoken.feature.credentials.pruneCredential
import ai.routin.mytoken.feature.home.HomeScreen
import ai.routin.mytoken.feature.home.HomeViewModel
import ai.routin.mytoken.feature.home.CredentialDetailScreen
import ai.routin.mytoken.feature.settings.SettingsScreen
import ai.routin.mytoken.feature.settings.SettingsViewModel
import ai.routin.mytoken.feature.transfer.ImportConflictMode
import ai.routin.mytoken.feature.transfer.TransferCompletedScreen
import ai.routin.mytoken.feature.transfer.TransferPreviewScreen
import ai.routin.mytoken.feature.transfer.TransferScannerScreen
import ai.routin.mytoken.feature.transfer.TransferUiState
import ai.routin.mytoken.feature.transfer.TransferViewModel
import ai.routin.mytoken.core.ui.MyTokenButton
import ai.routin.mytoken.core.ui.MyTokenButtonTone
import ai.routin.mytoken.core.ui.MyTokenDialog
import ai.routin.mytoken.core.ui.MyTokenDialogRow
import ai.routin.mytoken.core.ui.MyTokenDialogSection
import ai.routin.mytoken.core.ui.MyTokenDialogStatus
import ai.routin.mytoken.core.ui.MyTokenDialogTone
import ai.routin.mytoken.core.ui.MyTokenEdgeDrawerHost
import ai.routin.mytoken.core.ui.MyTokenEdgeDrawerSide
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenRadius
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.rememberMyTokenLayoutMode
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.UUID
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** 首页 + 详情/编辑/迁移的轻量返回栈。 */
sealed interface AppScreen {
    data object Home : AppScreen
    data class Detail(val credentialId: UUID) : AppScreen
    data class Editor(val credentialId: UUID?) : AppScreen
    data object Transfer : AppScreen
}

/** 首页卡片末尾的两个凭证入口；都从底部滑入。 */
private enum class HomeCredentialSheet { Add, Manage }

/**
 * 首页是唯一主轴；凭证与设置分别从左右两侧以工具抽屉浮出。
 * 详情、编辑和迁移仍使用轻量返回栈，保证系统返回键语义清晰。
 */
@Composable
fun MyTokenApp(
    graph: AppGraph,
    appVersion: String,
    notificationPermissionGranted: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    openCredentialId: UUID? = null,
    onOpenCredentialConsumed: () -> Unit = {},
) {
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
    var drawerSide by remember { mutableStateOf<MyTokenEdgeDrawerSide?>(null) }
    var homeSheet by remember { mutableStateOf<HomeCredentialSheet?>(null) }
    var pageDirection by remember { mutableIntStateOf(1) }
    val homeScrollStates = ai.routin.mytoken.feature.home.rememberHomeScrollStates()
    val backStack = remember { mutableStateListOf<AppScreen>() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    // 长按「关于」导出诊断日志：真机不方便连 adb 时也能把日志发出来。
    val logExportController = remember {
        ai.routin.mytoken.diagnostics.LogExportController(
            context = context.applicationContext,
            scope = scope,
        )
    }
    val logExportState by logExportController.state.collectAsState()

    fun navigate(target: AppScreen) {
        pageDirection = 1
        drawerSide = null
        homeSheet = null
        backStack.add(screen)
        screen = target
    }

    fun goBack() {
        pageDirection = -1
        drawerSide = null
        homeSheet = null
        screen = backStack.removeLastOrNull() ?: AppScreen.Home
    }

    val drawerIsVisible = screen == AppScreen.Home && drawerSide != null
    BackHandler(enabled = drawerIsVisible || backStack.isNotEmpty()) {
        if (drawerIsVisible) {
            drawerSide = null
        } else {
            goBack()
        }
    }

    val homeViewModel = remember {
            HomeViewModel(
                repository = graph.credentialRepository,
                refreshUseCase = graph.refreshUseCase,
                credentialOrderIds = graph.credentialOrderStore.order,
                usageCardDensityFlow = graph.displaySettingsStore.settings
                    .map { it.usageCardDensity },
                onUsageCardDensityChange = { density ->
                    graph.displaySettingsStore.setUsageCardDensity(density)
                },
                refreshOnStart = false,
            )
    }

    // Lifecycle-aware freshness ticker: relative-time labels re-render every
    // 30s while the activity is resumed, and the ticker is paused while the
    // app is in the background. The tick loop itself runs off the main
    // dispatcher so it never keeps the main looper (or test idle detection) busy.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(homeViewModel, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> homeViewModel.setNowTickEnabled(true)
                Lifecycle.Event.ON_PAUSE -> homeViewModel.setNowTickEnabled(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val credentialListViewModel = remember {
        CredentialListViewModel(graph.credentialRepository, graph.credentialOrderStore)
    }
    val settingsViewModel = remember {
        SettingsViewModel(
            refreshStore = graph.refreshSettingsStore,
            displayStore = graph.displaySettingsStore,
            notificationStore = graph.notificationSettingsStore,
            updateStore = graph.updateSettingsStore,
            gitHubTokenStore = graph.gitHubTokenStore,
            updateController = graph.appUpdateController,
        )
    }

    val homeState by homeViewModel.state.collectAsStateWithLifecycle()
    val usageCardDensity by homeViewModel.usageCardDensity.collectAsStateWithLifecycle()
    val listState by credentialListViewModel.state.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()
    val updateState by settingsViewModel.updateState.collectAsStateWithLifecycle()
    val releaseHistoryState by settingsViewModel.releaseHistoryState.collectAsStateWithLifecycle()

    LaunchedEffect(settingsState.isLoading) {
        if (!settingsState.isLoading && settingsState.refresh.openAppRefresh) {
            homeViewModel.refreshAll(settingsState.refresh.retryOnFailure)
        }
    }

    val layoutMode = rememberMyTokenLayoutMode()

    LaunchedEffect(openCredentialId) {
        openCredentialId?.let { credentialId ->
            pageDirection = 1
            drawerSide = null
            screen = AppScreen.Detail(credentialId)
            backStack.clear()
            onOpenCredentialConsumed()
        }
    }

    AnimatedContent(
        targetState = screen,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val forward = pageDirection >= 0
            val enter = slideInHorizontally(
                animationSpec = spring(
                    dampingRatio = 1f,
                    stiffness = Spring.StiffnessMedium,
                ),
                initialOffsetX = { width -> if (forward) width else -width / 6 },
            ) + fadeIn(animationSpec = tween(durationMillis = 180))
            val exit = slideOutHorizontally(
                animationSpec = spring(
                    dampingRatio = 1f,
                    stiffness = Spring.StiffnessMedium,
                ),
                targetOffsetX = { width -> if (forward) -width / 6 else width },
            ) + fadeOut(animationSpec = tween(durationMillis = 140))
            enter togetherWith exit
        },
        label = "appScreenTransition",
    ) { current ->
        when (current) {
        AppScreen.Home -> {
            MyTokenEdgeDrawerHost(
                drawerSide = drawerSide,
                onDismissRequest = { drawerSide = null },
                startDrawer = {
                    CredentialListScreen(
                        state = listState,
                        layoutMode = MyTokenLayoutMode.Compact,
                        drawerMode = true,
                        onToggleEnabled = credentialListViewModel::toggleEnabled,
                        onMove = credentialListViewModel::move,
                        onEditCredential = { id -> navigate(AppScreen.Editor(id)) },
                        onRequestDelete = credentialListViewModel::requestDelete,
                        onDismissDelete = credentialListViewModel::dismissDelete,
                        onConfirmDelete = credentialListViewModel::confirmDelete,
                        onImportFromMac = { navigate(AppScreen.Transfer) },
                        onAddManually = { navigate(AppScreen.Editor(null)) },
                    )
                },
                endDrawer = {
                    SettingsScreen(
                        state = settingsState,
                        layoutMode = MyTokenLayoutMode.Compact,
                        drawerMode = true,
                        appVersion = appVersion,
                        updateState = updateState,
                        releaseHistoryState = releaseHistoryState,
                        onOpenAppRefreshChange = settingsViewModel::setOpenAppRefresh,
                        onRetryOnFailureChange = settingsViewModel::setRetryOnFailure,
                        onThemeModeChange = settingsViewModel::setThemeMode,
                        onOpenTransfer = { navigate(AppScreen.Transfer) },
                        onCredentialFailureAlertsChange = settingsViewModel::setCredentialFailureAlertsEnabled,
                        notificationPermissionGranted = notificationPermissionGranted,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        onCheckForUpdates = settingsViewModel::checkForUpdates,
                        onLoadReleaseHistory = settingsViewModel::loadReleaseHistory,
                        onLoadCachedReleaseHistory = settingsViewModel::loadCachedReleaseHistory,
                        onDownloadAndInstall = settingsViewModel::downloadAndInstall,
                        onOpenInstallPermissionSettings = settingsViewModel::openInstallPermissionSettings,
                        onInstallDownloadedUpdate = settingsViewModel::installDownloadedUpdate,
                        onMirrorBaseChange = settingsViewModel::setUpdateMirrorBase,
                        onGitHubTokenSave = settingsViewModel::setGitHubToken,
                        onGitHubTokenRemove = settingsViewModel::removeGitHubToken,
                        logExportState = logExportState,
                        onExportLogs = logExportController::export,
                        onShareLogs = logExportController::share,
                        onDismissLogExport = logExportController::dismiss,
                        onClose = { drawerSide = null },
                    )
                },
            ) {
                HomeScreen(
                    state = homeState,
                    layoutMode = layoutMode,
                    usageCardDensity = usageCardDensity,
                    onToggleUsageCardDensity = {
                        homeViewModel.setUsageCardDensity(
                            if (usageCardDensity == UsageCardDensity.COMPACT) {
                                UsageCardDensity.FULL
                            } else {
                                UsageCardDensity.COMPACT
                            }
                        )
                    },
                    onRefreshAll = {
                        homeViewModel.refreshAll(settingsState.refresh.retryOnFailure)
                    },
                    onRefreshCredential = { id ->
                        homeState.groups.flatMap { it.cards }
                            .firstOrNull { it.credential.id == id }
                            ?.let { homeViewModel.refreshCredential(it.credential) }
                    },
                    onRetryCredential = { id ->
                        val credential = homeState.groups.flatMap { it.cards }
                            .firstOrNull { it.credential.id == id }
                            ?.credential
                        credential?.let {
                            homeViewModel.refreshCredential(it)
                        }
                    },
                    onOpenCredential = { id -> navigate(AppScreen.Detail(id)) },
                    onImportFromMac = { navigate(AppScreen.Transfer) },
                    onAddManually = { homeSheet = HomeCredentialSheet.Add },
                    onOpenCredentials = { drawerSide = MyTokenEdgeDrawerSide.Start },
                    onManageCredentials = { homeSheet = HomeCredentialSheet.Manage },
                    onOpenSettings = { drawerSide = MyTokenEdgeDrawerSide.End },
                    hasUpdateAvailable = updateState is ai.routin.mytoken.feature.settings.AppUpdateUiState.Available ||
                        updateState is ai.routin.mytoken.feature.settings.AppUpdateUiState.NeedsInstallPermission ||
                        updateState is ai.routin.mytoken.feature.settings.AppUpdateUiState.ReadyToInstall,
                    homeScrollStates = homeScrollStates,
                )            }
        }

        is AppScreen.Detail -> {
            val card = homeState.groups
                .flatMap { it.cards }
                .firstOrNull { it.credential.id == current.credentialId }
            var deletionTarget by remember(current.credentialId) {
                mutableStateOf<Credential?>(null)
            }
            CredentialDetailScreen(
                card = card,
                lowThresholdPercent = settingsState.notifications.lowThresholdPercent,
                onBack = { goBack() },
                onRefresh = { card?.let { homeViewModel.refreshCredential(it.credential) } },
                onEdit = { navigate(AppScreen.Editor(current.credentialId)) },
                onDelete = { deletionTarget = card?.credential },
            )
            deletionTarget?.let { target ->
                MyTokenDialog(
                    title = "删除凭证",
                    subtitle = "只影响本机数据",
                    icon = Icons.Outlined.Delete,
                    tone = MyTokenDialogTone.Danger,
                    onDismissRequest = { deletionTarget = null },
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
                            onClick = { deletionTarget = null },
                            modifier = Modifier.weight(1f),
                            tone = MyTokenButtonTone.Neutral,
                        )
                        MyTokenButton(
                            text = "删除",
                            onClick = {
                                deletionTarget = null
                                scope.launch {
                                    runCatching { graph.credentialRepository.delete(target.id) }
                                    // Keep the Android-local order/pinned sets clean
                                    // (same contract as the credential list delete path).
                                    runCatching {
                                        graph.credentialOrderStore.pruneCredential(target.id)
                                    }
                                    homeViewModel.refreshAll()
                                    pageDirection = -1
                                    drawerSide = null
                                    screen = AppScreen.Home
                                    backStack.clear()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            tone = MyTokenButtonTone.Destructive,
                        )
                    },
                )
            }
        }

        is AppScreen.Editor -> {
            CredentialEditorRoute(
                graph = graph,
                credentialId = current.credentialId,
                layoutMode = layoutMode,
                topBarStatusPadding = true,
                onBack = { goBack() },
                onSaved = {
                    homeViewModel.refreshAfterExternalChange()
                    goBack()
                },
            )
        }

        AppScreen.Transfer -> {
            val transferViewModel = remember {
                TransferViewModel(graph.transferRepository)
            }
            val transferState by transferViewModel.state.collectAsStateWithLifecycle()
            when (val state = transferState) {
                is TransferUiState.Idle -> TransferScannerScreen(
                    isActive = true,
                    layoutMode = layoutMode,
                    onQrCodeScanned = transferViewModel::onQrCodeScanned,
                    onCancel = { goBack() },
                )
                is TransferUiState.Connecting, is TransferUiState.Importing -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text(text = if (state is TransferUiState.Connecting) "连接中…" else "导入中…")
                }
                is TransferUiState.PreviewReady -> TransferPreviewScreen(
                    state = state,
                    layoutMode = layoutMode,
                    onConfirm = { mode: ImportConflictMode -> transferViewModel.confirmImport(mode) },
                    onCancel = transferViewModel::cancel,
                )
                is TransferUiState.Completed -> {
                    LaunchedEffect(state) {
                        homeViewModel.refreshAfterExternalChange()
                    }
                    TransferCompletedScreen(
                        summary = state.summary,
                        layoutMode = layoutMode,
                        onDone = { goBack() },
                    )
                }
                is TransferUiState.Failed -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = transferViewModel::cancel) {
                        Text(text = "返回")
                    }
                }
            }
        }
        }
    }

    when (homeSheet) {
        HomeCredentialSheet.Add -> HomeCredentialSheet(
            heightFraction = 0.92f,
            testTag = "home_sheet_add",
            onDismiss = { homeSheet = null },
        ) { dismiss ->
            CredentialEditorRoute(
                graph = graph,
                credentialId = null,
                layoutMode = layoutMode,
                topBarStatusPadding = false,
                onBack = { dismiss { } },
                onSaved = {
                    homeViewModel.refreshAfterExternalChange()
                    dismiss { }
                },
            )
        }

        HomeCredentialSheet.Manage -> HomeCredentialSheet(
            heightFraction = 0.88f,
            testTag = "home_sheet_manage",
            onDismiss = { homeSheet = null },
        ) { dismiss ->
            CredentialListScreen(
                state = listState,
                layoutMode = MyTokenLayoutMode.Compact,
                drawerMode = true,
                onToggleEnabled = credentialListViewModel::toggleEnabled,
                onMove = credentialListViewModel::move,
                onEditCredential = { id -> dismiss { navigate(AppScreen.Editor(id)) } },
                onRequestDelete = credentialListViewModel::requestDelete,
                onDismissDelete = credentialListViewModel::dismissDelete,
                onConfirmDelete = credentialListViewModel::confirmDelete,
                onImportFromMac = { dismiss { navigate(AppScreen.Transfer) } },
                onAddManually = { dismiss { homeSheet = HomeCredentialSheet.Add } },
            )
        }

        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeCredentialSheet(
    heightFraction: Float,
    testTag: String,
    onDismiss: () -> Unit,
    content: @Composable ((after: () -> Unit) -> Unit) -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dismissThen: ((after: () -> Unit) -> Unit) = { after ->
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            after()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.canvas,
        shape = RoundedCornerShape(
            topStart = MyTokenRadius.sheetTop,
            topEnd = MyTokenRadius.sheetTop,
        ),
        dragHandle = {
            Box(modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(MyTokenRadius.pill))
                        .background(colors.line),
                )
            }
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(heightFraction)
                .testTag(testTag),
        ) {
            content(dismissThen)
        }
    }
}

@Composable
private fun CredentialEditorRoute(
    graph: AppGraph,
    credentialId: UUID?,
    layoutMode: MyTokenLayoutMode,
    topBarStatusPadding: Boolean,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val editorViewModel = remember(credentialId) {
        CredentialEditorViewModel(
            repository = graph.credentialRepository,
            providers = graph.providers,
            credentialId = credentialId,
        )
    }
    val editorState by editorViewModel.state.collectAsStateWithLifecycle()
    CredentialEditorScreen(
        state = editorState,
        layoutMode = layoutMode,
        topBarStatusPadding = topBarStatusPadding,
        onBack = onBack,
        onProviderChange = editorViewModel::setProviderId,
        onNameChange = editorViewModel::setName,
        onApiKeyChange = editorViewModel::setApiKey,
        onAccessKeyIDChange = editorViewModel::setAccessKeyID,
        onSecretAccessKeyChange = editorViewModel::setSecretAccessKey,
        onRegionChange = editorViewModel::setRegion,
        onBaseURLChange = editorViewModel::setBaseURL,
        onUserIDChange = editorViewModel::setUserID,
        onXiaomiUsageKindChange = editorViewModel::setXiaomiUsageKind,
        onWebsiteURLChange = editorViewModel::setWebsiteURL,
        onToggleSecretVisible = editorViewModel::toggleSecretVisible,
        onTestConnection = editorViewModel::testConnection,
        onSave = editorViewModel::save,
    )
    if (editorState.saveCompleted) {
        LaunchedEffect(editorState.saveCompleted) {
            onSaved()
        }
    }
}
