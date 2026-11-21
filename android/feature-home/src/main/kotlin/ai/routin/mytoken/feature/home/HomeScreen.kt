package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenAdaptiveContent
import ai.routin.mytoken.core.ui.MyTokenBrandMark
import ai.routin.mytoken.core.ui.MyTokenButton
import ai.routin.mytoken.core.ui.MyTokenButtonTone
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenSize
import ai.routin.mytoken.core.ui.MyTokenMotion
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.rememberMyTokenMotionEnabled
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageCardDensity
import ai.routin.mytoken.domain.usage.RefreshStatus
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.UUID
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
    var pendingLocateId by remember { mutableStateOf<UUID?>(null) }

    val fontScale = LocalDensity.current.fontScale
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
                // 报告与筛选工具条固定在分页区上方，补偿它们高度的一半。
                centerBiasPx = headerHeightPx / 2f,
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
            val contentPadding = PaddingValues(top = 2.dp, bottom = 24.dp)
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
                            .testTag(pageTag),
                        contentPadding = contentPadding,
                        verticalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        items(
                            items = visibleCards,
                            key = { it.credential.id },
                            contentType = { "credential-usage-card" },
                        ) { card -> cardItem(card, pageDensity) }
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { headerHeightPx = it.height },
                ) {
                    HomeToolbar(
                        credentialCount = state.cards.size,
                        onOpenCredentials = onOpenCredentials,
                        onOpenSettings = onOpenSettings,
                        hasUpdateAvailable = hasUpdateAvailable,
                    )
                    if (!state.isLoading && allCards.isNotEmpty()) {
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
