package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.alerts.AlertThresholds
import ai.routin.mytoken.core.ui.MyTokenBrandMark
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenMotion
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.core.ui.ProviderLogoPlate
import ai.routin.mytoken.core.ui.rememberMyTokenMotionEnabled
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageCardDensity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * 大屏：左侧供应商轨 + 手机同款左右分页卡片区 + 报告收合 + 右侧详情浮层。
 *
 * 卡片区复用手机竖屏的简洁 / 完整两页、密度开关联动与滚动收合；
 * 详情改为悬浮抽屉，首页不缩放、不缩窄。
 */
@Composable
internal fun WideHomeScreen(
    state: HomeUiState,
    allCards: List<CredentialCardUi>,
    visibleCards: List<CredentialCardUi>,
    providers: List<ProviderId>,
    providerCounts: Map<ProviderId, Int>,
    selectedProvider: ProviderId?,
    onSelectProvider: (ProviderId?) -> Unit,
    usageCardDensity: UsageCardDensity,
    onToggleUsageCardDensity: () -> Unit,
    layoutMode: MyTokenLayoutMode,
    selectedDetailId: UUID?,
    onSelectDetail: (UUID?) -> Unit,
    onRefreshAll: () -> Unit,
    onRefreshCredential: (UUID) -> Unit,
    onRetryCredential: (UUID) -> Unit,
    onOpenCredentials: () -> Unit,
    onManageCredentials: () -> Unit,
    onOpenSettings: () -> Unit,
    onImportFromMac: () -> Unit,
    onAddManually: () -> Unit,
    globalAlertThresholds: AlertThresholds,
    alertThresholdOverrides: Map<UUID, AlertThresholds>,
    onEditCredential: (UUID) -> Unit,
    onDeleteCredential: (UUID) -> Unit,
    hasUpdateAvailable: Boolean,
    onOpenUpdate: () -> Unit,
    homeScrollStates: HomeScrollStates,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    val motionEnabled = rememberMyTokenMotionEnabled()
    val density = LocalDensity.current
    val fontScale = density.fontScale
    val scope = rememberCoroutineScope()

    // 整窗比例：Configuration 的可用高度不含系统栏，补回后才不会把 4:3 推成 16:9。
    val configuration = LocalConfiguration.current
    val layoutDirection = LocalLayoutDirection.current
    val systemBars = WindowInsets.systemBars
    val windowWidthDp = configuration.screenWidthDp + with(density) {
        (systemBars.getLeft(this, layoutDirection) + systemBars.getRight(this, layoutDirection))
            .toDp()
            .value
    }
    val windowHeightDp = configuration.screenHeightDp + with(density) {
        (systemBars.getTop(this) + systemBars.getBottom(this)).toDp().value
    }
    val aspectRatio = if (windowHeightDp > 0f) windowWidthDp / windowHeightDp else 1f
    val isLandscape = windowWidthDp > windowHeightDp
    val railWidth = WideHomeSpec.railWidth(layoutMode)
    val detailPanelWidth = WideHomeSpec.detailWidth(aspectRatio)
    val reportOrientation = WideHomeSpec.reportOrientation(isLandscape)

    val pagerState = rememberPagerState(
        initialPage = if (usageCardDensity == UsageCardDensity.COMPACT) 0 else 1,
        pageCount = { 2 },
    )
    var pagerHasUserInput by remember { mutableStateOf(false) }
    var headerHeightPx by remember { mutableIntStateOf(0) }
    var collapsedPx by remember { mutableFloatStateOf(0f) }
    var highlightedCredentialId by remember { mutableStateOf<UUID?>(null) }
    var flashTick by remember { mutableIntStateOf(0) }
    var pendingLocateId by remember { mutableStateOf<UUID?>(null) }
    var deleteTarget by remember { mutableStateOf<CredentialCardUi?>(null) }

    val chromeVisible = !state.isLoading && allCards.isNotEmpty()
    val latestDensity by rememberUpdatedState(usageCardDensity)
    val latestToggleDensity by rememberUpdatedState(onToggleUsageCardDensity)

    fun pageDensity(page: Int): UsageCardDensity =
        if (page == 0) UsageCardDensity.COMPACT else UsageCardDensity.FULL

    fun scrollModeFor(cardDensity: UsageCardDensity): HomeScrollMode =
        if (cardDensity.isCompact()) HomeScrollMode.CompactGrid else HomeScrollMode.FullWideGrid

    fun activeScrollMode(): HomeScrollMode = scrollModeFor(pageDensity(pagerState.currentPage))

    fun activeScrollableState(): ScrollableState = when (activeScrollMode()) {
        HomeScrollMode.CompactGrid -> homeScrollStates.compact
        HomeScrollMode.FullWideGrid -> homeScrollStates.fullWide
        HomeScrollMode.FullList -> homeScrollStates.full
    }

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

    fun collapseTargetFromList(cardDensity: UsageCardDensity = pageDensity(pagerState.currentPage)): Float {
        val max = headerHeightPx.toFloat()
        if (max <= 0f) return 0f
        val firstCardOffset = when (scrollModeFor(cardDensity)) {
            HomeScrollMode.CompactGrid ->
                homeScrollStates.compact.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == 0 }?.offset?.y
            HomeScrollMode.FullWideGrid ->
                homeScrollStates.fullWide.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == 0 }?.offset?.y
            HomeScrollMode.FullList ->
                homeScrollStates.full.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == 0 }?.offset
        }
        return if (firstCardOffset == null) {
            max
        } else {
            (-firstCardOffset).toFloat().coerceIn(0f, max)
        }
    }

    // 报告/控制条是可收合的覆盖层；卡片列表原生滚动，头部只跟随位移。
    val headerScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val max = headerHeightPx.toFloat()
                if (max <= 0f) return Offset.Zero
                if (consumed.y < 0f) {
                    collapsedPx = (collapsedPx - consumed.y).coerceIn(0f, max)
                    return Offset.Zero
                }
                if (consumed.y > 0f && activePageFirstCardFullyVisible()) {
                    collapsedPx = (collapsedPx - consumed.y).coerceIn(0f, max)
                    return Offset.Zero
                }
                if (collapsedPx > 0f && available.y > 0f && !activePageCanScrollBackward()) {
                    val expand = min(available.y, collapsedPx)
                    collapsedPx -= expand
                    return Offset(0f, expand)
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

    // 两页各自保留滚动位置；落页后把头部对齐到新页，并持久化密度。
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val settled = pageDensity(page)
                collapsedPx = collapseTargetFromList(settled)
                if (settled != latestDensity) latestToggleDensity()
            }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (scrolling) pagerHasUserInput = true
            }
    }
    LaunchedEffect(usageCardDensity) {
        if (pagerHasUserInput) return@LaunchedEffect
        val target = if (usageCardDensity == UsageCardDensity.COMPACT) 0 else 1
        if (pagerState.settledPage != target) {
            if (motionEnabled) {
                pagerState.animateScrollToPage(
                    target,
                    animationSpec = tween(
                        durationMillis = MyTokenMotion.modeSwitchDurationMillis(true),
                        easing = FastOutSlowInEasing,
                    ),
                )
            } else {
                pagerState.scrollToPage(target)
            }
        }
    }

    val headerHeightDp = with(density) { headerHeightPx.toDp() }
    // 收起进度只在绘制/图层阶段读取；滚动时这里不会整页重组。
    val collapseProgressState = remember {
        derivedStateOf {
            if (headerHeightPx <= 0) 0f else (collapsedPx / headerHeightPx).coerceIn(0f, 1f)
        }
    }
    val collapseProgress: () -> Float = { collapseProgressState.value }

    fun toggleDensity() {
        val target = if (pagerState.currentPage == 0) 1 else 0
        scope.launch {
            if (motionEnabled) {
                // 固定时长的 tween 比默认弹簧尾段更短，减少模式切换的耗时帧。
                pagerState.animateScrollToPage(
                    target,
                    animationSpec = tween(
                        durationMillis = MyTokenMotion.modeSwitchDurationMillis(true),
                        easing = FastOutSlowInEasing,
                    ),
                )
            } else {
                pagerState.scrollToPage(target)
            }
        }
    }

    val densityProgress: () -> Float = {
        (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
    }

    fun locateNow(id: UUID) {
        val index = visibleCards.indexOfFirst { it.credential.id == id }
        if (index < 0) return
        val currentMode = activeScrollMode()
        scope.launch {
            // 定位滚动不会派发 nested scroll，让头部跟随定位过程同步收起。
            val collapseTracker = launch {
                snapshotFlow { collapseTargetFromList() }
                    .distinctUntilChanged()
                    .collect { collapsedPx = it }
            }
            locateCard(
                states = homeScrollStates,
                mode = currentMode,
                index = index,
                centerBiasPx = headerHeightPx.toFloat(),
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
            // 报告口径始终是全部凭证；定位被筛掉的卡片时先恢复全部。
            onSelectProvider(null)
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

    val requestedDetailCard = allCards.firstOrNull { it.credential.id == selectedDetailId }
    // 凭证被删除或集合变化导致目标消失时收起详情；加载中先等卡片恢复。
    LaunchedEffect(selectedDetailId, requestedDetailCard, state.isLoading) {
        if (!state.isLoading && selectedDetailId != null && requestedDetailCard == null) {
            onSelectDetail(null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // 顶部导航底色随收起进度从黄色渐变到画布色；深色主题即黄到黑。
            // 静止时纯色；滚动时渐变只覆盖头部下方一段，避免全屏着色在低端 GPU 上掉帧。
            .drawBehind {
                drawRect(colors.canvas)
                val p = collapseProgress()
                if (p > 0.001f) {
                    val bandHeight = (headerHeightPx + 320.dp.toPx()).coerceAtMost(size.height)
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to lerp(colors.canvas, colors.reportTicket, p),
                            1f to colors.canvas,
                        ),
                        size = Size(size.width, bandHeight),
                    )
                }
            },
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            WideProviderRail(
                providers = providers,
                counts = providerCounts,
                selectedProvider = selectedProvider,
                onSelectProvider = {
                    pendingLocateId = null
                    onSelectProvider(it)
                },
                allTotal = allCards.size,
                isRefreshing = state.isRefreshingAll,
                hasUpdateAvailable = hasUpdateAvailable,
                onRefreshAll = onRefreshAll,
                onOpenSettings = onOpenSettings,
                onOpenCredentials = onOpenCredentials,
                railWidth = railWidth,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clipToBounds(),
            ) {
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

                    else -> {
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            val compactColumns = remember(maxWidth, fontScale) {
                                WideHomeSpec.compactColumns(maxWidth, fontScale)
                            }
                            val fullColumns = remember(maxWidth, fontScale) {
                                WideHomeSpec.fullColumns(maxWidth, fontScale)
                            }
                            HorizontalPager(
                                state = pagerState,
                                // 两页都提前组合并测量：切换密度时只做图层位移动画，
                                // 不会在按钮按下时才开始组合另一页的卡片。
                                beyondViewportPageCount = 1,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(headerScrollConnection)
                                    .testTag("home_pager"),
                                key = { page -> page },
                            ) { page ->
                                val pageDensity = pageDensity(page)
                                // 静止时只有当前页进入无障碍树和测试语义树。
                                val pageActive by remember {
                                    derivedStateOf {
                                        pagerState.isScrollInProgress ||
                                            pagerState.settledPage == page
                                    }
                                }
                                val pageSemantics = if (pageActive) {
                                    Modifier.testTag("home_page_active")
                                } else {
                                    Modifier.clearAndSetSemantics { }
                                }
                                Box(modifier = Modifier.fillMaxSize().then(pageSemantics)) {
                                    WideCardsPage(
                                        density = pageDensity,
                                        columns = if (pageDensity.isCompact()) {
                                            compactColumns
                                        } else {
                                            fullColumns
                                        },
                                        cards = visibleCards,
                                        layoutMode = layoutMode,
                                        selectedDetailId = selectedDetailId,
                                        highlightedCredentialId = highlightedCredentialId,
                                        flashTick = flashTick,
                                        headerHeightDp = headerHeightDp,
                                        scrollStates = homeScrollStates,
                                        onOpenCard = { id ->
                                            onSelectDetail(if (selectedDetailId == id) null else id)
                                        },
                                        onRefreshCard = onRefreshCredential,
                                        onRetryCard = onRetryCredential,
                                        onManageCredentials = onManageCredentials,
                                    )
                                }
                            }

                            if (chromeVisible) {
                                // 底衬单独一层：收起时只让这层画一条纯色，报告内容仍留在
                                // graphicsLayer 的缓存里随位移移动，滚动时不必逐帧重录报告。
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(headerHeightDp)
                                        .graphicsLayer {
                                            translationY = -headerHeightPx * collapseProgress()
                                        }
                                        .drawBehind {
                                            val p = collapseProgress()
                                            if (p > 0.001f) {
                                                drawRect(lerp(colors.canvas, colors.reportTicket, p))
                                            }
                                        }
                                        .testTag("home_wide_header_backdrop"),
                                )
                                // 报告和工具条是覆盖层：只做图层位移，不改变分页区高度。
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            translationY = -headerHeightPx * collapseProgress()
                                        }
                                        .onSizeChanged { headerHeightPx = it.height }
                                        // 报告本身不可滚动：在头部直接上/下拖也驱动当前列表滚动。
                                        .pointerInput(Unit) {
                                            detectVerticalDragGestures { change, dragAmount ->
                                                if (dragAmount < 0f) {
                                                    // dispatchRawDelta 正值向列表尾部滚动，与手势方向相反。
                                                    val consumed = activeScrollableState()
                                                        .dispatchRawDelta(-dragAmount)
                                                    collapsedPx = (collapsedPx + consumed)
                                                        .coerceIn(0f, headerHeightPx.toFloat())
                                                } else if (!activePageCanScrollBackward()) {
                                                    if (collapsedPx > 0f) {
                                                        collapsedPx = (collapsedPx - dragAmount)
                                                            .coerceIn(0f, headerHeightPx.toFloat())
                                                    } else {
                                                        val consumed = activeScrollableState()
                                                            .dispatchRawDelta(-dragAmount)
                                                        if (activePageFirstCardFullyVisible()) {
                                                            collapsedPx = (collapsedPx + consumed)
                                                                .coerceIn(0f, headerHeightPx.toFloat())
                                                        }
                                                    }
                                                } else {
                                                    val consumed = activeScrollableState()
                                                        .dispatchRawDelta(-dragAmount)
                                                    if (activePageFirstCardFullyVisible()) {
                                                        collapsedPx = (collapsedPx + consumed)
                                                            .coerceIn(0f, headerHeightPx.toFloat())
                                                    }
                                                }
                                                change.consume()
                                            }
                                        }
                                        .testTag("home_wide_collapsing_header"),
                                ) {
                                    WideReportHeader(
                                        cards = allCards,
                                        syncText = state.lastUpdatedText ?: "刚刚同步",
                                        orientation = reportOrientation,
                                        onLocate = ::requestLocate,
                                        density = usageCardDensity,
                                        onToggleDensity = ::toggleDensity,
                                        densityProgress = densityProgress,
                                        updateAvailable = hasUpdateAvailable,
                                        onOpenUpdate = onOpenUpdate,
                                        motionEnabled = motionEnabled,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        WideDetailDrawer(
            selectedCard = requestedDetailCard,
            layoutMode = layoutMode,
            panelWidth = detailPanelWidth,
            globalAlertThresholds = globalAlertThresholds,
            alertThresholdOverrides = alertThresholdOverrides,
            onClose = { onSelectDetail(null) },
            onRefresh = onRefreshCredential,
            onEdit = onEditCredential,
            onDelete = { deleteTarget = it },
        )
    }

    deleteTarget?.let { target ->
        CredentialDeleteDialog(
            target = target.credential,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                onDeleteCredential(target.credential.id)
            },
        )
    }
}

/** 单页卡片区：按密度选手机同款布局，列宽以手机竖屏卡宽为基准。 */
@Composable
private fun WideCardsPage(
    density: UsageCardDensity,
    columns: Int,
    cards: List<CredentialCardUi>,
    layoutMode: MyTokenLayoutMode,
    selectedDetailId: UUID?,
    highlightedCredentialId: UUID?,
    flashTick: Int,
    headerHeightDp: Dp,
    scrollStates: HomeScrollStates,
    onOpenCard: (UUID) -> Unit,
    onRefreshCard: (UUID) -> Unit,
    onRetryCard: (UUID) -> Unit,
    onManageCredentials: () -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    val compact = density.isCompact()
    val spacing = if (compact) WideHomeSpec.compactCardSpacing else WideHomeSpec.fullCardSpacing
    val mode = if (compact) HomeScrollMode.CompactGrid else HomeScrollMode.FullWideGrid
    val isScrolled by remember(mode) {
        derivedStateOf {
            when (mode) {
                HomeScrollMode.CompactGrid ->
                    scrollStates.compact.firstVisibleItemIndex > 0 ||
                        scrollStates.compact.firstVisibleItemScrollOffset > 0

                HomeScrollMode.FullWideGrid ->
                    scrollStates.fullWide.firstVisibleItemIndex > 0 ||
                        scrollStates.fullWide.firstVisibleItemScrollOffset > 0

                HomeScrollMode.FullList ->
                    scrollStates.full.firstVisibleItemIndex > 0 ||
                        scrollStates.full.firstVisibleItemScrollOffset > 0
            }
        }
    }
    val contentPadding = PaddingValues(top = headerHeightDp + 2.dp, bottom = 12.dp)
    Box(modifier = Modifier.fillMaxSize()) {
        when (mode) {
            HomeScrollMode.CompactGrid -> LazyVerticalStaggeredGrid(
                state = scrollStates.compact,
                columns = StaggeredGridCells.Fixed(columns),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(homeGridTag(mode, columns)),
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalItemSpacing = spacing,
            ) {
                staggeredItems(
                    items = cards,
                    key = { it.credential.id },
                    contentType = { "credential-usage-card" },
                ) { card ->
                    CredentialUsageCard(
                        card = card,
                        metricColumns = 2,
                        layoutMode = layoutMode,
                        usageCardDensity = density,
                        selected = selectedDetailId == card.credential.id,
                        highlightSignal =
                            if (highlightedCredentialId == card.credential.id) flashTick else 0,
                        onOpen = { onOpenCard(card.credential.id) },
                        onRefresh = { onRefreshCard(card.credential.id) },
                        onRetry = { onRetryCard(card.credential.id) },
                    )
                }
                item(
                    key = "credential_end",
                    contentType = "credential-end",
                    span = StaggeredGridItemSpan.FullLine,
                ) {
                    CredentialEndActions(onManage = onManageCredentials)
                }
            }

            HomeScrollMode.FullWideGrid -> LazyVerticalGrid(
                state = scrollStates.fullWide,
                columns = GridCells.Fixed(columns),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(homeGridTag(mode, columns)),
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                gridItems(
                    items = cards,
                    key = { it.credential.id },
                    contentType = { "credential-usage-card" },
                ) { card ->
                    CredentialUsageCard(
                        card = card,
                        metricColumns = 2,
                        layoutMode = layoutMode,
                        usageCardDensity = density,
                        selected = selectedDetailId == card.credential.id,
                        highlightSignal =
                            if (highlightedCredentialId == card.credential.id) flashTick else 0,
                        onOpen = { onOpenCard(card.credential.id) },
                        onRefresh = { onRefreshCard(card.credential.id) },
                        onRetry = { onRetryCard(card.credential.id) },
                    )
                }
                item(
                    key = "credential_end",
                    contentType = "credential-end",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    CredentialEndActions(onManage = onManageCredentials)
                }
            }

            HomeScrollMode.FullList -> Unit
        }
        if (isScrolled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.line, colors.line.copy(alpha = 0f)),
                        ),
                    )
                    .testTag("home_scroll_edge"),
            )
        }
    }
}

