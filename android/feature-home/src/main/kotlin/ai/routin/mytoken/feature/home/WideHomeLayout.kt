package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.alerts.AlertThresholds
import ai.routin.mytoken.core.ui.MyTokenBrandMark
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenMotion
import ai.routin.mytoken.core.ui.MyTokenRadius
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.ProviderLogoPlate
import ai.routin.mytoken.core.ui.rememberMyTokenMotionEnabled
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageCardDensity
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 大屏三栏：左侧供应商轨 / 中间首页 / 右侧详情。
 *
 * 中间完整保留手机首页的凭证报告、供应商行与密度切换、瀑布流卡片和管理凭证入口；
 * 详情直接复用当前详情页区块。只有窗口进入 medium / expanded 时才走这里。
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
    updateVersion: String?,
    onOpenUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    val motionEnabled = rememberMyTokenMotionEnabled()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyStaggeredGridState()

    var highlightedCredentialId by remember { mutableStateOf<UUID?>(null) }
    var flashTick by remember { mutableIntStateOf(0) }
    var pendingLocateId by remember { mutableStateOf<UUID?>(null) }
    var deleteTarget by remember { mutableStateOf<CredentialCardUi?>(null) }
    // 关闭动画期间保留上一张详情，避免宽度还没收完内容就消失。
    var lastDetailCard by remember { mutableStateOf<CredentialCardUi?>(null) }

    // 比例取整窗尺寸：Configuration 的可用高度不含系统栏，补回后才不会把 4:3 推成 16:9。
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
    val railWidth = WideHomeSpec.railWidth(layoutMode)
    val reportOrientation = WideHomeSpec.reportOrientation(aspectRatio)
    val openDetailWidth = WideHomeSpec.detailWidth(aspectRatio)
    val detailOpen = selectedDetailId != null
    val reflowEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    // 中间栏按 200ms 让位；详情内容按 260/240ms 从右挤入/退出。
    val middleInset by animateDpAsState(
        targetValue = if (detailOpen) openDetailWidth else 0.dp,
        animationSpec = tween(
            durationMillis = MyTokenMotion.detailReflowDurationMillis(motionEnabled),
            easing = reflowEasing,
        ),
        label = "wideMiddleInset",
    )
    val detailSlide by animateDpAsState(
        targetValue = if (detailOpen) 0.dp else openDetailWidth,
        animationSpec = tween(
            durationMillis = if (detailOpen) {
                MyTokenMotion.detailOpenDurationMillis(motionEnabled)
            } else {
                MyTokenMotion.detailCloseDurationMillis(motionEnabled)
            },
            easing = reflowEasing,
        ),
        label = "wideDetailSlide",
    )
    val detailRevealWidth = maxOf(middleInset, openDetailWidth - detailSlide)
        .coerceAtLeast(0.dp)

    val requestedDetailCard = allCards.firstOrNull {
        it.credential.id == selectedDetailId
    }
    LaunchedEffect(requestedDetailCard) {
        if (requestedDetailCard != null) lastDetailCard = requestedDetailCard
    }
    // 凭证被删除或集合变化导致目标消失时收起详情；加载中先等卡片恢复。
    LaunchedEffect(selectedDetailId, requestedDetailCard, state.isLoading) {
        if (!state.isLoading && selectedDetailId != null && requestedDetailCard == null) {
            onSelectDetail(null)
        }
    }
    val detailCard = requestedDetailCard ?: lastDetailCard

    fun locateNow(id: UUID) {
        val index = visibleCards.indexOfFirst { it.credential.id == id }
        if (index < 0) return
        scope.launch {
            locateInStaggeredGrid(
                state = gridState,
                index = index,
                centerBiasPx = 0f,
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

    // 筛选变化后回到顶部，避免沿用上一份列表的滚动位置。
    LaunchedEffect(selectedProvider) {
        gridState.scrollToItem(0)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = middleInset),
        ) {
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
                    .background(colors.canvas),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("home_page_active"),
                ) {
                    if (hasUpdateAvailable) {
                        HomeUpdateBanner(
                            version = updateVersion,
                            onOpenUpdate = onOpenUpdate,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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

                        else -> {
                            WideHomeHealthReport(
                                cards = allCards,
                                syncText = state.lastUpdatedText ?: "刚刚同步",
                                orientation = reportOrientation,
                                onLocate = ::requestLocate,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            WideSectionRow(
                                selectedProvider = selectedProvider,
                                providers = providers,
                                counts = providerCounts,
                                allTotal = allCards.size,
                                onSelectProvider = {
                                    pendingLocateId = null
                                    onSelectProvider(it)
                                },
                                density = usageCardDensity,
                                onToggleDensity = onToggleUsageCardDensity,
                                motionEnabled = motionEnabled,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            WideCardGrid(
                                cards = visibleCards,
                                layoutMode = layoutMode,
                                density = usageCardDensity,
                                selectedDetailId = selectedDetailId,
                                highlightedCredentialId = highlightedCredentialId,
                                flashTick = flashTick,
                                gridState = gridState,
                                fontScale = density.fontScale,
                                motionEnabled = motionEnabled,
                                onOpenCard = { id ->
                                    onSelectDetail(if (selectedDetailId == id) null else id)
                                },
                                onRefreshCard = onRefreshCredential,
                                onRetryCard = onRetryCredential,
                                onManageCredentials = onManageCredentials,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
        if (detailRevealWidth > 0.dp && detailCard != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(detailRevealWidth)
                    .fillMaxHeight()
                    .clipToBounds()
                    .background(colors.surface)
                    .drawBehind {
                        drawLine(
                            color = colors.line,
                            start = Offset(0.5f, 0f),
                            end = Offset(0.5f, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    .semantics {
                        contentDescription = "凭证详情，${detailCard.credential.name}"
                    }
                    .testTag("home_wide_detail"),
            ) {
                Box(
                    modifier = Modifier
                        .width(openDetailWidth)
                        .fillMaxHeight()
                        .offset(x = detailSlide)
                        .align(Alignment.CenterEnd),
                ) {
                    // 切换卡片时重置滚动位置与分享弹窗，详情从顶部开始。
                    key(detailCard.credential.id) {
                        CredentialDetailPane(
                            card = detailCard,
                            layoutMode = layoutMode,
                            globalAlertThresholds = globalAlertThresholds,
                            alertThresholdOverride =
                                alertThresholdOverrides[detailCard.credential.id],
                            onClose = { onSelectDetail(null) },
                            onRefresh = { onRefreshCredential(detailCard.credential.id) },
                            onEdit = { onEditCredential(detailCard.credential.id) },
                            onDelete = { deleteTarget = detailCard },
                        )
                    }
                }
            }
        }
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

/** 左侧 72/64dp 图标轨：品牌、全部、供应商，底部固定刷新/设置/凭证管理。 */
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
    val spinTransition = rememberInfiniteTransition(label = "wide-rail-refresh")
    val spin by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "wide-rail-refresh-spin",
    )

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
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(colors.controlHandle)
                        .border(1.dp, colors.line, RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GridView,
                        contentDescription = null,
                        tint = if (selectedProvider == null) colors.ink else colors.muted,
                        modifier = Modifier.size(17.dp),
                    )
                }
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
                    .graphicsLayer {
                        rotationZ = if (isRefreshing && motionEnabled) spin else 0f
                    },
            )
        }
        Box {
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
            if (hasUpdateAvailable) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 3.dp, end = 1.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(colors.reportTicket)
                        .border(1.dp, colors.surface, CircleShape)
                        .testTag("home_update_badge"),
                )
            }
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

/** 中间分区行：沿用现有供应商筛选菜单与密度切换，不重复放设置齿轮。 */
@Composable
private fun WideSectionRow(
    selectedProvider: ProviderId?,
    providers: List<ProviderId>,
    counts: Map<ProviderId, Int>,
    allTotal: Int,
    onSelectProvider: (ProviderId?) -> Unit,
    density: UsageCardDensity,
    onToggleDensity: () -> Unit,
    motionEnabled: Boolean,
) {
    val densityProgress by animateFloatAsState(
        targetValue = if (density.isCompact()) 0f else 1f,
        animationSpec = tween(
            durationMillis = MyTokenMotion.modeSwitchDurationMillis(motionEnabled),
        ),
        label = "wideDensityProgress",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .testTag("home_provider_toolbar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ProviderFilter(
            selectedProvider = selectedProvider,
            providers = providers,
            counts = counts,
            allTotal = allTotal,
            onSelect = onSelectProvider,
        )
        DensityModeSwitch(
            density = density,
            onToggle = onToggleDensity,
            progress = { densityProgress },
        )
    }
}

@Composable
private fun WideCardGrid(
    cards: List<CredentialCardUi>,
    layoutMode: MyTokenLayoutMode,
    density: UsageCardDensity,
    selectedDetailId: UUID?,
    highlightedCredentialId: UUID?,
    flashTick: Int,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
    fontScale: Float,
    motionEnabled: Boolean,
    onOpenCard: (UUID) -> Unit,
    onRefreshCard: (UUID) -> Unit,
    onRetryCard: (UUID) -> Unit,
    onManageCredentials: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = WideHomeSpec.columnCount(maxWidth, fontScale)
        val filterMillis = MyTokenMotion.filterDurationMillis(motionEnabled)
        LazyVerticalStaggeredGrid(
            state = gridState,
            columns = StaggeredGridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxSize()
                .testTag(homeGridTag(HomeScrollMode.CompactGrid, columns)),
            contentPadding = PaddingValues(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(WideHomeSpec.cardSpacing),
            verticalItemSpacing = WideHomeSpec.cardSpacing,
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
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(durationMillis = filterMillis),
                        placementSpec = tween(durationMillis = filterMillis),
                        fadeOutSpec = tween(durationMillis = filterMillis),
                    ),
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
    }
}
