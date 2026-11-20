package ai.routin.mytoken.feature.home

import android.animation.ValueAnimator
import ai.routin.mytoken.core.ui.MyTokenButton
import ai.routin.mytoken.core.ui.MyTokenButtonTone
import ai.routin.mytoken.core.ui.MyTokenHelpButton
import ai.routin.mytoken.core.ui.HelpMarker
import ai.routin.mytoken.core.ui.HelpRow
import ai.routin.mytoken.core.ui.HelpSection
import ai.routin.mytoken.core.ui.MyTokenAdaptiveContent
import ai.routin.mytoken.core.ui.MyTokenBrandMark
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenPalette
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.core.ui.MyTokenVisualPolicy
import ai.routin.mytoken.core.ui.ProviderLogoPlate
import ai.routin.mytoken.core.ui.adaptiveGridColumns
import ai.routin.mytoken.core.ui.maxColumns
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageCardDensity
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.usage.RefreshStatus
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
)
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
    updateVersion: String? = null,
    layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact,
    usageCardDensity: UsageCardDensity = UsageCardDensity.FULL,
    onToggleUsageCardDensity: () -> Unit = {},
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
) {
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
    val scope = rememberCoroutineScope()
    var highlightedCredentialId by remember { mutableStateOf<UUID?>(null) }
    var flashTick by remember { mutableIntStateOf(0) }
    val firstCardItemIndex = if (hasUpdateAvailable) 3 else 2
    val fixedHeaderCenterBiasPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        28.dp.toPx()
    }

    Scaffold { padding ->
        val contentModifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
        when {
            state.isLoading -> Column(
                modifier = contentModifier.padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                HomeHeader(
                    credentialCount = state.cards.size,
                    usageCardDensity = usageCardDensity,
                    onToggleUsageCardDensity = onToggleUsageCardDensity,
                    onOpenCredentials = onOpenCredentials,
                    onOpenSettings = onOpenSettings,
                    hasUpdateAvailable = hasUpdateAvailable,
                )
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }

            allCards.isEmpty() -> Column(
                modifier = contentModifier.padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                HomeHeader(
                    credentialCount = state.cards.size,
                    usageCardDensity = usageCardDensity,
                    onToggleUsageCardDensity = onToggleUsageCardDensity,
                    onOpenCredentials = onOpenCredentials,
                    onOpenSettings = onOpenSettings,
                    hasUpdateAvailable = hasUpdateAvailable,
                )
                EmptyState(
                    modifier = Modifier.weight(1f),
                    onImportFromMac = onImportFromMac,
                    onAddManually = onAddManually,
                )
            }

            else -> MyTokenAdaptiveContent(
                maxWidth = 1440.dp,
                horizontalPadding = 16.dp,
                modifier = contentModifier,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    HomeHeader(
                        credentialCount = state.cards.size,
                        usageCardDensity = usageCardDensity,
                        onToggleUsageCardDensity = onToggleUsageCardDensity,
                        onOpenCredentials = onOpenCredentials,
                        onOpenSettings = onOpenSettings,
                        hasUpdateAvailable = hasUpdateAvailable,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshingAll,
                        onRefresh = onRefreshAll,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("home_pull_refresh"),
                    ) {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val columns = adaptiveGridColumns(
                            availableWidth = maxWidth,
                            maxColumns = layoutMode.maxColumns(1, 2, 3),
                            minItemWidth = if (usageCardDensity.isCompact()) 210.dp else 260.dp,
                            spacing = if (usageCardDensity.isCompact()) 8.dp else 12.dp,
                        )
                        LazyVerticalStaggeredGrid(
                            state = gridState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("home_grid_${columns}_columns"),
                            columns = StaggeredGridCells.Fixed(columns),
                            contentPadding = PaddingValues(
                                top = 12.dp,
                                bottom = 24.dp,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalItemSpacing = if (usageCardDensity.isCompact()) 8.dp else 12.dp,
                        ) {
                            if (hasUpdateAvailable) {
                                item(
                                    key = "home_update_banner",
                                    span = StaggeredGridItemSpan.FullLine,
                                ) {
                                    HomeUpdateBanner(
                                        version = updateVersion,
                                        onOpenSettings = onOpenSettings,
                                    )
                                }
                            }
                            item(
                                key = "home_health",
                                span = StaggeredGridItemSpan.FullLine,
                            ) {
                                HealthSummary(
                                    cards = visibleCards,
                                    lastUpdatedText = state.lastUpdatedText ?: "刚刚同步",
                                    onLocate = { id ->
                                        val index = visibleCards.indexOfFirst {
                                            it.credential.id == id
                                        }
                                        if (index >= 0) {
                                            scope.launch {
                                                gridState.animateToItemCentered(
                                                    firstCardItemIndex + index,
                                                    fixedHeaderCenterBiasPx,
                                                )
                                                highlightedCredentialId = id
                                                flashTick += 1
                                                delay(1_000)
                                                if (highlightedCredentialId == id) {
                                                    highlightedCredentialId = null
                                                }
                                            }
                                        }
                                    },
                                )
                            }
                            item(
                                key = "home_filters",
                                span = StaggeredGridItemSpan.FullLine,
                            ) {
                                ProviderFilterRow(
                                    selectedProvider = selectedProvider,
                                    providers = visibleProviders,
                                    counts = providerCounts,
                                    total = allCards.size,
                                    onSelect = { selectedProvider = it },
                                )
                            }
                            staggeredItems(
                                visibleCards,
                                key = { it.credential.id },
                                contentType = { "credential-usage-card" },
                            ) { card ->
                                val isHighlighted =
                                    highlightedCredentialId == card.credential.id
                                CredentialUsageCard(
                                    card = card,
                                    metricColumns = 2,
                                    layoutMode = layoutMode,
                                    usageCardDensity = usageCardDensity,
                                    highlightSignal = if (isHighlighted) flashTick else 0,
                                    onOpen = { onOpenCredential(card.credential.id) },
                                    onRefresh = { onRefreshCredential(card.credential.id) },
                                    onRetry = { onRetryCredential(card.credential.id) },
                                )
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    credentialCount: Int,
    usageCardDensity: UsageCardDensity,
    onToggleUsageCardDensity: () -> Unit,
    onOpenCredentials: () -> Unit,
    onOpenSettings: () -> Unit,
    hasUpdateAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("home_toolbar"),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(role = Role.Button, onClick = onOpenCredentials)
                .semantics {
                    contentDescription = "打开凭证列表，共 $credentialCount 个凭证"
                }
                .testTag("home_open_credentials"),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.FormatListBulleted,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
        DensitySwitch(
            density = usageCardDensity,
            onToggle = onToggleUsageCardDensity,
            modifier = Modifier.align(Alignment.Center),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
                .clickable(role = Role.Button, onClick = onOpenSettings)
                .semantics {
                    contentDescription = if (hasUpdateAvailable) {
                        "打开设置，有可用更新"
                    } else {
                        "打开设置"
                    }
                }
                .testTag("home_open_settings"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
            if (hasUpdateAvailable) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(MyTokenPalette.warning)
                        .testTag("home_update_badge"),
                )
            }
        }
    }
}

@Composable
private fun DensitySwitch(
    density: UsageCardDensity,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val compact = density.isCompact()
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val trackColor = if (dark) {
        Color.White.copy(alpha = 0.07f)
    } else {
        Color(0xFF17222C).copy(alpha = 0.055f)
    }

    BoxWithConstraints(
        modifier = modifier
            .testTag("usage_density_toggle")
            .width(156.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(trackColor)
            .padding(2.dp),
    ) {
        val optionWidth = maxWidth / 2
        val thumbOffset by animateDpAsState(
            targetValue = if (compact) 0.dp else optionWidth,
            animationSpec = spring(
                dampingRatio = 1f,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "densityThumbOffset",
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .selectableGroup(),
        ) {
            DensityOption(
                text = "简洁",
                compact = true,
                selected = compact,
                testTag = "usage_density_option_compact",
                modifier = Modifier.weight(1f),
                onClick = { if (!compact) onToggle() },
            )
            DensityOption(
                text = "完整",
                compact = false,
                selected = !compact,
                testTag = "usage_density_option_full",
                modifier = Modifier.weight(1f),
                onClick = { if (compact) onToggle() },
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(
                    x = thumbOffset + (optionWidth - 18.dp) / 2,
                    y = (-4).dp,
                )
                .width(18.dp)
                .height(2.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun DensityOption(
    text: String,
    compact: Boolean,
    selected: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "densityOptionColor",
    )
    Row(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics {
                contentDescription = if (compact) "简洁模式" else "完整模式"
                stateDescription = if (selected) "已选" else "未选"
            }
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        DensityGlyph(compact = compact, color = contentColor)
        Text(
            text = text,
            modifier = Modifier.padding(start = 4.dp),
            fontSize = 11.sp,
            lineHeight = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
        )
    }
}

@Composable
private fun DensityGlyph(compact: Boolean, color: Color) {
    Column(
        modifier = Modifier.size(14.dp),
        verticalArrangement = Arrangement.spacedBy(
            space = if (compact) 4.dp else 2.dp,
            alignment = Alignment.CenterVertically,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(if (compact) 2 else 3) {
            Box(
                modifier = Modifier
                    .width(if (compact) 9.dp else 11.dp)
                    .height(2.dp)
                    .background(color, RoundedCornerShape(1.dp)),
            )
        }
    }
}

/**
 * 首页更新提醒条：应用启动时会检查 GitHub Release，发现新版本后在首页顶部提示。
 * 点击进入设置页，由设置页的更新卡片负责下载与安装。
 */
@Composable
private fun HomeUpdateBanner(
    version: String?,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(role = Role.Button, onClick = onOpenSettings)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics { contentDescription = "有新版本可用，前往设置查看并更新" }
            .testTag("home_update_banner"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UpdateBannerGlyph()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 11.dp),
        ) {
            Text(
                text = version?.takeIf { it.isNotBlank() }?.let { "发现新版本 $it" } ?: "发现新版本",
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "前往设置查看更新内容并安装",
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
            )
        }
        Text(
            text = "去更新",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun UpdateBannerGlyph(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val twinkle = if (animationsEnabled) {
        rememberInfiniteTransition(label = "updateTwinkle").animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1_250, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "updateTwinklePhase",
        )
    } else {
        remember { mutableFloatStateOf(0.85f) }
    }

    Box(
        modifier = modifier
            .size(38.dp)
            .testTag("home_update_banner_icon"),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val phase = twinkle.value
            drawRoundRect(
                color = if (isDark) {
                    accent.copy(alpha = 0.16f + 0.06f * phase)
                } else {
                    Color.White.copy(alpha = 0.66f + 0.05f * phase)
                },
                cornerRadius = CornerRadius(12.dp.toPx()),
            )
            val glowRadius = size.minDimension * (0.38f + 0.08f * phase)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.20f + 0.18f * phase),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = center,
            )
        }
        Canvas(
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    val phase = twinkle.value
                    alpha = 0.84f + 0.16f * phase
                    scaleX = 0.94f + 0.08f * phase
                    scaleY = 0.94f + 0.08f * phase
                },
        ) {
            drawStar(
                center = center,
                outerRadius = size.minDimension * 0.46f,
                innerRadius = size.minDimension * 0.21f,
                color = accent,
            )
        }
    }
}

private fun DrawScope.drawStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color,
) {
    val star = Path().apply {
        repeat(10) { index ->
            val radius = if (index % 2 == 0) outerRadius else innerRadius
            val angle = (-90f + index * 36f) * PI.toFloat() / 180f
            val x = center.x + radius * cos(angle)
            val y = center.y + radius * sin(angle)
            if (index == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path = star, color = color)
}

@Composable
private fun HealthSummary(
    cards: List<CredentialCardUi>,
    lastUpdatedText: String,
    onLocate: (UUID) -> Unit,
) {
    val healthEntries = remember(cards) { cards.map { it to healthTone(it) } }
    val healthyCount = healthEntries.count { (_, tone) -> tone == HealthTone.Normal }
    val total = cards.size
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .testTag("health_summary"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "账户健康",
                style = MyTokenType.metricLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = healthyCount.toString(),
                fontSize = 24.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp,
            )
            Text(
                text = "/ $total",
                style = MyTokenType.metricLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp, top = 2.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SyncPill(text = lastUpdatedText)
                MyTokenHelpButton(
                    title = "账户健康",
                    subtitle = "按首页顺序排列，点击短条定位卡片",
                    sections = listOf(
                        HelpSection(
                            title = "颜色",
                            rows = listOf(
                                HelpRow(
                                    label = "正常",
                                    detail = "余量充足",
                                    marker = HelpMarker.Positive,
                                ),
                                HelpRow(
                                    label = "接近上限",
                                    detail = "建议留意",
                                    marker = HelpMarker.Warning,
                                ),
                                HelpRow(
                                    label = "已超限 / 不可用",
                                    detail = "建议处理",
                                    marker = HelpMarker.Critical,
                                ),
                            ),
                        ),
                        HelpSection(
                            title = "判定阈值",
                            rows = listOf(
                                HelpRow("通用进度", "80% 标红 · 50% 标橙"),
                                HelpRow("小米 MiMo", "90% 标红 · 70% 标橙"),
                                HelpRow("余额", "小于等于 0 标红"),
                            ),
                        ),
                    ),
                    modifier = Modifier.testTag("health_help"),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            healthEntries.forEachIndexed { index, (card, tone) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .clickable { onLocate(card.credential.id) }
                        .semantics {
                            contentDescription =
                                "${card.credential.name}，${healthToneLabel(tone)}"
                        }
                        .testTag("health_segment_$index"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    ProviderLogoPlate(
                        painter = painterResource(
                            ProviderCatalog.logoRes(card.credential.providerId),
                        ),
                        contentDescription = null,
                        size = 19.dp,
                        shape = RoundedCornerShape(5.dp),
                        contentPadding = 2.dp,
                        modifier = Modifier
                            .testTag("health_provider_logo_${card.credential.providerId.rawValue}"),
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(healthToneColor(tone)),
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncPill(text: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.045f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(4.5.dp)
                .background(MyTokenPalette.positiveLight, CircleShape),
        )
        Text(
            text = text,
            style = MyTokenType.sync,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProviderFilterRow(
    selectedProvider: ProviderId?,
    providers: List<ProviderId>,
    counts: Map<ProviderId, Int>,
    total: Int,
    onSelect: (ProviderId?) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ProviderChip(
            text = "全部 $total",
            selected = selectedProvider == null,
            testTag = "provider_filter_menu",
            onClick = { onSelect(null) },
        )
        providers.forEach { provider ->
            ProviderChip(
                text = "${ProviderCatalog.displayName(provider)} ${counts[provider] ?: 0}",
                selected = selectedProvider == provider,
                testTag = "provider_filter_item_${provider.rawValue}",
                onClick = { onSelect(provider) },
            )
        }
    }
}

@Composable
private fun ProviderChip(
    text: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    val container by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
        },
        label = "providerChipContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "providerChipContent",
    )
    Box(
        modifier = Modifier
            .heightIn(min = 30.dp)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick)
            .semantics { this.selected = selected }
            .testTag(testTag)
            .padding(horizontal = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MyTokenType.chip,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
            style = MaterialTheme.typography.titleLarge,
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

private suspend fun LazyStaggeredGridState.animateToItemCentered(
    index: Int,
    centerBiasPx: Float,
) {
    val viewportHeight = layoutInfo.viewportSize.height
    animateScrollToItem(
        index = index,
        scrollOffset = -(viewportHeight / 3),
    )
    withFrameNanos { }
    var item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (item == null) {
        delay(16)
        item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    }
    item ?: return

    val viewportCenter = layoutInfo.viewportSize.height / 2f
    val itemCenter = item.offset.y + item.size.height / 2f
    val delta = itemCenter - (viewportCenter - centerBiasPx)
    if (abs(delta) > 1f) {
        animateScrollBy(
            value = delta,
            animationSpec = tween(
                durationMillis = 240,
                easing = FastOutSlowInEasing,
            ),
        )
    }
}

private enum class HealthTone { Normal, Warning, Critical }

private fun healthTone(card: CredentialCardUi): HealthTone {
    if (card.status == RefreshStatus.Failed || card.error != null) return HealthTone.Critical
    val tones = card.snapshot?.metrics.orEmpty().mapNotNull(::metricHealthTone)
    return when {
        tones.any { it == HealthTone.Critical } -> HealthTone.Critical
        tones.any { it == HealthTone.Warning } -> HealthTone.Warning
        else -> HealthTone.Normal
    }
}

private fun metricHealthTone(metric: UsageMetric): HealthTone? = when (metric.healthState) {
    UsageMetricHealthState.Critical,
    UsageMetricHealthState.Unavailable -> HealthTone.Critical

    UsageMetricHealthState.Warning,
    UsageMetricHealthState.Stale -> HealthTone.Warning

    UsageMetricHealthState.Normal -> HealthTone.Normal
    UsageMetricHealthState.Unknown -> fallbackHealthTone(metric)
}

private fun fallbackHealthTone(metric: UsageMetric): HealthTone? {
    if (metric.presentation == UsageMetricPresentation.Progress) {
        val used = metric.used?.toDouble() ?: return null
        val limit = metric.limit?.toDouble() ?: return null
        if (limit <= 0.0) return HealthTone.Normal
        val percent = used / limit * 100.0
        return when {
            percent >= 80.0 -> HealthTone.Critical
            percent >= 50.0 -> HealthTone.Warning
            else -> HealthTone.Normal
        }
    }
    if (metric.presentation == UsageMetricPresentation.Balance) {
        return if ((metric.value ?: return null) <= java.math.BigDecimal.ZERO) {
            HealthTone.Critical
        } else {
            HealthTone.Normal
        }
    }
    return null
}

private fun healthToneColor(tone: HealthTone): Color = when (tone) {
    HealthTone.Normal -> MyTokenPalette.positiveLight
    HealthTone.Warning -> MyTokenPalette.warning
    HealthTone.Critical -> MyTokenPalette.critical
}

private fun healthToneLabel(tone: HealthTone): String = when (tone) {
    HealthTone.Normal -> "正常"
    HealthTone.Warning -> "需注意"
    HealthTone.Critical -> "已超限"
}
