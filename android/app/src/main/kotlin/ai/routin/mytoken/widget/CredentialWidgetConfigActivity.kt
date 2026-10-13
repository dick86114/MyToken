package ai.routin.mytoken.widget

import ai.routin.mytoken.MyTokenApplication
import ai.routin.mytoken.core.ui.MyTokenTheme
import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.feature.home.ProviderCatalog
import ai.routin.mytoken.feature.settings.AppThemeMode
import ai.routin.mytoken.feature.settings.DisplaySettings
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class CredentialWidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(Activity.RESULT_CANCELED)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val graph = (application as MyTokenApplication).graph
        val store = WidgetConfigStore(this)
        val widgetInfo = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)
        val kind = if (
            widgetInfo?.provider?.className?.endsWith("OverviewWidgetProvider") == true
        ) {
            WidgetKind.Overview
        } else {
            WidgetKind.Credential
        }
        val initialConfig = store.load(appWidgetId, kind)

        setContent {
            val displaySettings by graph.displaySettingsStore.settings
                .collectAsState(initial = DisplaySettings())
            val darkTheme = when (displaySettings.themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
            val credentials by remember { graph.credentialRepository.observeCredentials() }
                .collectAsState(initial = emptyList())
            val scope = rememberCoroutineScope()
            val saveUseCase = remember(kind, appWidgetId) {
                WidgetConfigurationSaveUseCase(
                    save = store::save,
                    enqueueRefresh = {
                        WidgetRefresher.enqueue(this@CredentialWidgetConfigActivity)
                    },
                    refreshNow = { config ->
                        withTimeoutOrNull(15_000) {
                            when (kind) {
                                WidgetKind.Credential -> config.credentialId
                                    ?.let { id -> credentials.firstOrNull { it.id == id } }
                                    ?.let { credential -> graph.refreshUseCase.refresh(credential) }
                                WidgetKind.Overview -> graph.refreshUseCase.refreshAll()
                            }
                        }
                    },
                    updateWidget = { widgetId ->
                        val widget: GlanceAppWidget = when (kind) {
                            WidgetKind.Credential -> CredentialGlanceWidget()
                            WidgetKind.Overview -> OverviewGlanceWidget()
                        }
                        widget.update(
                            this@CredentialWidgetConfigActivity,
                            GlanceAppWidgetManager(this@CredentialWidgetConfigActivity)
                                .getGlanceIdBy(widgetId),
                        )
                    },
                )
            }

            MyTokenTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WidgetConfigurationScreen(
                        kind = kind,
                        initialConfig = initialConfig,
                        credentials = credentials.filter(Credential::isEnabled),
                        onSave = { config ->
                            scope.launch {
                                saveUseCase.apply(appWidgetId, config)
                                setResult(
                                    Activity.RESULT_OK,
                                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                                )
                                finish()
                            }
                        },
                    )
                }
            }
        }
    }

    companion object {
        const val ACTION_EDIT_WIDGET = "ai.routin.mytoken.action.EDIT_WIDGET_CREDENTIAL"
    }
}

