package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.alerts.AlertThresholds
import ai.routin.mytoken.core.ui.MyTokenAdaptiveContent
import ai.routin.mytoken.core.ui.MyTokenBrandMark
import ai.routin.mytoken.core.ui.MyTokenButton
import ai.routin.mytoken.core.ui.MyTokenButtonTone
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenMotion
import ai.routin.mytoken.core.ui.MyTokenRadius
import ai.routin.mytoken.core.ui.MyTokenSize
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenUpdateDialog
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.core.ui.rememberMyTokenMotionEnabled
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageCardDensity
import ai.routin.mytoken.domain.usage.RefreshStatus
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.UUID
import kotlin.math.min
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onRefreshAll: () -> Unit,
    onRefreshCredential: (UUID) -> Unit,
    onRetryCredential: (UUID) -> Unit = onRefreshCredential,
    onOpenCredential: (UUID) -> Unit,
    onImportFromMac: () -> Unit,
    onAddManually: () -> Unit,
    onOpenCredentials: () -> Unit = {},
    onManageCredentials: () -> Unit = onOpenCredentials,
    onOpenSettings: () -> Unit = {},
    hasUpdateAvailable: Boolean = false,
    updateVersion: String? = null,
    updateReleaseNotes: String? = null,
    updateConfirmLabel: String = "下载并安装",
    isUpdateDownloading: Boolean = false,
    updateDownloadProgress: Float? = null,
    isUpdateReadyToInstall: Boolean = false,
    isUpdateNeedsInstallPermission: Boolean = false,
    onConfirmUpdate: () -> Unit = onOpenSettings,
    layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact,
    usageCardDensity: UsageCardDensity = UsageCardDensity.FULL,
    onToggleUsageCardDensity: () -> Unit = {},
    homeScrollStates: HomeScrollStates = rememberHomeScrollStates(),
    globalAlertThresholds: AlertThresholds = AlertThresholds.DEFAULT,
    alertThresholdOverrides: Map<UUID, AlertThresholds> = emptyMap(),
    onEditCredential: (UUID) -> Unit = {},
    onDeleteCredential: (UUID) -> Unit = {},
) {
    val colors = MyTokenThemeTokens.colors
    var selectedProvider by remember { mutableStateOf<ProviderId?>(null) }
    val allCards = remember(state.cards) {
        state.cards.filter { it.status != RefreshStatus.Disabled }
    }
    val providerCounts = remember(allCards) {
        allCards.groupingBy { it.credential.providerId }.eachCount()
    }
    val visibleProviders = remember(state.groups, providerCounts) {
        state.groups.map { it.providerId }.filter { (providerCounts[it] ?: 0) > 0 }
    }
    val visibleCards = remember(allCards, selectedProvider) {
        selectedProvider
            ?.let { provider -> allCards.filter { it.credential.providerId == provider } }
            ?: allCards
    }
    // 固定导航收起后展示与报告一致的摘要；报警凭证的供应商 logo 横向跟随。
    val reportFindings = remember(allCards) { homeReportFindings(allCards) }
    val navHeadlineCount = if (reportFindings.isEmpty()) allCards.size else reportFindings.size
    val navSummaryTone = remember(reportFindings) { homeReportTone(reportFindings) }
    val navSummaryTail = remember(reportFindings) { homeReportTail(reportFindings) }
    val navAlertProviders = remember(reportFindings) {
        reportFindings.map { it.providerId }.distinct().take(4)
    }

    val motionEnabled = rememberMyTokenMotionEnabled()
    val scope = rememberCoroutineScope()
    var highlightedCredentialId by remember { mutableStateOf<UUID?>(null) }
    var flashTick by remember { mutableIntStateOf(0) }
    var headerHeightPx by remember { mutableIntStateOf(0) }
    var rootHeightPx by remember { mutableFloatStateOf(0f) }
    var collapsedPx by remember { mutableFloatStateOf(0f) }
    var pullOffsetPx by remember { mutableFloatStateOf(0f) }
    var pullArmed by remember { mutableStateOf(false) }
    var pullTriggered by remember { mutableStateOf(false) }
    var pullSettleJob by remember { mutableStateOf<Job?>(null) }
    var pendingLocateId by remember { mutableStateOf<UUID?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateDownloadStarted by remember { mutableStateOf(false) }
    var shownUpdateVersion by remember { mutableStateOf<String?>(null) }
    var pagerHasUserInput by remember { mutableStateOf(false) }
    // 大屏右侧详情的选中项；折叠/旋转后仍保留，切回 compact 时交给现有整页详情。
    var wideDetailId by rememberSaveable { mutableStateOf<UUID?>(null) }

    // 下载期间保持弹窗打开并展示进度；下载结束（成功或失败）后自动收起，
    // 但缺安装权限时保留弹窗，引导用户去系统授权。
    LaunchedEffect(showUpdateDialog, isUpdateDownloading, isUpdateNeedsInstallPermission) {
        if (!showUpdateDialog) {
            updateDownloadStarted = false
            return@LaunchedEffect
        }
        if (isUpdateDownloading) {
            updateDownloadStarted = true
        } else if (updateDownloadStarted) {
            if (!isUpdateNeedsInstallPermission) {
                showUpdateDialog = false
            }
            updateDownloadStarted = false
        }
    }

    // 折叠或旋转回普通手机宽度时，把已打开的侧栏详情交还给整页详情导航。
    LaunchedEffect(layoutMode, wideDetailId) {
        val id = wideDetailId
        if (layoutMode == MyTokenLayoutMode.Compact && id != null) {
            wideDetailId = null
            onOpenCredential(id)
        }
    }

    if (layoutMode != MyTokenLayoutMode.Compact) {
        // 右侧详情是浮层，系统返回先收起它，不离开首页。
        BackHandler(enabled = wideDetailId != null) {
            wideDetailId = null
        }
        Scaffold(containerColor = colors.canvas) { padding ->
            WideHomeScreen(
                state = state,
                allCards = allCards,
                visibleCards = visibleCards,
                providers = visibleProviders,
                providerCounts = providerCounts,
                selectedProvider = selectedProvider,
                onSelectProvider = {
                    pendingLocateId = null
                    selectedProvider = it
                },
                usageCardDensity = usageCardDensity,
                onToggleUsageCardDensity = onToggleUsageCardDensity,
                layoutMode = layoutMode,
                selectedDetailId = wideDetailId,
                onSelectDetail = { wideDetailId = it },
                onRefreshAll = onRefreshAll,
                onRefreshCredential = onRefreshCredential,
                onRetryCredential = onRetryCredential,
                onOpenCredentials = onOpenCredentials,
                onManageCredentials = onManageCredentials,
                onOpenSettings = onOpenSettings,
                onImportFromMac = onImportFromMac,
                onAddManually = onAddManually,
                globalAlertThresholds = globalAlertThresholds,
                alertThresholdOverrides = alertThresholdOverrides,
                onEditCredential = onEditCredential,
                onDeleteCredential = onDeleteCredential,
                hasUpdateAvailable = hasUpdateAvailable,
                onOpenUpdate = {
                    shownUpdateVersion = updateVersion
                    showUpdateDialog = true
                },
                homeScrollStates = homeScrollStates,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
        HomeUpdateDialogHost(
            show = showUpdateDialog,
            shownVersion = shownUpdateVersion,
            updateVersion = updateVersion,
            releaseNotes = updateReleaseNotes,
            confirmLabel = updateConfirmLabel,
            downloading = isUpdateDownloading,
            progress = updateDownloadProgress,
            readyToInstall = isUpdateReadyToInstall,
            needsInstallPermission = isUpdateNeedsInstallPermission,
            onConfirmUpdate = onConfirmUpdate,
            onDismiss = { if (!isUpdateDownloading) showUpdateDialog = false },
        )
        return
    }

    val density = LocalDensity.current
    val topNavHeightPx = with(density) { 60.dp.toPx() }
    val pullThresholdPx = with(density) { 56.dp.toPx() }
    val pullMaxPx = with(density) { 96.dp.toPx() }
    val pullHoldPx = with(density) { 56.dp.toPx() }
    val chromeVisible = !state.isLoading && allCards.isNotEmpty()
    val latestRefreshing by rememberUpdatedState(state.isRefreshingAll)
    val latestRefreshAll by rememberUpdatedState(onRefreshAll)
    // 刷新仍在后台进行，但用户一旦上滑就立即收起顶部指示器，避免它长期残留在页面顶端。
    fun dismissPullIndicator() {
        if (pullOffsetPx <= 0f && !pullTriggered) return
        pullSettleJob?.cancel()
        pullSettleJob = null
        pullTriggered = false
        pullOffsetPx = 0f
    }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val pagerState = rememberPagerState(
        initialPage = if (usageCardDensity == UsageCardDensity.COMPACT) 0 else 1,
        pageCount = { 2 },
    )

    fun activeScrollMode(): HomeScrollMode {
        val pageDensity = if (pagerState.currentPage == 0) {
            UsageCardDensity.COMPACT
        } else {
            UsageCardDensity.FULL
        }
        return homeScrollMode(pageDensity, screenWidthDp)
    }

    /** 第一张卡片完整露出（在报告下沿以下）后，报告才开始跟随下拉。 */
    fun activePageFirstCardFullyVisible(): Boolean {
        val visibleHeaderBottom = (headerHeightPx - collapsedPx).coerceAtLeast(0f)
        return when (activeScrollMode()) {
            HomeScrollMode.CompactGrid -> {
                val info = homeScrollStates.compact.layoutInfo
                info.visibleItemsInfo.firstOrNull { it.index == 0 }
                    ?.let { it.offset.y + info.beforeContentPadding >= visibleHeaderBottom } ?: false
            }

            HomeScrollMode.FullWideGrid -> {
                val info = homeScrollStates.fullWide.layoutInfo
                info.visibleItemsInfo.firstOrNull { it.index == 0 }
                    ?.let { it.offset.y + info.beforeContentPadding >= visibleHeaderBottom } ?: false
            }

            HomeScrollMode.FullList -> {
                val info = homeScrollStates.full.layoutInfo
                info.visibleItemsInfo.firstOrNull { it.index == 0 }
                    ?.let { it.offset + info.beforeContentPadding >= visibleHeaderBottom } ?: false
            }
        }
    }

    fun activePageCanScrollBackward(): Boolean = when (activeScrollMode()) {
        HomeScrollMode.CompactGrid -> homeScrollStates.compact.canScrollBackward
        HomeScrollMode.FullWideGrid -> homeScrollStates.fullWide.canScrollBackward
        HomeScrollMode.FullList -> homeScrollStates.full.canScrollBackward
    }

    // 列表原生滚动；头部在 onPostScroll 里同步跟随，不再拦截 fling。
    val pullConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                if (available.y < 0f) {
                    pullArmed = false
                    dismissPullIndicator()
                    return Offset.Zero
                }
                if (!pullArmed || latestRefreshing || collapsedPx > 0f) {
                    return Offset.Zero
                }
                val remaining = (pullMaxPx - pullOffsetPx).coerceAtLeast(0f)
                if (remaining <= 0f) return Offset.Zero
                pullOffsetPx += min(available.y * 0.55f, remaining)
                return Offset(0f, available.y)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val max = headerHeightPx.toFloat()
                if (max > 0f) {
                    if (consumed.y < 0f) {
                        // 上滑时头部跟随卡片收起。
                        collapsedPx = (collapsedPx - consumed.y).coerceIn(0f, max)
                        return Offset.Zero
                    }
                    if (consumed.y > 0f && activePageFirstCardFullyVisible()) {
                        // 第一张卡片完整露出后，列表下移多少，报告就展开多少，
                        // 两块区域一起向下移动，不再出现报告盖住卡片。
                        collapsedPx = (collapsedPx - consumed.y).coerceIn(0f, max)
                        return Offset.Zero
                    }
                    if (collapsedPx > 0f && available.y > 0f && !activePageCanScrollBackward()) {
                        // 列表已在顶部但头部仍有收起量：用剩余下拉补齐报告。
                        val expand = min(available.y, collapsedPx)
                        collapsedPx -= expand
                        return Offset(0f, expand)
                    }
                }
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(chromeVisible) {
        if (!chromeVisible) {
            headerHeightPx = 0
            collapsedPx = 0f
        }
    }
    LaunchedEffect(headerHeightPx) {
        if (collapsedPx > headerHeightPx) {
            collapsedPx = headerHeightPx.toFloat()
        }
    }

    val fontScale = density.fontScale
    val latestDensity by rememberUpdatedState(usageCardDensity)
    val latestToggleDensity by rememberUpdatedState(onToggleUsageCardDensity)

    fun activeScrollableState(): ScrollableState = when (activeScrollMode()) {
        HomeScrollMode.CompactGrid -> homeScrollStates.compact
        HomeScrollMode.FullWideGrid -> homeScrollStates.fullWide
        HomeScrollMode.FullList -> homeScrollStates.full
    }

    // 程序化定位不会派发 nested scroll；直接读第一张卡的布局位移来同步头部。
    // 首卡 offset 恰为负的列表滚动量，offset 缺失即已滚过整块头部。
    fun collapseTargetFromList(density: UsageCardDensity = usageCardDensity): Float {
        val max = headerHeightPx.toFloat()
        if (max <= 0f) return 0f
        val firstCardOffset = when (homeScrollMode(density, screenWidthDp)) {
            HomeScrollMode.FullList ->
                homeScrollStates.full.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == 0 }?.offset
            HomeScrollMode.FullWideGrid ->
                homeScrollStates.fullWide.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == 0 }?.offset?.y
            HomeScrollMode.CompactGrid ->
                homeScrollStates.compact.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == 0 }?.offset?.y
        }
        return if (firstCardOffset == null) {
            max
        } else {
            (-firstCardOffset).toFloat().coerceIn(0f, max)
        }
    }

    val headerHeightDp = with(density) { headerHeightPx.toDp() }

    // 收起进度只读同步累积的位移；读取发生在布局/绘制阶段，不触发整页重组。
    val collapseProgressState = remember {
        derivedStateOf {
            if (headerHeightPx <= 0) 0f else (collapsedPx / headerHeightPx).coerceIn(0f, 1f)
        }
    }
    val collapseProgress: () -> Float = { collapseProgressState.value }

    // 手势拖动由 Pager 自然跟手；只有真正停稳后才把落点页同步回持久化的模式。
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val settled =
                    if (page == 0) UsageCardDensity.COMPACT else UsageCardDensity.FULL
                // 两页各自保留滚动位置；切页后头部必须对齐新页，而不是沿用上一页的收起量。
                collapsedPx = collapseTargetFromList(settled)
                if (settled != latestDensity) latestToggleDensity()
            }
    }

    // 用户一旦亲手滑过分页，就由手势与落页结果决定模式；晚到的旧密度回调不能抢回页面。
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (scrolling) pagerHasUserInput = true
            }
    }

    // 顶部按钮切换模式时，让整页平滑滑到目标页而不是直接跳变；同时覆盖
    // 连续点击（动画进行中反向点击）的情况，targetPage 不同就重新定向。
    // 用户已经滑过分页后跳过，避免与快速反滑或延迟落库的旧回调打架。
    LaunchedEffect(usageCardDensity) {
        if (pagerHasUserInput) return@LaunchedEffect
        val target = if (usageCardDensity == UsageCardDensity.COMPACT) 0 else 1
        if (pagerState.settledPage != target || pagerState.targetPage != target) {
            if (motionEnabled) pagerState.animateScrollToPage(target) else pagerState.scrollToPage(target)
        }
    }

    suspend fun animatePullTo(target: Float) {
        val start = pullOffsetPx
        if (!motionEnabled || start == target) {
            pullOffsetPx = target
            return
        }
        animate(
            initialValue = start,
            targetValue = target,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        ) { value, _ ->
            pullOffsetPx = value
        }
    }

    fun settlePull(allowRefresh: Boolean) {
        val shouldRefresh =
            allowRefresh && pullOffsetPx >= pullThresholdPx && !latestRefreshing
        pullSettleJob?.cancel()
        pullSettleJob = scope.launch {
            if (!shouldRefresh) {
                animatePullTo(0f)
                return@launch
            }
            pullTriggered = true
            latestRefreshAll()
            // 等待刷新状态真正开始，再保持指示器；短请求也不会卡在展开态。
            val started = withTimeoutOrNull(900) {
                snapshotFlow { latestRefreshing }.first { it }
            }
            if (started == true) {
                animatePullTo(pullHoldPx)
                snapshotFlow { latestRefreshing }.first { !it }
            }
            animatePullTo(0f)
            pullTriggered = false
        }
    }

    val pullGestureModifier = Modifier.pointerInput(
        pagerState,
        homeScrollStates,
        screenWidthDp,
    ) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            if (!pullTriggered) pullSettleJob?.cancel()
            pullArmed = !latestRefreshing && collapsedPx <= 0f &&
                !activePageCanScrollBackward()
            do {
                val event = awaitPointerEvent()
            } while (event.changes.any { it.pressed })
            val shouldSettle = pullArmed || pullOffsetPx > 0f
            val allowRefresh = pullArmed
            pullArmed = false
            if (shouldSettle) settlePull(allowRefresh)
        }
    }

    fun locateNow(id: UUID) {
        val index = visibleCards.indexOfFirst { it.credential.id == id }
        if (index < 0) return
        scope.launch {
            // 定位滚动不会派发 nested scroll，让头部跟随定位过程同步收起。
            val collapseTracker = launch {
                snapshotFlow { collapseTargetFromList() }
                    .distinctUntilChanged()
                    .collect { collapsedPx = it }
            }
            locateCard(
                states = homeScrollStates,
                mode = homeScrollMode(usageCardDensity, screenWidthDp),
                index = index,
                // 列表带有与覆盖层等高的顶部留白；居中时要同时补偿导航与留白。
                centerBiasPx = topNavHeightPx / 2f + headerHeightPx,
                durationMillis = MyTokenMotion.locateDurationMillis(motionEnabled),
            )
            collapseTracker.cancel()
            collapsedPx = collapseTargetFromList()
            highlightedCredentialId = id
            flashTick += 1
            delay(1_000)
            if (highlightedCredentialId == id) {
                highlightedCredentialId = null
            }
        }
    }

    fun requestLocate(id: UUID) {
        if (visibleCards.any { it.credential.id == id }) {
            locateNow(id)
        } else {
            // 报告口径是全部凭证；定位被筛选隐藏的卡片时先清除筛选。
            selectedProvider = null
            pendingLocateId = id
        }
    }

    LaunchedEffect(pendingLocateId, selectedProvider) {
        val id = pendingLocateId ?: return@LaunchedEffect
        if (selectedProvider == null && visibleCards.any { it.credential.id == id }) {
            pendingLocateId = null
            locateNow(id)
        }
    }

    @Composable
    fun cardItem(card: CredentialCardUi, density: UsageCardDensity) {
        val isHighlighted = highlightedCredentialId == card.credential.id
        CredentialUsageCard(
            card = card,
            metricColumns = 2,
            layoutMode = layoutMode,
            usageCardDensity = density,
            highlightSignal = if (isHighlighted) flashTick else 0,
            onOpen = { onOpenCredential(card.credential.id) },
            onRefresh = { onRefreshCredential(card.credential.id) },
            onRetry = { onRetryCredential(card.credential.id) },
        )
    }

    @Composable
    fun modePage(pageDensity: UsageCardDensity) {
        val pageMode = homeScrollMode(pageDensity, screenWidthDp)
        val pageColumns = homeGridColumns(pageMode, screenWidthDp, fontScale)
        val pageTag = homeGridTag(pageMode, pageColumns)
        val isScrolled by remember(pageMode) {
            derivedStateOf {
                when (pageMode) {
                    HomeScrollMode.FullList ->
                        homeScrollStates.full.firstVisibleItemIndex > 0 ||
                            homeScrollStates.full.firstVisibleItemScrollOffset > 0

                    HomeScrollMode.CompactGrid ->
                        homeScrollStates.compact.firstVisibleItemIndex > 0 ||
                            homeScrollStates.compact.firstVisibleItemScrollOffset > 0

                    HomeScrollMode.FullWideGrid ->
                        homeScrollStates.fullWide.firstVisibleItemIndex > 0 ||
                            homeScrollStates.fullWide.firstVisibleItemScrollOffset > 0
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            val contentPadding = PaddingValues(top = headerHeightDp + 2.dp, bottom = 12.dp)
            val spacing = if (pageDensity.isCompact()) 8.dp else 12.dp
            when (pageMode) {
                    HomeScrollMode.FullList -> LazyColumn(
                        state = homeScrollStates.full,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(pageTag),
                        contentPadding = contentPadding,
                        verticalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        items(
                            items = visibleCards,
                            key = { it.credential.id },
                            contentType = { "credential-usage-card" },
                        ) { card -> cardItem(card, pageDensity) }
                        item(key = "credential_end", contentType = "credential-end") {
                            CredentialEndActions(onManage = onManageCredentials)
                        }
                    }

                    HomeScrollMode.FullWideGrid -> LazyVerticalGrid(
                        state = homeScrollStates.fullWide,
                        columns = GridCells.Fixed(pageColumns),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(pageTag),
                        contentPadding = contentPadding,
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        verticalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        gridItems(
                            items = visibleCards,
                            key = { it.credential.id },
                            contentType = { "credential-usage-card" },
                        ) { card -> cardItem(card, pageDensity) }
                        item(
                            key = "credential_end",
                            contentType = "credential-end",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            CredentialEndActions(onManage = onManageCredentials)
                        }
                    }

                    // 简洁卡高度随内容自适应，用瀑布流避免相邻列因行数不同而对不齐。
                    HomeScrollMode.CompactGrid -> LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(pageColumns),
                        state = homeScrollStates.compact,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(pageTag),
                        contentPadding = contentPadding,
                        verticalItemSpacing = spacing,
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        staggeredItems(
                            items = visibleCards,
                            key = { it.credential.id },
                            contentType = { "credential-usage-card" },
                        ) { card -> cardItem(card, pageDensity) }
                        item(
                            key = "credential_end",
                            contentType = "credential-end",
                            span = StaggeredGridItemSpan.FullLine,
                        ) {
                            CredentialEndActions(onManage = onManageCredentials)
                        }
                    }
                }

            if (isScrolled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(colors.line, Color.Transparent),
                            ),
                        )
                        .testTag("home_scroll_edge"),
                )
            }
        }
    }

    Scaffold { padding ->
        Box(
            // 背景层铺满整个窗口（含状态栏），内容区再按系统栏内边距避让。
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { rootHeightPx = it.height.toFloat() }
                // 整页背景随收起进度从顶部黄色渐变到画布色；深色主题即黄到黑。
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            0f to lerp(
                                colors.canvas,
                                colors.reportTicket,
                                collapseProgress(),
                            ),
                            1f to colors.canvas,
                        ),
                    )
                },
        ) {
            MyTokenAdaptiveContent(
                maxWidth = 1440.dp,
                horizontalPadding = 16.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    HomeToolbar(
                        credentialCount = state.cards.size,
                        headlineCount = navHeadlineCount,
                        summaryTail = navSummaryTail,
                        summaryTone = navSummaryTone,
                        alertProviders = navAlertProviders,
                        collapseProgress = collapseProgress,
                        onOpenCredentials = onOpenCredentials,
                        onOpenSettings = onOpenSettings,
                        hasUpdateAvailable = hasUpdateAvailable,
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clipToBounds()
                            .nestedScroll(pullConnection)
                            .then(pullGestureModifier),
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            // 两种密度都提前组合并测量，首次滑动不会像“重新加载”一页。
                            beyondViewportPageCount = 1,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { translationY = pullOffsetPx }
                                .testTag("home_pager"),
                            key = { page -> page },
                        ) { page ->
                            val pageDensity = if (page == 0) {
                                UsageCardDensity.COMPACT
                            } else {
                                UsageCardDensity.FULL
                            }
                            // 两页都提前组合；静止时只有当前页进入无障碍树和测试语义树。
                            val pageActive = pagerState.isScrollInProgress || pagerState.settledPage == page
                            val pageSemantics = if (pageActive) {
                                Modifier.testTag("home_page_active")
                            } else {
                                Modifier.clearAndSetSemantics { }
                            }
                            Box(modifier = Modifier.fillMaxSize().then(pageSemantics)) {
                                when {
                                    state.isLoading -> Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator()
                                    }

                                    allCards.isEmpty() -> EmptyState(
                                        modifier = Modifier.fillMaxSize(),
                                        onImportFromMac = onImportFromMac,
                                        onAddManually = onAddManually,
                                    )

                                    else -> modePage(pageDensity = pageDensity)
                                }
                            }
                        }

                        if (chromeVisible) {
                            // 报告和工具条是覆盖层：只做位移动画，不改变分页区高度。
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // 只用图层位移，滚动时不会让整块头部重新测量。
                                    .graphicsLayer {
                                        translationY =
                                            pullOffsetPx - headerHeightPx * collapseProgress()
                                    }
                                    // 遮罩必须放在图层内，否则收起后仍以原尺寸盖住下方卡片。
                                    // 绘制整页渐变的对应切片，既挡住下方卡片又不出现接缝。
                                    .drawBehind {
                                        val headerTopPx =
                                            padding.calculateTopPadding().toPx() +
                                                topNavHeightPx +
                                                pullOffsetPx -
                                                headerHeightPx * collapseProgress()
                                        val pageHeight = rootHeightPx
                                        val startFraction = if (pageHeight > 0f) {
                                            (headerTopPx / pageHeight).coerceIn(0f, 1f)
                                        } else {
                                            0f
                                        }
                                        val endFraction = if (pageHeight > 0f) {
                                            ((headerTopPx + size.height) / pageHeight)
                                                .coerceIn(0f, 1f)
                                        } else {
                                            0f
                                        }
                                        val highlight = lerp(
                                            colors.canvas,
                                            colors.reportTicket,
                                            collapseProgress(),
                                        )
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                0f to lerp(
                                                    highlight,
                                                    colors.canvas,
                                                    startFraction,
                                                ),
                                                1f to lerp(
                                                    highlight,
                                                    colors.canvas,
                                                    endFraction,
                                                ),
                                            ),
                                        )
                                    }
                                    // 完全收起后不再绘制整块报告，深列表快速 fling 更轻。
                                    .drawWithContent {
                                        if (collapseProgress() < 1f) drawContent()
                                    }
                                    .onSizeChanged { headerHeightPx = it.height }
                                    // 报告本身不可滚动：在头部直接上/下拖也驱动当前列表滚动，
                                    // 与在卡片列表上的纵向手势保持一致。
                                    .pointerInput(Unit) {
                                        detectVerticalDragGestures { change, dragAmount ->
                                            if (dragAmount < 0f) {
                                                // 同一次手势只要向上滚过，就要求重新起手才能下拉刷新。
                                                pullArmed = false
                                                dismissPullIndicator()
                                                // dispatchRawDelta 正值向列表尾部滚动，与手势方向相反。
                                                val consumed = activeScrollableState().dispatchRawDelta(-dragAmount)
                                                collapsedPx = (collapsedPx + consumed)
                                                    .coerceIn(0f, headerHeightPx.toFloat())
                                            } else if (!activePageCanScrollBackward()) {
                                                if (collapsedPx > 0f) {
                                                    collapsedPx = (collapsedPx - dragAmount)
                                                        .coerceIn(0f, headerHeightPx.toFloat())
                                                } else if (
                                                    pullArmed &&
                                                    !latestRefreshing &&
                                                    !pullTriggered
                                                ) {
                                                    val remaining = (
                                                        pullMaxPx - pullOffsetPx
                                                        ).coerceAtLeast(0f)
                                                    if (remaining > 0f) {
                                                        pullOffsetPx += min(
                                                            dragAmount * 0.55f,
                                                            remaining,
                                                        )
                                                    }
                                                }
                                            } else {
                                                val consumed = activeScrollableState().dispatchRawDelta(-dragAmount)
                                                if (activePageFirstCardFullyVisible()) {
                                                    collapsedPx = (collapsedPx + consumed)
                                                        .coerceIn(0f, headerHeightPx.toFloat())
                                                }
                                            }
                                            change.consume()
                                        }
                                    }
                                    .testTag("home_collapsing_header"),
                            ) {
                                if (hasUpdateAvailable) {
                                    HomeUpdateBanner(
                                        version = updateVersion,
                                        onOpenUpdate = {
                                            shownUpdateVersion = updateVersion
                                            showUpdateDialog = true
                                        },
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                HomeHealthReport(
                                    cards = allCards,
                                    syncText = state.lastUpdatedText ?: "刚刚同步",
                                    isRefreshing = state.isRefreshingAll,
                                    onRefresh = onRefreshAll,
                                    onLocate = ::requestLocate,
                                )
                                HomeProviderToolbar(
                                    selectedProvider = selectedProvider,
                                    providers = visibleProviders,
                                    counts = providerCounts,
                                    allTotal = allCards.size,
                                    onSelect = {
                                        pendingLocateId = null
                                        selectedProvider = it
                                    },
                                    density = usageCardDensity,
                                    // 点击只驱动分页动画；落页后再持久化模式，
                                    // 避免选中态先跳变造成整块闪烁。
                                    onToggleDensity = {
                                        val targetPage =
                                            if (pagerState.currentPage == 0) 1 else 0
                                        scope.launch {
                                            if (motionEnabled) {
                                                pagerState.animateScrollToPage(targetPage)
                                            } else {
                                                pagerState.scrollToPage(targetPage)
                                            }
                                        }
                                    },
                                    densityProgress = {
                                        (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                                            .coerceIn(0f, 1f)
                                    },
                                )
                            }
                        }

                        if (pullOffsetPx > 0.5f || pullTriggered) {
                            HomePullIndicator(
                                offsetPx = pullOffsetPx,
                                thresholdPx = pullThresholdPx,
                                refreshing = pullTriggered,
                                motionEnabled = motionEnabled,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    HomeUpdateDialogHost(
        show = showUpdateDialog,
        shownVersion = shownUpdateVersion,
        updateVersion = updateVersion,
        releaseNotes = updateReleaseNotes,
        confirmLabel = updateConfirmLabel,
        downloading = isUpdateDownloading,
        progress = updateDownloadProgress,
        readyToInstall = isUpdateReadyToInstall,
        needsInstallPermission = isUpdateNeedsInstallPermission,
        onConfirmUpdate = onConfirmUpdate,
        onDismiss = { if (!isUpdateDownloading) showUpdateDialog = false },
    )
}

/** 手机与大屏共用的更新弹窗；下载结束后的收起规则保持一致。 */
@Composable
private fun HomeUpdateDialogHost(
    show: Boolean,
    shownVersion: String?,
    updateVersion: String?,
    releaseNotes: String?,
    confirmLabel: String,
    downloading: Boolean,
    progress: Float?,
    readyToInstall: Boolean,
    needsInstallPermission: Boolean,
    onConfirmUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) return
    MyTokenUpdateDialog(
        version = shownVersion ?: updateVersion,
        releaseNotes = releaseNotes,
        confirmLabel = confirmLabel,
        downloading = downloading,
        downloadProgress = progress,
        needsInstallPermission = needsInstallPermission,
        onConfirm = {
            onConfirmUpdate()
            if (readyToInstall) onDismiss()
        },
        onDismiss = onDismiss,
        modifier = Modifier.testTag("home_update_dialog"),
        confirmTag = "home_update_confirm",
    )
}

/** 下拉刷新指示器：透明图标直接出现在报告上方，不铺整行底色。 */
@Composable
private fun HomePullIndicator(
    offsetPx: Float,
    thresholdPx: Float,
    refreshing: Boolean,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    val progress = (offsetPx / thresholdPx).coerceIn(0f, 1f)
    val infinite = rememberInfiniteTransition(label = "pull-refresh")
    val spin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
        label = "pull-refresh-spin",
    )
    val rotation = if (refreshing && motionEnabled) spin else progress * 270f
    Box(
        modifier = modifier
            .size(40.dp)
            .graphicsLayer {
                alpha = if (refreshing) 1f else progress
                val scale = 0.72f + 0.28f * progress
                scaleX = scale
                scaleY = scale
            }
            .semantics {
                contentDescription = if (refreshing) {
                    "正在刷新全部凭证"
                } else {
                    "下拉刷新全部凭证"
                }
            }
            .testTag("home_pull_indicator"),
        contentAlignment = Alignment.Center,
    ) {
        ReportRefreshGlyph(
            tint = colors.actionText,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { rotationZ = rotation },
        )
    }
}

/** 卡片末尾的小胶囊入口：集中管理已有凭证。 */
@Composable
internal fun CredentialEndActions(
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    val shape = RoundedCornerShape(MyTokenRadius.pill)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(MyTokenSize.touch)
                .clip(shape)
                .clickable(role = Role.Button, onClick = onManage)
                .semantics { contentDescription = "管理凭证" }
                .testTag("home_manage_credentials"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .clip(shape)
                    .background(colors.surface)
                    .border(1.dp, colors.line, shape)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "管理凭证",
                    style = MyTokenType.caption,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ink,
                )
            }
        }
    }
}

@Composable
internal fun EmptyState(
    modifier: Modifier = Modifier,
    onImportFromMac: () -> Unit,
    onAddManually: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MyTokenBrandMark(size = 54.dp)
        Text(
            text = "暂无凭证",
            modifier = Modifier.padding(top = 18.dp),
            style = MyTokenType.title,
            fontWeight = FontWeight.SemiBold,
        )
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
