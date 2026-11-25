package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.alerts.AlertThresholds
import ai.routin.mytoken.core.ui.MyTokenBrandMark
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenMotion
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.core.ui.rememberMyTokenMotionEnabled
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageCardDensity
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ArrowCircleUp
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import java.util.UUID
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
    val isLandscape = windowWidthDp > windowHeightDp
    val railWidth = WideHomeSpec.railWidth(layoutMode)
    val reportOrientation = WideHomeSpec.reportOrientation(isLandscape)
    val systemBarTopDp = with(density) { systemBars.getTop(density).toDp() }
    val systemBarBottomDp = with(density) { systemBars.getBottom(density).toDp() }
    val systemBarEndDp = with(density) {
        systemBars.getRight(density, layoutDirection).toDp()
    }
    // 收合头部：状态栏 + 48dp 报告行为整块实色琥珀，随后 200dp 过渡到画布色。
    // 整窗背景与遮住卡片的实色渐变共用这两个尺寸，颜色和终点完全一致。
    val headerBarHeight = 48.dp
    val headerFadeDistance = 200.dp
    val headerGradientEndDp = systemBarTopDp + headerBarHeight + headerFadeDistance
    val headerSolidFraction =
        (systemBarTopDp + headerBarHeight).value / headerGradientEndDp.value.coerceAtLeast(1f)
    val headerCoverBrush = remember(
        colors.reportTicket,
        colors.canvas,
        headerSolidFraction,
    ) {
        Brush.verticalGradient(
            0f to colors.reportTicket,
            headerSolidFraction to colors.reportTicket,
            1f to colors.canvas,
        )
    }

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
    // 收起后的一行短票与手机竖屏导航摘要同口径：报告包含全部凭证。
    val reportFindings = remember(allCards) { homeReportFindings(allCards) }
    val collapsedHeadline = if (reportFindings.isEmpty()) allCards.size else reportFindings.size
    val collapsedTail = remember(reportFindings) { homeReportTail(reportFindings) }
    val collapsedTone = remember(reportFindings) { homeReportTone(reportFindings) }
    val syncLabel = state.lastUpdatedText ?: "刚刚同步"

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

    // 全窗背景：顶部（状态栏 + 报告行）整块不透明票面黄，向下渐变到画布色。
    // 预渲染成整窗位图，滚动时只贴一张图，不逐帧新建 shader。
    val gradientCache = remember(colors.canvas, colors.reportTicket) {
        WideGradientBitmapCache()
    }
    val view = LocalView.current
    val activity = remember(view) { view.context.findActivity() }
    val latestDetailId by rememberUpdatedState(selectedDetailId)
    LaunchedEffect(activity, view, colors.canvas, colors.reportTicket) {
        val window = activity?.window ?: return@LaunchedEffect
        // 状态栏图标深浅跟随状态栏后面的渐变颜色；只在跨过明暗阈值时写一次。
        snapshotFlow {
            if (latestDetailId != null) {
                // 详情遮罩把整窗压暗，系统栏改用浅色图标。
                Triple(false, false, true)
            } else {
                val p = collapseProgressState.value
                val statusDark = lerp(colors.canvas, colors.reportTicket, p).luminance() > 0.5f
                // 导航栏/手势条始终是画布色，图标深浅只跟画布明暗走，不再跟随收合变琥珀。
                val navDark = colors.canvas.luminance() > 0.5f
                Triple(statusDark, navDark, false)
            }
        }
            .distinctUntilChanged()
            .collect { (statusDark, navDark, detailOpen) ->
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = statusDark
                controller.isAppearanceLightNavigationBars = navDark
                // 系统导航栏也设成画布色；只有详情遮罩打开时才用遮罩色。
                window.navigationBarColor = if (detailOpen) {
                    colors.scrim.toArgb()
                } else {
                    colors.canvas.toArgb()
                }
            }
    }
    DisposableEffect(activity, view) {
        val window = activity?.window
        // 三键导航默认会给透明导航栏加一层对比度遮罩，会盖住收合后的琥珀色。
        // 大屏自己保证背景对比度，因此关闭这层系统遮罩。
        val previousContrast = window?.isNavigationBarContrastEnforced
        val previousNavColor = window?.navigationBarColor
        window?.isNavigationBarContrastEnforced = false
        onDispose {
            if (window == null) return@onDispose
            val controller = WindowCompat.getInsetsController(window, view)
            val canvasDark = colors.canvas.luminance() > 0.5f
            controller.isAppearanceLightStatusBars = canvasDark
            controller.isAppearanceLightNavigationBars = canvasDark
            if (previousContrast != null) {
                window.isNavigationBarContrastEnforced = previousContrast
            }
            if (previousNavColor != null) {
                window.navigationBarColor = previousNavColor
            }
        }
    }

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
            delay(
                MyTokenMotion.locateHighlightWindowMillis(
                    enabled = motionEnabled,
                    pulses = MyTokenMotion.wideLocatePulseCount,
                ).toLong(),
            )
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
            // 整窗统一背景：左轨、卡片区和系统栏后面都是同一张画布 + 票面黄渐变。
            // 滚动时 drawBehind 只贴一张缓存位图，不重组、不逐帧新建 shader。
            .background(colors.canvas)
            .drawBehind {
                val p = collapseProgress()
                if (p > 0.001f) {
                    // 顶部状态栏 + 报告行整块保持不透明票面黄，再向下渐变到画布色。
                    // 整窗同一张背景图：左轨与内容区共用，不会出现左右两条渐变。
                    val headerPx = systemBarTopDp.toPx() + headerBarHeight.toPx()
                    val fadePx = headerFadeDistance.toPx()
                    val target = IntSize(
                        size.width.toInt().coerceAtLeast(1),
                        size.height.toInt().coerceAtLeast(1),
                    )
                    val cached = gradientCache.bitmap
                    val bitmap = if (
                        cached != null &&
                        gradientCache.size == target &&
                        gradientCache.headerPx == headerPx
                    ) {
                        cached
                    } else {
                        val created = ImageBitmap(target.width, target.height)
                        CanvasDrawScope().draw(
                            density = this,
                            layoutDirection = layoutDirection,
                            canvas = Canvas(created),
                            size = Size(target.width.toFloat(), target.height.toFloat()),
                        ) {
                            val height = size.height.coerceAtLeast(1f)
                            val solidStop = (headerPx / height).coerceIn(0f, 1f)
                            val canvasStop = ((headerPx + fadePx) / height)
                                .coerceIn(solidStop, 1f)
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to colors.reportTicket,
                                    solidStop to colors.reportTicket,
                                    canvasStop to colors.canvas,
                                    1f to colors.canvas,
                                ),
                            )
                        }
                        gradientCache.bitmap = created
                        gradientCache.size = target
                        gradientCache.headerPx = headerPx
                        created
                    }
                    drawImage(
                        image = bitmap,
                        dstSize = target,
                        alpha = p,
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
                onUpdateClick = onOpenUpdate,
                railWidth = railWidth,
                collapseProgress = collapseProgress,
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
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp + systemBarEndDp,
                                ),
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
                                        bottomInset = systemBarBottomDp,
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
                                // 不透明实色渐变头：从状态栏的琥珀用实色色标过渡到画布色，
                                // 高度随收合进度增长，把渐变区内的卡片全部盖住；
                                // 不使用 alpha 遮罩，收合后任何卡片都不会从头部透出。
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(headerGradientEndDp)
                                        .drawBehind {
                                            val p = collapseProgress()
                                            if (p > 0.001f) {
                                                clipRect(bottom = size.height * p) {
                                                    drawRect(brush = headerCoverBrush)
                                                }
                                            }
                                        }
                                        .testTag("home_wide_header_cover"),
                                )
                                // 报告和工具条是覆盖层：只做图层位移，不改变分页区高度。
                                // 背景统一由整窗渐变承担，这里不再额外叠一层底衬。
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        // 先量完整高度（含状态栏避让），卡片顶部留白才能
                                        // 与报告 + 控制条的真实底部对齐。
                                        .onSizeChanged { headerHeightPx = it.height }
                                        // 状态栏后面的背景由整窗渐变负责；内容避开状态栏，
                                        // 收起后卡片可以继续滚到状态栏后面。
                                        .windowInsetsPadding(
                                            WindowInsets.systemBars.only(
                                                WindowInsetsSides.Top,
                                            ),
                                        )
                                        .graphicsLayer {
                                            translationY = -headerHeightPx * collapseProgress()
                                        }
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
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (chromeVisible) {
            // 收合后的报告行直接落在不透明实色渐变头上，不再有独立底板/圆角票。
            WideCollapsedReportTicket(
                headlineCount = collapsedHeadline,
                tail = collapsedTail,
                tone = collapsedTone,
                syncText = syncLabel,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        start = railWidth,
                        top = systemBarTopDp,
                        end = systemBarEndDp,
                    )
                    .fillMaxWidth()
                    .height(headerBarHeight)
                    .graphicsLayer {
                        alpha = ((collapseProgress() - 0.45f) / 0.55f).coerceIn(0f, 1f)
                    }
                    .testTag("home_wide_collapsed_ticket"),
            )
            // 底部导航栏/手势条区域保持画布色，收合时也不跟着变琥珀。
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(systemBarBottomDp)
                    .background(colors.canvas)
                    .testTag("home_wide_nav_bar_tint"),
            )
        }

        WideDetailDrawer(
            selectedCard = requestedDetailCard,
            layoutMode = layoutMode,
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
    bottomInset: Dp,
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
    val contentPadding = PaddingValues(
        top = headerHeightDp + 2.dp,
        bottom = 12.dp + bottomInset,
    )
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
                        flat = true,
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
                        flat = true,
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

/**
 * 报告 + 密度控制条：报告在上，控制条在报告与卡片之间、右对齐。
 * 横屏/竖屏使用同一结构，密度开关始终横排，不再随更新状态改变方向。
 */
@Composable
private fun WideReportHeader(
    cards: List<CredentialCardUi>,
    syncText: String,
    orientation: WideReportOrientation,
    onLocate: (UUID) -> Unit,
    density: UsageCardDensity,
    onToggleDensity: () -> Unit,
    densityProgress: () -> Float,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        WideHomeHealthReport(
            cards = cards,
            syncText = syncText,
            orientation = orientation,
            onLocate = onLocate,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(WideHomeSpec.reportControlsHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            DensityModeSwitch(
                density = density,
                onToggle = onToggleDensity,
                progress = densityProgress,
            )
        }
    }
}

/** 收起后的单行短票：数量 + 结论 + 最近同步，与左轨 logo 行齐平。 */
@Composable
private fun WideCollapsedReportTicket(
    headlineCount: Int,
    tail: String,
    tone: HomeHealthTone,
    syncText: String,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$headlineCount 个凭证",
            style = MyTokenType.label,
            fontWeight = FontWeight.SemiBold,
            color = colors.reportTicketInk,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = tail,
            style = MyTokenType.caption,
            fontWeight = FontWeight.Medium,
            color = severityAccent(
                tone,
                colors.reportTicketMuted,
                colors.reportNavCritical,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "最近同步 ${reportSyncLabel(syncText)}",
            style = MyTokenType.caption,
            color = colors.reportTicketMuted,
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
    onUpdateClick: () -> Unit,
    railWidth: Dp,
    collapseProgress: () -> Float,
) {
    val colors = MyTokenThemeTokens.colors
    val darkTheme = colors.canvas.luminance() < 0.5f
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
            // 背景透明，让整窗渐变一直铺到左轨；与内容之间不再画竖向分割线。
            .windowInsetsPadding(
                WindowInsets.systemBars.only(
                    WindowInsetsSides.Start +
                        WindowInsetsSides.Top +
                        WindowInsetsSides.Bottom,
                ),
            )
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
            Box(modifier = Modifier.size(30.dp)) {
                if (darkTheme) {
                    MyTokenBrandMark(
                        size = 30.dp,
                        modifier = Modifier
                            .graphicsLayer { alpha = 1f - collapseProgress() }
                            .testTag("home_wide_rail_brand"),
                    )
                    // 琥珀底上用深色标，保证收起后仍可读。
                    MyTokenBrandMark(
                        size = 30.dp,
                        darkTheme = false,
                        modifier = Modifier
                            .graphicsLayer { alpha = collapseProgress() }
                            .testTag("home_wide_rail_brand_amber"),
                    )
                } else {
                    MyTokenBrandMark(
                        size = 30.dp,
                        modifier = Modifier.testTag("home_wide_rail_brand"),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(1.dp)
                .background(colors.line)
                .testTag("home_wide_rail_brand_divider"),
        )
        Spacer(modifier = Modifier.height(4.dp))
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
                AllProvidersGlyph(selected = selectedProvider == null)
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
                    // 供应商图标直接落在整窗渐变上，不垫白色底板。
                    Image(
                        painter = painterResource(ProviderCatalog.logoRes(provider)),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(30.dp),
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
        if (hasUpdateAvailable) {
            // 更新提示移到左轨刷新按钮上方，只有小箭头做呼吸动画。
            RailActionButton(
                contentDescription = "有新版本，查看更新",
                tag = "home_wide_rail_update",
                onClick = onUpdateClick,
            ) {
                BreathingUpdateArrow(
                    tint = colors.actionFill,
                    modifier = Modifier.size(20.dp),
                    motionEnabled = motionEnabled,
                )
            }
        }
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
            contentDescription = "打开设置",
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

/** “全部供应商”图标：直接显示 2×2 网格（app.grid.2x2），不再垫白色底板。 */
@Composable
private fun AllProvidersGlyph(
    selected: Boolean,
) {
    val colors = MyTokenThemeTokens.colors
    Box(
        modifier = Modifier
            .size(30.dp)
            .testTag("home_wide_rail_all_icon"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.GridView,
            contentDescription = null,
            tint = if (selected) colors.ink else colors.muted,
            modifier = Modifier
                .size(20.dp)
                .testTag("home_wide_rail_all_grid_icon"),
        )
    }
}

/** 更新图标：与设置页“检测更新”的 ArrowCircleUp 一致，只让图标呼吸。 */
@Composable
private fun BreathingUpdateArrow(
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    motionEnabled: Boolean,
) {
    val pulse = if (motionEnabled) {
        val transition = rememberInfiniteTransition(label = "wide-rail-update-breath")
        transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1_400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "wide-rail-update-arrow",
        )
    } else {
        null
    }
    Icon(
        imageVector = Icons.Outlined.ArrowCircleUp,
        contentDescription = null,
        tint = tint,
        modifier = modifier.graphicsLayer {
            val p = pulse?.value ?: 1f
            alpha = 0.45f + 0.55f * p
            val scale = 0.9f + 0.1f * p
            scaleX = scale
            scaleY = scale
        },
    )
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
            // 不再垫选中/未选中底板：图标直接落在整窗渐变上，只保留左侧选中指示条。
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

/** 从 Compose 的 LocalView context 找到宿主 Activity，用于状态栏图标明暗。 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** 全窗渐变的位图缓存；只在窗口尺寸或头部高度变化时重建。 */
private class WideGradientBitmapCache {
    var bitmap: ImageBitmap? = null
    var size: IntSize = IntSize.Zero
    var headerPx: Float = -1f
}