@Composable
private fun WidgetConfigurationScreen(
    kind: WidgetKind,
    initialConfig: WidgetConfig,
    credentials: List<Credential>,
    onSave: (WidgetConfig) -> Unit,
) {
    val graph = (androidx.compose.ui.platform.LocalContext.current.applicationContext as MyTokenApplication).graph
    val snapshots = credentials.associate {
        it.id to graph.refreshUseCase.states.value[it.id]?.snapshot
    }
    var style by remember { mutableStateOf(initialConfig.style) }
    var selectedCredentialId by remember { mutableStateOf(initialConfig.credentialId) }
    var overviewSource by remember { mutableStateOf(initialConfig.overviewSource) }
    var overviewIds by remember { mutableStateOf(initialConfig.limitedOverviewCredentialIds) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = if (kind == WidgetKind.Overview) "MyToken 总览" else "MyToken 凭证",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        ConfigurationPreview(
            preview = resolveConfigurationUiState(
                config = WidgetConfig(
                    kind = kind,
                    credentialId = selectedCredentialId,
                    overviewCredentialIds = overviewIds,
                    style = style,
                    overviewSource = overviewSource,
                ),
                credentials = credentials,
                snapshots = snapshots,
            ).preview,
        )
        Text("视觉风格", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetDisplayStyle.entries.forEach { option ->
                FilterChip(
                    selected = style == option,
                    onClick = { style = option },
                    label = { Text(option.displayName()) },
                )
            }
        }
        if (credentials.isEmpty()) {
            Text("打开应用添加凭证")
        } else if (kind == WidgetKind.Credential) {
            Text("凭证", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(credentials, key = { it.id }) { credential ->
                    CredentialOptionRow(
                        credential = credential,
                        selected = credential.id == selectedCredentialId,
                        onClick = { selectedCredentialId = credential.id },
                    )
                }
            }
        } else {
            Text("凭证来源", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WidgetOverviewSource.entries.forEach { source ->
                    FilterChip(
                        selected = overviewSource == source,
                        onClick = { overviewSource = source },
                        label = { Text(if (source == WidgetOverviewSource.AllEnabled) "全部启用" else "手动选择") },
                    )
                }
            }
            if (overviewSource == WidgetOverviewSource.Manual) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(credentials, key = { it.id }) { credential ->
                        val selected = credential.id in overviewIds
                        CredentialOptionRow(
                            credential = credential,
                            selected = selected,
                            onClick = {
                                overviewIds = if (selected) {
                                    overviewIds - credential.id
                                } else {
                                    (overviewIds + credential.id).take(WidgetConfig.MAX_OVERVIEW_CREDENTIALS)
                                }
                            },
                        )
                    }
                }
            }
        }
        Button(
            onClick = {
                onSave(
                    WidgetConfig(
                        kind = kind,
                        credentialId = selectedCredentialId,
                        overviewCredentialIds = overviewIds,
                        style = style,
                        overviewSource = overviewSource,
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("保存")
        }
    }
}

@Composable
private fun ConfigurationPreview(preview: WidgetPresentation?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val credential = preview?.credentials?.firstOrNull()
        val accent = WidgetProviderPalette.accentColor(credential?.providerId.orEmpty())
        Text(
            text = credential?.name ?: "暂无预览",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        when (preview?.style) {
            WidgetDisplayStyle.ScanBand -> PreviewScanBand(credential, accent)
            WidgetDisplayStyle.SignalTiles -> PreviewSignalTiles(credential, accent)
            WidgetDisplayStyle.FocusRing, WidgetDisplayStyle.Auto, null -> PreviewFocusRing(credential, accent)
        }
        Text(
            text = preview?.style?.displayName() ?: "自动",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PreviewFocusRing(credential: WidgetCredentialPresentation?, accent: androidx.compose.ui.graphics.Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        credential?.let {
            WidgetFocusRingPolicy.metrics(it).forEach { metric ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .size(68.dp)
                            .border(7.dp, accent, CircleShape),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(metric.valueText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(metric.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewScanBand(credential: WidgetCredentialPresentation?, accent: androidx.compose.ui.graphics.Color) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        credential?.orderedMetrics?.take(3)?.forEach { metric ->
            PreviewMetricRow(metric, accent)
        }
    }
}

@Composable
private fun PreviewSignalTiles(credential: WidgetCredentialPresentation?, accent: androidx.compose.ui.graphics.Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        credential?.orderedMetrics?.take(6)?.chunked(3)?.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { metric ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(6.dp),
                    ) {
                        Text(metric.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        Text(metric.valueText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        PreviewBar(metric.progressPercent, accent)
                    }
                }
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PreviewMetricRow(
    metric: ai.routin.mytoken.widget.WidgetMetricPresentation,
    accent: androidx.compose.ui.graphics.Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row {
            Text(metric.label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), maxLines = 1)
            Text(metric.valueText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        PreviewBar(metric.progressPercent, accent)
    }
}

@Composable
private fun PreviewBar(percent: Int?, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(2.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(((percent ?: 0).coerceIn(0, 100)) / 100f)
                .height(4.dp)
                .background(color, RoundedCornerShape(2.dp)),
        )
    }
}

@Composable
private fun CredentialOptionRow(
    credential: Credential,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = credential.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = ProviderCatalog.displayName(credential.providerId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Text("已选", color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun WidgetDisplayStyle.displayName(): String = when (this) {
    WidgetDisplayStyle.Auto -> "自动"
    WidgetDisplayStyle.FocusRing -> "聚焦环"
    WidgetDisplayStyle.ScanBand -> "扫描带"
    WidgetDisplayStyle.SignalTiles -> "信号格"
}

