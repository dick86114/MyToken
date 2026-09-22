package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenAdaptiveContent
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.adaptiveGridColumns
import ai.routin.mytoken.core.ui.maxColumns

import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageCardDensity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import ai.routin.mytoken.core.ui.GlassButton
import ai.routin.mytoken.core.ui.GlassButtonTone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.UUID

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
    layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact,
    usageCardDensity: UsageCardDensity = UsageCardDensity.FULL,
    onToggleUsageCardDensity: () -> Unit = {},
) {
    var selectedProvider by remember { mutableStateOf<ProviderId?>(null) }
    val allCards = remember(state.cards) {
        state.cards.filter { it.status != ai.routin.mytoken.domain.usage.RefreshStatus.Disabled }
    }
    val providerCounts = remember(allCards) {
        allCards.groupingBy { it.credential.providerId }.eachCount()
    }
    val visibleProviders = remember(state.groups, providerCounts) {
        state.groups.map { it.providerId }.filter { (providerCounts[it] ?: 0) > 0 }
    }
    val filterOptions = remember(providerCounts, visibleProviders) {
        buildList {
            add(
                ProviderFilterOption(
                    providerId = null,
                    title = "全部",
                    count = providerCounts.values.sum(),
                )
            )
            visibleProviders.forEach { provider ->
                add(
                    ProviderFilterOption(
                        providerId = provider,
                        title = ProviderCatalog.displayName(provider),
                        count = providerCounts[provider] ?: 0,
                    )
                )
            }
        }
    }
    val visibleCards = remember(allCards, selectedProvider) {
        selectedProvider
            ?.let { provider -> allCards.filter { it.credential.providerId == provider } }
            ?: allCards
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "账户用量",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${visibleCards.size} 个凭证",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleUsageCardDensity,
                        modifier = Modifier.testTag("usage_density_toggle"),
                    ) {
                        if (usageCardDensity.isCompact()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = "切换到完整模式",
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Dashboard,
                                contentDescription = "切换到简洁模式",
                            )
                        }
                    }
                    ProviderFilterMenu(
                        selectedProvider = selectedProvider,
                        options = filterOptions,
                        onSelect = { selectedProvider = it },
                    )
                    IconButton(onClick = onRefreshAll) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = "刷新全部")
                    }
                },
            )
        },
    ) { padding ->
        val contentModifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
        when {
            state.isLoading -> Box(
                modifier = contentModifier,
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            allCards.isEmpty() -> EmptyState(
                modifier = contentModifier,
                onImportFromMac = onImportFromMac,
                onAddManually = onAddManually,
            )

            else -> PullToRefreshBox(
                isRefreshing = state.isRefreshingAll,
                onRefresh = onRefreshAll,
                modifier = contentModifier.testTag("home_pull_refresh"),
            ) {
                MyTokenAdaptiveContent(
                    maxWidth = 1440.dp,
                    horizontalPadding = 16.dp,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val columns = adaptiveGridColumns(
                            availableWidth = maxWidth,
                            maxColumns = layoutMode.maxColumns(1, 2, 3),
                            // 220dp 让展开折叠屏（~600dp）保持 2 列，只有大平板（~840dp+）才到 3 列。
                            minItemWidth = if (usageCardDensity.isCompact()) 220.dp else 260.dp,
                            spacing = if (usageCardDensity.isCompact()) 8.dp else 12.dp,
                        )
                        val metricColumns = 2
                        LazyVerticalStaggeredGrid(
                            state = rememberLazyStaggeredGridState(),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("home_grid_${columns}_columns"),
                            columns = StaggeredGridCells.Fixed(columns),
                            contentPadding = PaddingValues(
                                top = 4.dp,
                                bottom = if (layoutMode == MyTokenLayoutMode.Compact) 96.dp else 24.dp,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalItemSpacing = if (usageCardDensity.isCompact()) 8.dp else 12.dp,
                        ) {
                            staggeredItems(
                                visibleCards,
                                key = { it.credential.id },
                                contentType = { "credential-usage-card" },
                            ) { card ->
                                CredentialUsageCard(
                                    card = card,
                                    metricColumns = metricColumns,
                                    layoutMode = layoutMode,
                                    usageCardDensity = usageCardDensity,
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

private data class ProviderFilterOption(
    val providerId: ProviderId?,
    val title: String,
    val count: Int,
) {
    val menuTitle: String get() = "$title（$count）"
}

@Composable
private fun ProviderFilterMenu(
    selectedProvider: ProviderId?,
    options: List<ProviderFilterOption>,
    onSelect: (ProviderId?) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val selectedTitle = if (selectedProvider == null) {
        "供应商：全部"
    } else {
        val title = options.firstOrNull { it.providerId == selectedProvider }?.title
            ?: ProviderCatalog.displayName(selectedProvider)
        "供应商：$title"
    }

    Box {
        TextButton(
            onClick = { isExpanded = true },
            modifier = Modifier.testTag("provider_filter_menu"),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = selectedTitle,
                modifier = Modifier.testTag("provider_filter_selected"),
                style = MaterialTheme.typography.labelLarge,
            )
            Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
        }

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            options.forEach { option ->
                val providerId = option.providerId
                DropdownMenuItem(
                    text = { Text(text = option.menuTitle) },
                    onClick = {
                        onSelect(providerId)
                        isExpanded = false
                    },
                    modifier = providerId?.let { provider ->
                        Modifier.testTag("provider_filter_item_${provider.rawValue}")
                    } ?: Modifier,
                )
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
        Text(
            text = "尚未配置 Key",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "从 Mac 迁移，或手动添加一个 plan Key",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))
        GlassButton(
            onClick = onImportFromMac,
            tone = GlassButtonTone.Primary,
            text = "从 Mac 导入",
        )
        Spacer(modifier = Modifier.height(8.dp))
        GlassButton(
            onClick = onAddManually,
            text = "手动添加",
        )
    }
}