/** 报告 + 更新/密度控制：横屏左右排，竖屏上下排。 */
@Composable
private fun WideReportHeader(
    cards: List<CredentialCardUi>,
    syncText: String,
    orientation: WideReportOrientation,
    onLocate: (UUID) -> Unit,
    density: UsageCardDensity,
    onToggleDensity: () -> Unit,
    densityProgress: () -> Float,
    updateAvailable: Boolean,
    onOpenUpdate: () -> Unit,
    motionEnabled: Boolean,
) {
    when (orientation) {
        WideReportOrientation.Horizontal -> Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WideHomeHealthReport(
                cards = cards,
                syncText = syncText,
                orientation = orientation,
                onLocate = onLocate,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            LandscapeReportControls(
                density = density,
                onToggleDensity = onToggleDensity,
                densityProgress = densityProgress,
                updateAvailable = updateAvailable,
                onOpenUpdate = onOpenUpdate,
                motionEnabled = motionEnabled,
            )
        }

        WideReportOrientation.Vertical -> Column(modifier = Modifier.fillMaxWidth()) {
            WideHomeHealthReport(
                cards = cards,
                syncText = syncText,
                orientation = orientation,
                onLocate = onLocate,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                if (updateAvailable) {
                    WideUpdateHint(
                        onClick = onOpenUpdate,
                        motionEnabled = motionEnabled,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                DensityModeSwitch(
                    density = density,
                    onToggle = onToggleDensity,
                    progress = densityProgress,
                )
            }
        }
    }
}

/** 横屏报告右侧：默认竖排密度；有更新时切横排并在上方放更新提示。 */
@Composable
private fun LandscapeReportControls(
    density: UsageCardDensity,
    onToggleDensity: () -> Unit,
    densityProgress: () -> Float,
    updateAvailable: Boolean,
    onOpenUpdate: () -> Unit,
    motionEnabled: Boolean,
) {
    Column(
        modifier = Modifier.width(84.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (updateAvailable) {
            WideUpdateHint(
                onClick = onOpenUpdate,
                motionEnabled = motionEnabled,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        DensityModeSwitch(
            density = density,
            onToggle = onToggleDensity,
            progress = densityProgress,
            orientation = if (updateAvailable) {
                DensitySwitchOrientation.Horizontal
            } else {
                DensitySwitchOrientation.Vertical
            },
        )
    }
}

/** 小更新提示：只有箭头做呼吸动画，文字与整块保持稳定。 */
@Composable
private fun WideUpdateHint(
    onClick: () -> Unit,
    motionEnabled: Boolean,
) {
    val colors = MyTokenThemeTokens.colors
    val pulse = if (motionEnabled) {
        val transition = rememberInfiniteTransition(label = "wide-update-breath")
        transition.animateFloat(
            initialValue = 0.82f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "wide-update-arrow",
        )
    } else {
        null
    }
    Row(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.reportTicket)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "有可用更新，查看更新" }
            .padding(horizontal = 9.dp)
            .testTag("home_wide_update_hint"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowUp,
            contentDescription = null,
            tint = colors.reportTicketInk,
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer {
                    val p = pulse?.value ?: 1f
                    scaleX = p
                    scaleY = p
                    alpha = 0.55f + 0.45f * p
                },
        )
        Text(
            text = "更新",
            style = MyTokenType.caption,
            fontWeight = FontWeight.SemiBold,
            color = colors.reportTicketInk,
            maxLines = 1,
        )
    }
}

/** 左侧 64/72dp 图标轨：品牌、全部、供应商，底部固定刷新/设置/凭证管理。 */
@Composable
private fun WideProviderRail(
    providers: List<ProviderId>,
    counts: Map<ProviderId, Int>,
    selectedProvider: ProviderId?,
    onSelectProvider: (ProviderId?) -> Unit,
    allTotal: Int,
    isRefreshing: Boolean,
    hasUpdateAvailable: Boolean,
    onRefreshAll: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCredentials: () -> Unit,
    railWidth: Dp,
) {
    val colors = MyTokenThemeTokens.colors
    val motionEnabled = rememberMyTokenMotionEnabled()
    // 只有刷新时才启动旋转动画，避免静止时还有一个无限动画在跑。
    val spin = if (isRefreshing && motionEnabled) {
        val transition = rememberInfiniteTransition(label = "wide-rail-refresh")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
            label = "wide-rail-refresh-spin",
        )
    } else {
        null
    }

    Column(
        modifier = Modifier
            .width(railWidth)
            .fillMaxHeight()
            .background(colors.surface)
            .drawBehind {
                drawLine(
                    color = colors.line,
                    start = Offset(size.width - 0.5f, 0f),
                    end = Offset(size.width - 0.5f, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .testTag("home_wide_rail"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(role = Role.Button, onClick = onOpenCredentials)
                .semantics { contentDescription = "打开凭证列表" }
                .testTag("home_open_credentials"),
            contentAlignment = Alignment.Center,
        ) {
            MyTokenBrandMark(size = 30.dp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            RailSelectionButton(
                selected = selectedProvider == null,
                contentDescription = "全部供应商，$allTotal 个凭证",
                tag = "home_wide_rail_all",
                onClick = { onSelectProvider(null) },
            ) {
                AllProvidersGlyph(
                    providers = providers,
                    selected = selectedProvider == null,
                )
            }
            providers.forEach { provider ->
                RailSelectionButton(
                    selected = selectedProvider == provider,
                    contentDescription =
                        "${ProviderCatalog.displayName(provider)}，${counts[provider] ?: 0} 个凭证",
                    tag = "home_wide_rail_provider_${provider.rawValue}",
                    onClick = {
                        onSelectProvider(if (selectedProvider == provider) null else provider)
                    },
                ) {
                    ProviderLogoPlate(
                        painter = painterResource(ProviderCatalog.logoRes(provider)),
                        contentDescription = null,
                        size = 30.dp,
                        shape = RoundedCornerShape(9.dp),
                        contentPadding = 3.dp,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(1.dp)
                .background(colors.line),
        )
        Spacer(modifier = Modifier.height(6.dp))
        RailActionButton(
            contentDescription = if (isRefreshing) "正在刷新全部凭证" else "刷新全部凭证",
            tag = "home_wide_rail_refresh",
            onClick = onRefreshAll,
        ) {
            ReportRefreshGlyph(
                tint = colors.muted,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = spin?.value ?: 0f },
            )
        }
        RailActionButton(
            contentDescription = if (hasUpdateAvailable) {
                "打开设置，有可用更新"
            } else {
                "打开设置"
            },
            tag = "home_open_settings",
            onClick = onOpenSettings,
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(20.dp),
            )
        }
        RailActionButton(
            contentDescription = "凭证管理",
            tag = "home_wide_rail_credentials",
            onClick = onOpenCredentials,
        ) {
            Icon(
                imageVector = Icons.Outlined.CreditCard,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * “全部供应商”图标：30dp 白色 plate 内铺当前供应商小 logo。
 * 1 个居中；2–4 个 2 列；5–6 个 3×2；更多 3 列自适应行数。
 */
@Composable
private fun AllProvidersGlyph(
    providers: List<ProviderId>,
    selected: Boolean,
) {
    val colors = MyTokenThemeTokens.colors
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(shape)
            .background(colors.controlHandle)
            .border(1.dp, colors.line, shape)
            .testTag("home_wide_rail_all_icon")
            .padding(1.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (providers.isEmpty()) {
            Icon(
                imageVector = Icons.Outlined.GridView,
                contentDescription = null,
                tint = if (selected) colors.ink else colors.muted,
                modifier = Modifier.size(17.dp),
            )
            return@Box
        }
        if (providers.size == 1) {
            ProviderLogoPlate(
                painter = painterResource(ProviderCatalog.logoRes(providers.first())),
                contentDescription = null,
                size = 20.dp,
                modifier = Modifier.testTag(
                    "home_wide_rail_all_logo_${providers.first().rawValue}",
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = 0.dp,
            )
            return@Box
        }
        val columns = WideHomeSpec.allProviderLogoColumns(providers.size)
        val rows = ceil(providers.size / columns.toFloat()).toInt()
        val gap = 1.dp
        val cell = minOf(
            (28.dp - gap * (columns - 1)) / columns,
            (28.dp - gap * (rows - 1)) / rows,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            providers.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { provider ->
                        ProviderLogoPlate(
                            painter = painterResource(
                                ProviderCatalog.logoRes(provider),
                            ),
                            contentDescription = null,
                            size = cell,
                            modifier = Modifier.testTag(
                                "home_wide_rail_all_logo_${provider.rawValue}",
                            ),
                            shape = RoundedCornerShape(2.dp),
                            contentPadding = 0.dp,
                        )
                    }
                    repeat(columns - row.size) {
                        Spacer(modifier = Modifier.size(cell))
                    }
                }
            }
        }
    }
}

@Composable
private fun RailSelectionButton(
    selected: Boolean,
    contentDescription: String,
    tag: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(shape)
            .background(if (selected) colors.subtle else colors.surface)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                this.selected = selected
                this.contentDescription = contentDescription
            }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        content()
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.ink),
            )
        }
    }
}

@Composable
private fun RailActionButton(
    contentDescription: String,
    tag: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
