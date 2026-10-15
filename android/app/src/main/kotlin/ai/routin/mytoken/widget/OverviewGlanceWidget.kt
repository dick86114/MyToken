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
import androidx.glance.appwidget.CircularProgressIndicator
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
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.first

/**
 * 总览组件：固定信号格。每个凭证一行，行内最多三张 5 小时 / 周 / 月指标卡片，
 * 卡片包含文案、数值、加粗进度条和该周期的重置时间；只有余额的凭证直接显示余额。
 */
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
                .padding(horizontal = 14.dp, vertical = 12.dp),
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
            Spacer(modifier = GlanceModifier.height(5.dp))
            if (presentation.credentials.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "打开应用添加凭证", style = titleStyle(), maxLines = 1)
                }
            } else {
                presentation.credentials.forEach { credential ->
                    if (credential.isBalanceOnly) {
                        BalanceRow(context, credential)
                    } else {
                        SignalTilesRow(context, credential, progressColors, presentation.size)
                    }
                    Spacer(modifier = GlanceModifier.height(6.dp))
                }
            }
        }
    }

    @Composable
    private fun RefreshControl(context: Context) {
        if (WidgetRefreshStateStore(context).isRefreshing()) {
            CircularProgressIndicator(
                modifier = GlanceModifier
                    .width(24.dp)
                    .height(24.dp)
                    .padding(3.dp),
                color = ColorProvider(R.color.widget_glance_accent),
            )
        } else {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_refresh),
                contentDescription = context.getString(R.string.widget_refresh),
                modifier = GlanceModifier
                    .width(26.dp)
                    .height(26.dp)
                    .clickable(actionRunCallback<WidgetRefreshAction>())
                    .padding(3.dp),
            )
        }
    }

    /** 只有余额的凭证：一行显示名称与余额，不画进度。 */
    @Composable
    private fun BalanceRow(
        context: Context,
        credential: WidgetCredentialPresentation,
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity(credentialIntent(context, credential)))
                .padding(vertical = 3.dp),
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
            }
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = credential.primary?.valueText ?: "-",
                style = balanceStyle(),
                maxLines = WidgetTextLayoutPolicy.lineLimit(WidgetTextRole.MetricValue),
            )
        }
    }

    @Composable
    private fun SignalTilesRow(
        context: Context,
        credential: WidgetCredentialPresentation,
        colors: WidgetProgressColors,
        size: WidgetSizeKind,
    ) {
        val stacked = size == WidgetSizeKind.Compact
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity(credentialIntent(context, credential))),
        ) {
            if (stacked) {
                CredentialHeading(credential)
                Spacer(modifier = GlanceModifier.height(3.dp))
                MetricTiles(credential, colors, compact = true)
            } else {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Column(modifier = GlanceModifier.width(66.dp)) {
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
                    }
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Box(modifier = GlanceModifier.defaultWeight()) {
                        MetricTiles(credential, colors, compact = false)
                    }
                }
            }
        }
    }

    @Composable
    private fun CredentialHeading(credential: WidgetCredentialPresentation) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = credential.name,
                style = titleStyle(),
                maxLines = WidgetTextLayoutPolicy.lineLimit(WidgetTextRole.CredentialName),
            )
        }
    }

    @Composable
    private fun MetricTiles(
        credential: WidgetCredentialPresentation,
        colors: WidgetProgressColors,
        compact: Boolean,
    ) {
        val metrics = credential.orderedMetrics.take(WidgetFocusRingPolicy.MAX_RINGS)
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            if (metrics.isEmpty()) {
                Text(
                    text = "暂无数据",
                    modifier = GlanceModifier.defaultWeight(),
                    style = labelStyle(),
                    maxLines = 1,
                )
                return@Row
            }
            metrics.forEach { metric ->
                MetricTile(
                    metric = metric,
                    colors = colors,
                    compact = compact,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
            repeat((WidgetFocusRingPolicy.MAX_RINGS - metrics.size).coerceAtLeast(0)) {
                Spacer(modifier = GlanceModifier.defaultWeight())
            }
        }
    }

    @Composable
    private fun MetricTile(
        metric: WidgetMetricPresentation,
        colors: WidgetProgressColors,
        compact: Boolean,
        modifier: GlanceModifier,
    ) {
        Column(
            modifier = modifier
                .padding(horizontal = 2.dp)
                .background(ColorProvider(R.color.widget_glance_track))
                .cornerRadius(9.dp)
                .padding(horizontal = 6.dp, vertical = 5.dp),
        ) {
            if (compact) {
                Text(text = metric.label, style = labelStyle(), maxLines = 1)
                Text(text = metric.valueText, style = tileValueStyle(), maxLines = 1)
            } else {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Text(
                        text = metric.label,
                        modifier = GlanceModifier.defaultWeight(),
                        style = labelStyle(),
                        maxLines = WidgetTextLayoutPolicy.lineLimit(WidgetTextRole.MetricLabel),
                    )
                    Text(
                        text = metric.valueText,
                        style = tileValueStyle(),
                        maxLines = WidgetTextLayoutPolicy.lineLimit(WidgetTextRole.MetricValue),
                    )
                }
            }
            metric.progressPercent?.let { percent ->
                Spacer(modifier = GlanceModifier.height(3.dp))
                LinearProgressIndicator(
                    progress = percent.coerceIn(0, 100) / 100f,
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = colors.provider(metric.severity),
                )
            }
            metric.resetClockText?.let { clock ->
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(text = "重置 $clock", style = resetStyle(), maxLines = 1)
            }
        }
    }

    private fun credentialIntent(
        context: Context,
        credential: WidgetCredentialPresentation,
    ): Intent = Intent(context, MainActivity::class.java)
        .setAction(MainActivity.ACTION_OPEN_CREDENTIAL)
        .putExtra(MainActivity.EXTRA_CREDENTIAL_ID, credential.id.toString())

    private fun labelStyle() = TextStyle(
        color = ColorProvider(R.color.widget_glance_label),
        fontSize = 10.sp,
    )

    private fun resetStyle() = TextStyle(
        color = ColorProvider(R.color.widget_glance_label),
        fontSize = 9.sp,
    )

    private fun titleStyle() = TextStyle(
        color = ColorProvider(R.color.widget_glance_title),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
    )

    private fun tileValueStyle() = TextStyle(
        color = ColorProvider(R.color.widget_glance_title),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
    )

    private fun balanceStyle() = TextStyle(
        color = ColorProvider(R.color.widget_glance_title),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
    )
}
