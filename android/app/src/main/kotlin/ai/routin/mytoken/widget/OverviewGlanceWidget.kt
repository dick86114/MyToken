package ai.routin.mytoken.widget

import ai.routin.mytoken.MainActivity
import ai.routin.mytoken.MyTokenApplication
import ai.routin.mytoken.R
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.util.UUID
import kotlinx.coroutines.flow.first

class OverviewGlanceWidget : MyTokenGlanceWidget() {
    override val widgetKind: WidgetKind = WidgetKind.Overview

    override suspend fun makePresentation(
        context: Context,
        config: WidgetConfig,
        size: WidgetSizeKind,
    ): WidgetPresentation {
        val graph = (context.applicationContext as MyTokenApplication).graph
        val credentials = graph.credentialRepository.observeCredentials().first()
        val byId = credentials.associateBy { it.id }
        val selected = when (config.overviewSource) {
            WidgetOverviewSource.AllEnabled -> credentials.filter { it.isEnabled }
            WidgetOverviewSource.Manual -> config.limitedOverviewCredentialIds.mapNotNull(byId::get)
        }
        val entries = selected.map { credential ->
            val snapshot = graph.refreshUseCase.states.value[credential.id]?.snapshot
                ?: graph.credentialRepository.cachedSnapshot(credential.id)
            credential to snapshot
        }
        return WidgetPresentationBuilder.makeOverviewPresentation(
            entries = entries,
            style = config.style,
            size = size,
        )
    }

