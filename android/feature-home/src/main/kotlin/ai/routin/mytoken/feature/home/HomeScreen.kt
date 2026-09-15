package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenAdaptiveContent
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.adaptiveGridColumns
import ai.routin.mytoken.core.ui.maxColumns

import ai.routin.mytoken.domain.model.ProviderId
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import ai.routin.mytoken.core.ui.GlassButton
import ai.routin.mytoken.core.ui.GlassButtonTone
import ai.routin.mytoken.core.ui.glassFilterChipBorder
import ai.routin.mytoken.core.ui.glassFilterChipColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
    onOpenCredential: (UUID) -> Unit,
    onImportFromMac: () -> Unit,
    onAddManually: () -> Unit,
    layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact,
) {
    var selectedProvider by remember { mutableStateOf<ProviderId?>(null) }
    val allCards = remember(state.cards) {
        state.cards.filter { it.status != ai.routin.mytoken.domain.usage.RefreshStatus.Disabled }
    }
    val visibleProviders = remember(state.groups) { state.groups.map { it.providerId } }
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
                            minItemWidth = 260.dp,
                            spacing = 12.dp,
                        )
                        val metricColumns = if (columns == 1) 2 else 1
                        LazyVerticalGrid(
                            state = rememberLazyGridState(),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("home_grid_${columns}_columns"),
                            columns = GridCells.Fixed(columns),
                            contentPadding = PaddingValues(
                                top = 4.dp,
                                bottom = if (layoutMode == MyTokenLayoutMode.Compact) 96.dp else 24.dp,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item(
                                key = "provider_filters",
                                span = { GridItemSpan(maxLineSpan) },
                            ) {
                                ProviderFilters(
                                    selectedProvider = selectedProvider,
                                    visibleProviders = visibleProviders,
                                    onSelect = { selectedProvider = it },
                                )
                            }
                            gridItems(
                                visibleCards,
                                key = { it.credential.id },
                                contentType = { "credential-usage-card" },
                            ) { card ->
                                CredentialUsageCard(
                                    card = card,
                                    metricColumns = metricColumns,
                                    onOpen = { onOpenCredential(card.credential.id) },
                                    onRetry = { onRefreshCredential(card.credential.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderFilters(
    selectedProvider: ProviderId?,
    visibleProviders: List<ProviderId>,
    onSelect: (ProviderId?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProviderFilterChips(
            selectedProvider = selectedProvider,
            visibleProviders = visibleProviders,
            onSelect = onSelect,
        )
    }
}

@Composable
private fun ProviderFilterChips(
    selectedProvider: ProviderId?,
    visibleProviders: List<ProviderId>,
    onSelect: (ProviderId?) -> Unit,
) {
    FilterChip(
        selected = selectedProvider == null,
        onClick = { onSelect(null) },
        label = { Text(text = "全部") },
        colors = glassFilterChipColors(selected = selectedProvider == null),
        border = glassFilterChipBorder(selected = selectedProvider == null),
    )
    visibleProviders.forEach { provider ->
        FilterChip(
            selected = selectedProvider == provider,
            onClick = {
                onSelect(if (selectedProvider == provider) null else provider)
            },
            label = { Text(text = ProviderCatalog.displayName(provider)) },
            colors = glassFilterChipColors(selected = selectedProvider == provider),
            border = glassFilterChipBorder(selected = selectedProvider == provider),
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
