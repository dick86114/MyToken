package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenAdaptiveContent
import ai.routin.mytoken.core.ui.MyTokenBrandMark
import ai.routin.mytoken.core.ui.MyTokenButton
import ai.routin.mytoken.core.ui.MyTokenButtonTone
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenRadius
import ai.routin.mytoken.core.ui.MyTokenMotion
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.rememberMyTokenMotionEnabled
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageCardDensity
import ai.routin.mytoken.domain.usage.RefreshStatus
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import java.util.UUID
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

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
    onOpenSettings: () -> Unit = {},
    hasUpdateAvailable: Boolean = false,
    layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact,
    usageCardDensity: UsageCardDensity = UsageCardDensity.FULL,
    onToggleUsageCardDensity: () -> Unit = {},
    homeScrollStates: HomeScrollStates = rememberHomeScrollStates(),
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

    val motionEnabled = rememberMyTokenMotionEnabled()
    val scope = rememberCoroutineScope()
    var highlightedCredentialId by remember { mutableStateOf<UUID?>(null) }
    var flashTick by remember { mutableIntStateOf(0) }
    var headerHeightPx by remember { mutableIntStateOf(0) }
    var collapsedPx by remember { mutableFloatStateOf(0f) }
    var pendingLocateId by remember { mutableStateOf<UUID?>(null) }

    val density = LocalDensity.current
    val topNavHeightPx = with(density) { 60.dp.toPx() }
    val toolbarHeightPx = with(density) { 64.dp.toPx() }
    val chromeVisible = !state.isLoading && allCards.isNotEmpty()
    val latestMaxCollapsePx by rememberUpdatedState(
        (headerHeightPx - toolbarHeightPx).coerceAtLeast(0f),
    )
    // 只响应用户手势：报告定位等程序化滚动应保持报告可见，不被折叠逻辑截走。
    val collapseConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput || available.y >= 0f) {
                    return Offset.Zero
                }
                val current = collapsedPx.coerceIn(0f, latestMaxCollapsePx)
                val remaining = latestMaxCollapsePx - current
                val consume = min(-available.y, remaining)
                if (consume <= 0f) return Offset.Zero
                collapsedPx = current + consume
                return Offset(0f, -consume)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput || available.y <= 0f) {
                    return Offset.Zero
                }
                val current = collapsedPx.coerceIn(0f, latestMaxCollapsePx)
                val consume = min(available.y, current)
                if (consume <= 0f) return Offset.Zero
                collapsedPx = current - consume
                return Offset(0f, consume)
            }
        }
    }
    LaunchedEffect(chromeVisible) {
        if (!chromeVisible) {
            collapsedPx = 0f
            headerHeightPx = 0
        }
    }

    val fontScale = density.fontScale
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val pagerState = rememberPagerState(
        initialPage = if (usageCardDensity == UsageCardDensity.COMPACT) 0 else 1,
        pageCount = { 2 },
    )
    val latestDensity by rememberUpdatedState(usageCardDensity)
    val latestToggleDensity by rememberUpdatedState(onToggleUsageCardDensity)

    // 手势拖动由 Pager 自然跟手；只有真正停稳后才把落点页同步回持久化的模式。
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val settled =
                    if (page == 0) UsageCardDensity.COMPACT else UsageCardDensity.FULL
                if (settled != latestDensity) latestToggleDensity()
            }
    }

    // 顶部按钮切换模式时，让整页平滑滑到目标页而不是直接跳变；同时覆盖
    // 连续点击（动画进行中反向点击）的情况，targetPage 不同就重新定向。
    LaunchedEffect(usageCardDensity) {
        val target = if (usageCardDensity == UsageCardDensity.COMPACT) 0 else 1
        if (pagerState.settledPage != target || pagerState.targetPage != target) {
            if (motionEnabled) pagerState.animateScrollToPage(target) else pagerState.scrollToPage(target)
        }
    }

    fun locateNow(id: UUID) {
        val index = visibleCards.indexOfFirst { it.credential.id == id }
        if (index < 0) return
        scope.launch {
            locateCard(
                states = homeScrollStates,
                mode = homeScrollMode(usageCardDensity, screenWidthDp),
                index = index,
                // 顶部导航 + 报告 + 筛选工具条都在分页区上方；补偿它们总高度的一半，
                // 卡片中心才会落在整块屏幕（而非分页区）的中心。
                centerBiasPx = (topNavHeightPx + headerHeightPx) / 2f,
                durationMillis = MyTokenMotion.locateDurationMillis(motionEnabled),
            )
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
            val contentPadding = PaddingValues(top = 2.dp, bottom = 12.dp)
            val spacing = if (pageDensity.isCompact()) 8.dp else 12.dp
            PullToRefreshBox(
                isRefreshing = state.isRefreshingAll,
                onRefresh = onRefreshAll,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_pull_refresh"),
            ) {
                when (pageMode) {
                    HomeScrollMode.FullList -> LazyColumn(
                        state = homeScrollStates.full,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(collapseConnection)
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
                            CredentialEndActions(
                                onAdd = onAddManually,
                                onManage = onOpenCredentials,
                            )
                        }
                    }

                    HomeScrollMode.FullWideGrid -> LazyVerticalGrid(
                        state = homeScrollStates.fullWide,
                        columns = GridCells.Fixed(pageColumns),
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(collapseConnection)
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
                            CredentialEndActions(
                                onAdd = onAddManually,
                                onManage = onOpenCredentials,
                            )
                        }
                    }

                    // 简洁卡高度随内容自适应，用瀑布流避免相邻列因行数不同而对不齐。
                    HomeScrollMode.CompactGrid -> LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(pageColumns),
                        state = homeScrollStates.compact,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(collapseConnection)
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
                            CredentialEndActions(
                                onAdd = onAddManually,
                                onManage = onOpenCredentials,
                            )
                        }
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
        MyTokenAdaptiveContent(
            maxWidth = 1440.dp,
            horizontalPadding = 16.dp,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(colors.canvas),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HomeToolbar(
                    credentialCount = state.cards.size,
                    onOpenCredentials = onOpenCredentials,
                    onOpenSettings = onOpenSettings,
                    hasUpdateAvailable = hasUpdateAvailable,
                )
                if (chromeVisible) {
                    // 报告和筛选工具条一起上移；只收起报告高度，工具条最终停在导航下方。
                    Layout(
                        content = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged { headerHeightPx = it.height },
                            ) {
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
                                    onToggleDensity = onToggleUsageCardDensity,
                                    densityProgress = {
                                        (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                                            .coerceIn(0f, 1f)
                                    },
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clipToBounds()
                            // 报告本身不可滚动：在头部直接上/下拖也能收起或展开，
                            // 与在卡片列表上的纵向手势保持一致。
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { change, dragAmount ->
                                    val max = latestMaxCollapsePx
                                    if (max <= 0f) return@detectVerticalDragGestures
                                    val current = collapsedPx.coerceIn(0f, max)
                                    val delta = if (dragAmount < 0f) {
                                        min(-dragAmount, max - current)
                                    } else {
                                        -min(dragAmount, current)
                                    }
                                    if (delta != 0f) {
                                        collapsedPx = current + delta
                                        change.consume()
                                    }
                                }
                            }
                            .testTag("home_collapsing_header"),
                    ) { measurables, constraints ->
                        val content = measurables.first().measure(
                            constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity),
                        )
                        val maxCollapse = (content.height - toolbarHeightPx).coerceAtLeast(0f)
                        val collapse = collapsedPx.coerceIn(0f, maxCollapse).roundToInt()
                        layout(constraints.maxWidth, (content.height - collapse).coerceAtLeast(0)) {
                            content.place(0, -collapse)
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("home_pager"),
                    key = { page -> page },
                ) { page ->
                    val pageDensity = if (page == 0) {
                        UsageCardDensity.COMPACT
                    } else {
                        UsageCardDensity.FULL
                    }
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
        }
    }
}

/** 卡片末尾的凭证入口：新增直接进新增页，管理打开现有凭证列表。 */
@Composable
private fun CredentialEndActions(
    onAdd: () -> Unit,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    val shape = RoundedCornerShape(MyTokenRadius.card)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape),
    ) {
        CredentialEndRow(
            title = "新增凭证",
            supportingText = "把其他账户也放到这里",
            emphasized = true,
            onClick = onAdd,
            testTag = "home_add_credential",
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = colors.canvas,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
        CredentialEndRow(
            title = "管理已有凭证",
            supportingText = "启停、编辑与调整顺序",
            emphasized = false,
            onClick = onManage,
            testTag = "home_manage_credentials",
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ListAlt,
                    contentDescription = null,
                    tint = colors.muted,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
    }
}

@Composable
private fun CredentialEndRow(
    title: String,
    supportingText: String,
    emphasized: Boolean,
    onClick: () -> Unit,
    testTag: String,
    icon: @Composable () -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "$title，$supportingText" }
            .testTag(testTag)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (emphasized) colors.ink else colors.subtle),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MyTokenType.label,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
            )
            Text(
                text = supportingText,
                modifier = Modifier.padding(top = 2.dp),
                style = MyTokenType.caption,
                color = colors.muted,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun EmptyState(
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
