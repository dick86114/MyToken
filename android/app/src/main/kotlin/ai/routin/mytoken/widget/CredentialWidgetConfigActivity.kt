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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.routin.mytoken.domain.model.UsageSnapshot
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.launch

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
            val saveUseCase = remember(kind, appWidgetId) {
                WidgetConfigurationSaveUseCase(
                    save = store::save,
                    enqueueRefresh = {
                        WidgetRefresher.enqueue(this@CredentialWidgetConfigActivity)
                    },
                    redrawWidget = { widgetId ->
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
                            // 先落盘并入队刷新，然后立刻返回桌面；直接重绘放到应用级作用域，
                            // 这样即使 Glance 合成较慢也不会卡住保存按钮。
                            saveUseCase.apply(appWidgetId, config)
                            (application as MyTokenApplication).appScope.launch {
                                saveUseCase.redraw(appWidgetId)
                                // 首次添加时启动器可能还没登记组件 id，稍后补一次重绘，
                                // 避免离线情况下只有刷新任务而组件保持空白。
                                kotlinx.coroutines.delay(1_200)
                                saveUseCase.redraw(appWidgetId)
                            }
                            setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                            )
                            finish()
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
    var snapshots by remember { mutableStateOf<Map<java.util.UUID, UsageSnapshot?>>(emptyMap()) }
    LaunchedEffect(credentials) {
        snapshots = loadWidgetConfigurationSnapshots(
            credentialIds = credentials.map { it.id },
            stateSnapshot = { id -> graph.refreshUseCase.states.value[id]?.snapshot },
            cachedSnapshot = { id -> graph.credentialRepository.cachedSnapshot(id) },
        )
    }
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
                    overviewSource = overviewSource,
                ),
                credentials = credentials,
                snapshots = snapshots,
            ).preview,
        )
        if (credentials.isEmpty()) {
            Text("打开应用添加凭证")
            Spacer(modifier = Modifier.weight(1f))
        } else if (kind == WidgetKind.Credential) {
            Text("凭证", style = MaterialTheme.typography.titleMedium)
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(credentials, key = { it.id }) { credential ->
                        CredentialOptionRow(
                            credential = credential,
                            selected = credential.id == selectedCredentialId,
                            onClick = { selectedCredentialId = credential.id },
                        )
                    }
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
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
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
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Button(
            onClick = {
                onSave(
                    WidgetConfig(
                        kind = kind,
                        credentialId = selectedCredentialId,
                        overviewCredentialIds = overviewIds,
                        overviewSource = overviewSource,
                    )
                )
            },
            enabled = kind == WidgetKind.Overview || selectedCredentialId != null,
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
        val progressColors = WidgetProgressColors(androidx.compose.ui.platform.LocalContext.current)
        Text(
            text = credential?.name ?: "暂无预览",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        // 预览必须和真实组件保持一致：单凭证聚焦环、总览信号格、余额型直接显示余额。
        if (credential?.isBalanceOnly == true) {
            PreviewBalance(credential)
        } else if (preview?.style == WidgetDisplayStyle.SignalTiles) {
            PreviewSignalTiles(preview.credentials, progressColors)
        } else {
            PreviewFocusRing(credential, progressColors)
        }
        Text(
            text = preview?.style?.displayName() ?: "暂无预览",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PreviewBalance(credential: WidgetCredentialPresentation) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = credential.primary?.label.orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = credential.primary?.valueText ?: "-",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PreviewFocusRing(
    credential: WidgetCredentialPresentation?,
    progressColors: WidgetProgressColors,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        credential?.let {
            WidgetFocusRingPolicy.metrics(it).forEach { metric ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .size(68.dp)
                            .border(7.dp, progressColors.composeColor(metric.severity), CircleShape),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(metric.valueText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(metric.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                    Text(
                        text = metric.resetClockText.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewSignalTiles(
    credentials: List<WidgetCredentialPresentation>,
    progressColors: WidgetProgressColors,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        credentials.take(2).forEach { credential ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = credential.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    credential.orderedMetrics.take(3).forEach { metric ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(6.dp),
                        ) {
                            Row {
                                Text(
                                    text = metric.label,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                )
                                Text(
                                    text = metric.valueText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                )
                            }
                            metric.progressPercent?.let { percent ->
                                Spacer(modifier = Modifier.height(3.dp))
                                PreviewBar(percent, progressColors.composeColor(metric.severity))
                            }
                            metric.resetClockText?.let { clock ->
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "重置 $clock",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    repeat((3 - credential.orderedMetrics.take(3).size).coerceAtLeast(0)) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
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