    @Composable
    override fun Content(
        context: Context,
        config: WidgetConfig,
        presentation: WidgetPresentation,
    ) {
        val progressColors = WidgetProgressColors(context)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(R.color.widget_glance_surface))
                .cornerRadius(22.dp)
                .padding(14.dp),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = "MyToken 总览",
                    modifier = GlanceModifier.defaultWeight(),
                    style = labelStyle(),
                    maxLines = 1,
                )
                RefreshControl(context)
            }
            Spacer(modifier = GlanceModifier.height(7.dp))
            if (presentation.credentials.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "打开应用添加凭证", style = titleStyle(), maxLines = 1)
                }
            } else {
                when (OverviewStyleLayoutPolicy.layout(presentation.style)) {
                    OverviewStyleLayout.ScanRows -> {
                        presentation.credentials.forEach { credential ->
                            OverviewRow(context, credential, progressColors)
                            Spacer(modifier = GlanceModifier.height(7.dp))
                        }
                    }
                    OverviewStyleLayout.FocusRings ->
                        OverviewFocusRings(context, presentation.credentials, progressColors)
                    OverviewStyleLayout.SignalTiles ->
                        OverviewSignalTiles(context, presentation.credentials, progressColors)
                }
            }
        }
    }

    @Composable
    private fun RefreshControl(context: Context) {
        if (WidgetRefreshStateStore(context).isRefreshing()) {
            CircularProgressIndicator(
                modifier = GlanceModifier
                    .width(20.dp)
                    .height(20.dp)
                    .padding(2.dp),
                color = ColorProvider(R.color.widget_glance_accent),
            )
        } else {
            Image(
                provider = ImageProvider(android.R.drawable.ic_popup_sync),
                contentDescription = "刷新",
                modifier = GlanceModifier
                    .width(20.dp)
                    .height(20.dp)
                    .clickable(actionRunCallback<WidgetRefreshAction>())
                    .padding(2.dp),
            )
        }
    }

    @Composable
    private fun OverviewFocusRings(
        context: Context,
        credentials: List<WidgetCredentialPresentation>,
        colors: WidgetProgressColors,
    ) {
        Column {
            credentials.forEach { credential ->
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .clickable(actionStartActivity(credentialIntent(context, credential))),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Column(modifier = GlanceModifier.width(86.dp)) {
                        Text(
                            text = credential.name,
                            style = titleStyle(),
                            maxLines = WidgetTextLayoutPolicy.lineLimit(WidgetTextRole.CredentialName),
                        )
                        Text(
                            text = credential.providerName,
                            style = labelStyle(),
                            maxLines = 1,
                        )
                    }
                    Row(
                        modifier = GlanceModifier.defaultWeight(),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                    ) {
                        WidgetFocusRingPolicy.metrics(credential).forEach { metric ->
                            Box(
                                modifier = GlanceModifier.padding(2.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    provider = ImageProvider(
                                        WidgetRingRenderer.bitmap(
                                            percent = metric.progressPercent,
                                            color = colors.colorInt(metric.severity),
                                            trackColor = colors.trackColorInt,
                                        )
                                    ),
                                    contentDescription = null,
                                    modifier = GlanceModifier
                                        .width(48.dp)
                                        .height(48.dp),
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(metric.valueText, style = valueStyle(), maxLines = 1)
                                    Text(metric.label, style = labelStyle(), maxLines = 1)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = GlanceModifier.height(7.dp))
            }
        }
    }

    @Composable
    private fun OverviewSignalTiles(
        context: Context,
        credentials: List<WidgetCredentialPresentation>,
        colors: WidgetProgressColors,
    ) {
        Column {
            credentials.forEach { credential ->
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .clickable(actionStartActivity(credentialIntent(context, credential))),
                ) {
                    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                        Text(
                            text = credential.name,
                            modifier = GlanceModifier.defaultWeight(),
                            style = titleStyle(),
                            maxLines = WidgetTextLayoutPolicy.lineLimit(WidgetTextRole.CredentialName),
                        )
                        Text(
                            text = credential.primary?.valueText ?: "-",
                            style = valueStyle(),
                            maxLines = 1,
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = (credential.primary?.progressPercent ?: 0).coerceIn(0, 100) / 100f,
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(12.dp),
                        color = colors.provider(credential.primary?.severity),
                    )
                }
                Spacer(modifier = GlanceModifier.height(8.dp))
            }
        }
    }

    private fun credentialIntent(
        context: Context,
        credential: WidgetCredentialPresentation,
    ): Intent = Intent(context, MainActivity::class.java)
        .setAction(MainActivity.ACTION_OPEN_CREDENTIAL)
        .putExtra(MainActivity.EXTRA_CREDENTIAL_ID, credential.id.toString())

    @Composable
    private fun OverviewRow(
        context: Context,
        credential: WidgetCredentialPresentation,
        colors: WidgetProgressColors,
    ) {
        val intent = credentialIntent(context, credential)
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity(intent))
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = credential.name,
                    style = titleStyle(),
                    maxLines = WidgetTextLayoutPolicy.lineLimit(WidgetTextRole.CredentialName),
                )
                Text(
                    text = credential.providerName,
                    style = labelStyle(),
                    maxLines = WidgetTextLayoutPolicy.lineLimit(WidgetTextRole.ProviderName),
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                LinearProgressIndicator(
                    progress = (credential.primary?.progressPercent ?: 0).coerceIn(0, 100) / 100f,
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = colors.provider(credential.primary?.severity),
                )
            }
            Spacer(modifier = GlanceModifier.width(8.dp))
            Column(modifier = GlanceModifier.width(74.dp)) {
                Text(
                    text = credential.primary?.valueText ?: "-",
                    style = valueStyle(),
                    maxLines = WidgetTextLayoutPolicy.lineLimit(WidgetTextRole.MetricValue),
                )
                Text(
                    text = credential.primary?.label ?: "",
                    style = labelStyle(),
                    maxLines = WidgetTextLayoutPolicy.lineLimit(WidgetTextRole.MetricLabel),
                )
            }
        }
    }

    private fun labelStyle() = TextStyle(
        color = ColorProvider(R.color.widget_glance_label),
        fontSize = 10.sp,
    )

    private fun titleStyle() = TextStyle(
        color = ColorProvider(R.color.widget_glance_title),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
    )

    private fun valueStyle() = TextStyle(
        color = ColorProvider(R.color.widget_glance_title),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
    )
}
