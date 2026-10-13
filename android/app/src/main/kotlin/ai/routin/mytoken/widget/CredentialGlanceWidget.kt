package ai.routin.mytoken.widget

import ai.routin.mytoken.MainActivity
import ai.routin.mytoken.MyTokenApplication
import ai.routin.mytoken.R
import ai.routin.mytoken.domain.model.AppError
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
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.first

class CredentialGlanceWidget : MyTokenGlanceWidget() {
    override val widgetKind: WidgetKind = WidgetKind.Credential

    override suspend fun makePresentation(
        context: Context,
        config: WidgetConfig,
        size: WidgetSizeKind,
    ): WidgetPresentation {
        val credentialId = config.credentialId
            ?: return WidgetPresentation(config.style, size, emptyList())
        val graph = (context.applicationContext as MyTokenApplication).graph
        val credential = graph.credentialRepository.observeCredentials()
            .first()
            .firstOrNull { it.id == credentialId }
            ?: return WidgetPresentation(config.style, size, emptyList())
        val usageState = graph.refreshUseCase.states.value[credentialId]
        val snapshot = usageState?.snapshot
            ?: graph.credentialRepository.cachedSnapshot(credentialId)
        val base = WidgetPresentationBuilder.makeCredentialPresentation(
            credential = credential,
            snapshot = snapshot,
            style = config.style,
            size = size,
            fixedMetricId = config.primaryMetricId,
        )
        val failure = when {
            usageState?.error is AppError.Authentication ->
                WidgetFailureKind.Authentication
            usageState?.error != null -> WidgetFailureKind.Other
            else -> null
        }
        val status = WidgetPresentationStateResolver.resolve(
            isEnabled = credential.isEnabled,
            hasSnapshot = snapshot != null,
            isStale = usageState?.isStale == true,
            isRefreshing = usageState?.status == ai.routin.mytoken.domain.usage.RefreshStatus.Loading,
            failure = failure,
            metricCount = snapshot?.metrics?.size ?: 0,
            fixedMetricUnavailable = base.credential?.fixedMetricUnavailable == true,
        )
        return base.copy(
            credentials = base.credentials.map { it.copy(status = status) },
        )
    }

    @Composable
    override fun Content(
        context: Context,
        config: WidgetConfig,
        presentation: WidgetPresentation,
    ) {
        val credential = presentation.credential
        val progressColors = WidgetProgressColors(context)
        val openDetail = actionStartActivity(
            Intent(context, MainActivity::class.java)
                .setAction(MainActivity.ACTION_OPEN_CREDENTIAL)
                .putExtra(MainActivity.EXTRA_CREDENTIAL_ID, credential?.id?.toString())
        )
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(R.color.widget_glance_surface))
                .cornerRadius(22.dp)
                .clickable(openDetail)
                .padding(14.dp),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.Top,
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = credential?.providerName?.uppercase() ?: "MYTOKEN",
                        style = labelStyle(),
                        maxLines = WidgetTextLayoutPolicy.lineLimit(WidgetTextRole.ProviderName),
                    )
                    Text(
                        text = credential?.name ?: "点击选择凭证",
                        style = titleStyle(),
                        maxLines = WidgetTextLayoutPolicy.lineLimit(WidgetTextRole.CredentialName),
                    )
                }
                if (credential != null) {
                    RefreshControl(context)
                }
            }
            Spacer(modifier = GlanceModifier.height(8.dp))
            if (credential == null) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "打开应用添加凭证", style = titleStyle(), maxLines = 1)
                }
            } else {
                if (credential.status == WidgetPresentationStatus.Loading && credential.primary == null) {
                    LoadingContent()
                } else if (credential.status == WidgetPresentationStatus.NoMetrics) {
                    StatusMessage("该套餐暂无可用指标")
                } else {
                    when (presentation.style) {
                        WidgetDisplayStyle.ScanBand -> ScanBandContent(credential, progressColors)
                        WidgetDisplayStyle.SignalTiles -> SignalTilesContent(credential, progressColors)
                        WidgetDisplayStyle.FocusRing, WidgetDisplayStyle.Auto ->
                            FocusRings(credential, progressColors)
                    }
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = WidgetPresentationStateResolver.copy(credential.status),
                    style = labelStyle(),
                    maxLines = WidgetTextLayoutPolicy.lineLimit(WidgetTextRole.StatusText),
                )
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
    private fun LoadingContent() {
        Column {
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(ColorProvider(R.color.widget_glance_track))
                    .cornerRadius(6.dp),
            ) {}
            Spacer(modifier = GlanceModifier.height(8.dp))
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(ColorProvider(R.color.widget_glance_track))
                    .cornerRadius(10.dp),
            ) {}
        }
    }

    @Composable
    private fun StatusMessage(message: String) {
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = message, style = titleStyle(), maxLines = 1)
        }
    }

    @Composable
    private fun FocusRings(
        credential: WidgetCredentialPresentation,
        colors: WidgetProgressColors,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            WidgetFocusRingPolicy.metrics(credential).forEach { metric ->
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(3.dp),
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
                            .width(68.dp)
                            .height(68.dp),
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = metric.valueText, style = valueStyle(), maxLines = 1)
                        Text(
                            text = metric.label,
                            style = labelStyle(),
                            maxLines = WidgetTextLayoutPolicy.lineLimit(WidgetTextRole.MetricLabel),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ScanBandContent(
        credential: WidgetCredentialPresentation,
        colors: WidgetProgressColors,
    ) {
        Column {
            credential.orderedMetrics.take(3).forEach { metric ->
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Text(
                        text = metric.label,
                        modifier = GlanceModifier.defaultWeight(),
                        style = labelStyle(),
                        maxLines = 1,
                    )
                    Text(text = metric.valueText, style = valueStyle(), maxLines = 1)
                }
                Spacer(modifier = GlanceModifier.height(3.dp))
                MetricBar(metric, colors, thicknessDp = 6)
                Spacer(modifier = GlanceModifier.height(7.dp))
            }
        }
    }

    @Composable
    private fun SignalTilesContent(
        credential: WidgetCredentialPresentation,
        colors: WidgetProgressColors,
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            credential.orderedMetrics.take(3).forEach { metric ->
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(3.dp)
                        .background(ColorProvider(R.color.widget_glance_track))
                        .cornerRadius(10.dp)
                        .padding(7.dp),
                ) {
                    Text(text = metric.label, style = labelStyle(), maxLines = 1)
                    Text(text = metric.valueText, style = valueStyle(), maxLines = 1)
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    MetricBar(metric, colors, thicknessDp = 6)
                }
            }
        }
    }

    @Composable
    private fun MetricBar(
        metric: WidgetMetricPresentation,
        colors: WidgetProgressColors,
        thicknessDp: Int,
    ) {
        LinearProgressIndicator(
            progress = (metric.progressPercent ?: 0).coerceIn(0, 100) / 100f,
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(thicknessDp.dp),
            color = colors.provider(metric.severity),
        )
    }

    private fun labelStyle() = TextStyle(
        color = ColorProvider(R.color.widget_glance_label),
        fontSize = 10.sp,
    )

    private fun titleStyle() = TextStyle(
        color = ColorProvider(R.color.widget_glance_title),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )

    private fun valueStyle() = TextStyle(
        color = ColorProvider(R.color.widget_glance_title),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
    )
}
