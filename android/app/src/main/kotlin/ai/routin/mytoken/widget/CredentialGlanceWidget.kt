package ai.routin.mytoken.widget

import ai.routin.mytoken.MainActivity
import ai.routin.mytoken.MyTokenApplication
import ai.routin.mytoken.R
import ai.routin.mytoken.domain.model.AppError
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
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

/**
 * 单凭证组件：固定聚焦环。5 小时 / 周 / 月有几个画几个（最多三个），
 * 每个环下面给出该周期的重置时间；只有余额的套餐直接展示余额。
 */
class CredentialGlanceWidget : MyTokenGlanceWidget() {
    override val widgetKind: WidgetKind = WidgetKind.Credential

    override suspend fun makePresentation(
        context: Context,
        config: WidgetConfig,
        size: WidgetSizeKind,
    ): WidgetPresentation {
        val empty = WidgetPresentation(WidgetDisplayStyle.FocusRing, size, emptyList())
        val credentialId = config.credentialId ?: return empty
        val graph = (context.applicationContext as MyTokenApplication).graph
        val credential = graph.credentialRepository.observeCredentials()
            .first()
            .firstOrNull { it.id == credentialId }
            ?: return empty
        val usageState = graph.refreshUseCase.states.value[credentialId]
        val snapshot = usageState?.snapshot
            ?: graph.credentialRepository.cachedSnapshot(credentialId)
        val base = WidgetPresentationBuilder.makeCredentialPresentation(
            credential = credential,
            snapshot = snapshot,
            size = size,
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
                .padding(horizontal = 14.dp, vertical = 12.dp),
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
            Spacer(modifier = GlanceModifier.height(6.dp))
            if (credential == null) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "打开应用添加凭证", style = titleStyle(), maxLines = 1)
                }
            } else {
                when {
                    credential.status == WidgetPresentationStatus.Loading && credential.primary == null ->
                        LoadingContent()
                    credential.status == WidgetPresentationStatus.NoMetrics ->
                        StatusMessage("该套餐暂无可用指标")
                    credential.isBalanceOnly -> BalanceContent(credential)
                    else -> FocusRings(credential, progressColors, presentation.size)
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

    /** 余额型凭证不画进度环，直接把余额作为主信息。 */
    @Composable
    private fun BalanceContent(credential: WidgetCredentialPresentation) {
        val metric = credential.primary ?: return
        Column(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        ) {
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(text = metric.label, style = labelStyle(), maxLines = 1)
            Text(
                text = metric.valueText,
                style = TextStyle(
                    color = ColorProvider(R.color.widget_glance_title),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    }

    @Composable
    private fun FocusRings(
        credential: WidgetCredentialPresentation,
        colors: WidgetProgressColors,
        size: WidgetSizeKind,
    ) {
        val ringSize = when (size) {
            WidgetSizeKind.Compact -> 42.dp
            WidgetSizeKind.Standard -> 54.dp
            WidgetSizeKind.Expanded -> 66.dp
        }
        val metrics = WidgetFocusRingPolicy.metrics(credential)
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            metrics.forEach { metric ->
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                ) {
                    Box(contentAlignment = Alignment.Center) {
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
                                .width(ringSize)
                                .height(ringSize),
                        )
                        Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                            Text(text = metric.valueText, style = ringValueStyle(size), maxLines = 1)
                            Text(text = metric.label, style = ringLabelStyle(size), maxLines = 1)
                        }
                    }
                    Spacer(modifier = GlanceModifier.height(3.dp))
                    Text(
                        text = metric.resetClockText.orEmpty(),
                        style = ringResetStyle(size),
                        maxLines = 1,
                    )
                }
            }
            repeat((WidgetFocusRingPolicy.MAX_RINGS - metrics.size).coerceAtLeast(0)) {
                Spacer(modifier = GlanceModifier.defaultWeight())
            }
        }
    }

    private fun labelStyle(fontSize: TextUnit = 10.sp) = TextStyle(
        color = ColorProvider(R.color.widget_glance_label),
        fontSize = fontSize,
    )

    private fun titleStyle() = TextStyle(
        color = ColorProvider(R.color.widget_glance_title),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )

    private fun ringValueStyle(size: WidgetSizeKind) = TextStyle(
        color = ColorProvider(R.color.widget_glance_title),
        fontSize = when (size) {
            WidgetSizeKind.Compact -> 11.sp
            WidgetSizeKind.Standard -> 13.sp
            WidgetSizeKind.Expanded -> 15.sp
        },
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )

    private fun ringLabelStyle(size: WidgetSizeKind) = TextStyle(
        color = ColorProvider(R.color.widget_glance_label),
        fontSize = when (size) {
            WidgetSizeKind.Compact -> 8.sp
            WidgetSizeKind.Standard -> 9.sp
            WidgetSizeKind.Expanded -> 10.sp
        },
        textAlign = TextAlign.Center,
    )

    private fun ringResetStyle(size: WidgetSizeKind) = TextStyle(
        color = ColorProvider(R.color.widget_glance_label),
        fontSize = when (size) {
            WidgetSizeKind.Compact -> 8.sp
            WidgetSizeKind.Standard -> 9.sp
            WidgetSizeKind.Expanded -> 10.sp
        },
        textAlign = TextAlign.Center,
    )
}
