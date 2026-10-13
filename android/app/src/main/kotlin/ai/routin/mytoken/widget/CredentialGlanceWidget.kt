package ai.routin.mytoken.widget

import ai.routin.mytoken.MainActivity
import ai.routin.mytoken.MyTokenApplication
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.LinearProgressIndicator
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
            ?: return WidgetPresentation(config.style, size, null)
        val graph = (context.applicationContext as MyTokenApplication).graph
        val credential = graph.credentialRepository.observeCredentials()
            .first()
            .firstOrNull { it.id == credentialId }
            ?: return WidgetPresentation(config.style, size, null)
        val snapshot = graph.refreshUseCase.states.value[credentialId]?.snapshot
            ?: graph.credentialRepository.cachedSnapshot(credentialId)
        return WidgetPresentationBuilder.makeCredentialPresentation(
            credential = credential,
            snapshot = snapshot,
            style = config.style,
            size = size,
            fixedMetricId = config.primaryMetricId,
        )
    }

    @Composable
    override fun Content(
        context: Context,
        config: WidgetConfig,
        presentation: WidgetPresentation,
    ) {
        val credential = presentation.credential
        val openDetail = actionStartActivity(
            Intent(context, MainActivity::class.java)
                .setAction(MainActivity.ACTION_OPEN_CREDENTIAL)
                .putExtra(MainActivity.EXTRA_CREDENTIAL_ID, credential?.id?.toString())
        )
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFFF8FAFC)))
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
                    Text(
                        text = "刷新",
                        modifier = GlanceModifier
                            .clickable(actionRunCallback<WidgetRefreshAction>())
                            .padding(4.dp),
                        style = labelStyle(),
                        maxLines = 1,
                    )
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
                when (presentation.style) {
                    WidgetDisplayStyle.ScanBand -> ScanBandContent(credential)
                    WidgetDisplayStyle.SignalTiles -> SignalTilesContent(credential)
                    WidgetDisplayStyle.FocusRing, WidgetDisplayStyle.Auto -> FocusContent(credential)
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = credential.orderedMetrics.firstOrNull()?.resetText
                        ?: credential.planName.ifBlank { "MyToken" },
                    style = labelStyle(),
                    maxLines = WidgetTextLayoutPolicy.lineLimit(WidgetTextRole.StatusText),
                )
            }
        }
    }

    @Composable
    private fun FocusContent(credential: WidgetCredentialPresentation) {
        val primary = credential.primary
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            Box(
                modifier = GlanceModifier
                    .width(72.dp)
                    .height(72.dp)
                    .background(severityColor(primary?.severity))
                    .cornerRadius(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = primary?.valueText ?: "-", style = valueStyle(), maxLines = 1)
            }
            Spacer(modifier = GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                credential.secondary.take(2).forEach { metric ->
                    MetricBar(metric)
                    Spacer(modifier = GlanceModifier.height(6.dp))
                }
            }
        }
    }

    @Composable
    private fun ScanBandContent(credential: WidgetCredentialPresentation) {
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
                MetricBar(metric)
                Spacer(modifier = GlanceModifier.height(7.dp))
            }
        }
    }

    @Composable
    private fun SignalTilesContent(credential: WidgetCredentialPresentation) {
        Column {
            credential.orderedMetrics.take(6).chunked(3).forEach { row ->
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    row.forEach { metric ->
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .padding(3.dp),
                        ) {
                            Text(
                                text = metric.label,
                                style = labelStyle(),
                                maxLines = 1,
                            )
                            Text(text = metric.valueText, style = valueStyle(), maxLines = 1)
                            MetricBar(metric)
                        }
                    }
                }
                Spacer(modifier = GlanceModifier.height(5.dp))
            }
        }
    }

    @Composable
    private fun MetricBar(metric: WidgetMetricPresentation) {
        LinearProgressIndicator(
            progress = (metric.progressPercent ?: 0).coerceIn(0, 100) / 100f,
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(4.dp),
            color = severityColor(metric.severity),
        )
    }

    private fun severityColor(severity: WidgetMetricSeverity?): ColorProvider = when (severity) {
        WidgetMetricSeverity.Warning -> ColorProvider(Color(0xFFD97706))
        WidgetMetricSeverity.Critical -> ColorProvider(Color(0xFFDC2626))
        WidgetMetricSeverity.Unavailable -> ColorProvider(Color(0xFF94A3B8))
        WidgetMetricSeverity.Normal, null -> ColorProvider(Color(0xFF2563EB))
    }

    private fun labelStyle() = TextStyle(
        color = ColorProvider(Color(0xFF64748B)),
        fontSize = 10.sp,
    )

    private fun titleStyle() = TextStyle(
        color = ColorProvider(Color(0xFF0F172A)),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )

    private fun valueStyle() = TextStyle(
        color = ColorProvider(Color(0xFF0F172A)),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
    )
}
