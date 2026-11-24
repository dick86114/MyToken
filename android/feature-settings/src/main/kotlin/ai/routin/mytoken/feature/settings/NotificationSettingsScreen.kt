package ai.routin.mytoken.feature.settings

import ai.routin.mytoken.core.alerts.PercentThresholdRange
import ai.routin.mytoken.core.ui.MyTokenRangeSlider
import ai.routin.mytoken.core.ui.MyTokenRow
import ai.routin.mytoken.core.ui.MyTokenStatus
import ai.routin.mytoken.core.ui.MyTokenStatusTone
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 通知设置：总开关、全局用量提醒阈值、凭证失效提醒与系统权限状态。
 * 阈值仅用于通知判定，首页健康配色仍固定使用共享的 80/95。
 */
@Composable
fun NotificationSettingsSection(
    settings: NotificationSettings,
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onCredentialFailureAlertsChange: (Boolean) -> Unit,
    onNotificationsEnabledChange: (Boolean) -> Unit = {},
    onThresholdsChange: (Int, Int) -> Unit = { _, _ -> },
    onAlertThresholdDraggingChange: (Boolean) -> Unit = {},
    onThresholdBoundsChange: (Rect) -> Unit = {},
    onSliderBoundsChange: (Rect) -> Unit = {},
) {
    val colors = MyTokenThemeTokens.colors
    var thresholdRange by remember(settings.lowThresholdPercent, settings.highThresholdPercent) {
        mutableStateOf(
            settings.lowThresholdPercent.toFloat()..settings.highThresholdPercent.toFloat(),
        )
    }
    val lowPercent = thresholdRange.start.roundToInt()
    val highPercent = thresholdRange.endInclusive.roundToInt()

    Section(title = "通知") {
        SettingSwitchRow(
            label = "启用通知",
            supportingText = "关闭后暂停所有用量与凭证失效提醒",
            iconContent = {
                SettingsBadgedIcon(
                    base = Icons.Outlined.Notifications,
                    tint = colors.actionFill,
                    size = 22.dp,
                    modifier = Modifier.testTag("settings_notifications_enabled_icon"),
                )
            },
            checked = settings.notificationsEnabled,
            onChange = onNotificationsEnabledChange,
        )

        if (settings.notificationsEnabled) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { onThresholdBoundsChange(it.windowRect()) }
                    .testTag("settings_alert_thresholds"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "用量提醒阈值",
                            style = MyTokenType.body,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.ink,
                        )
                        Text(
                            text = "同时影响首页状态与通知",
                            style = MyTokenType.caption,
                            color = colors.muted,
                        )
                    }
                    Text(
                        text = PercentThresholdRange.label(lowPercent, highPercent),
                        modifier = Modifier.testTag("settings_alert_threshold_values"),
                        style = MyTokenType.label,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink,
                    )
                }
                MyTokenRangeSlider(
                    value = thresholdRange,
                    onValueChange = { proposed ->
                        val normalized = PercentThresholdRange.normalize(thresholdRange, proposed)
                        thresholdRange = normalized.first.toFloat()..normalized.last.toFloat()
                    },
                    valueRange = PercentThresholdRange.VALUE_RANGE,
                    contentDescription = "用量提醒阈值",
                    valueDescription = PercentThresholdRange.label(lowPercent, highPercent),
                    onValueChangeFinished = {
                        onThresholdsChange(
                            thresholdRange.start.roundToInt(),
                            thresholdRange.endInclusive.roundToInt(),
                        )
                    },
                    onDraggingChanged = onAlertThresholdDraggingChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { onSliderBoundsChange(it.windowRect()) }
                        .testTag("settings_alert_threshold_range"),
                )
            }
        }

        SettingSwitchRow(
            label = "凭证失效提醒",
            supportingText = "Key 失效或权限不足时通知",
            iconContent = {
                SettingsBadgedIcon(
                    base = Icons.Outlined.CreditCard,
                    tint = MyTokenThemeTokens.colors.actionFill,
                    size = 22.dp,
                    modifier = Modifier.testTag("settings_credential_failure_icon"),
                )
            },
            checked = settings.credentialFailureAlertsEnabled,
            onChange = onCredentialFailureAlertsChange,
            enabled = settings.notificationsEnabled,
        )
        MyTokenRow(
            icon = null,
            title = "系统通知",
            trailing = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (permissionGranted) {
                        Row(
                            modifier = Modifier.testTag("settings_permission_status"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = colors.muted,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = "已允许",
                                style = MyTokenType.caption,
                                color = colors.muted,
                            )
                        }
                    } else {
                        MyTokenStatus(
                            text = "未授予",
                            tone = MyTokenStatusTone.Warning,
                            modifier = Modifier.testTag("settings_permission_status"),
                        )
                        Text(
                            text = "去授权",
                            style = MyTokenType.label,
                            color = colors.actionText,
                            modifier = Modifier
                                .clip(RoundedCornerShape(ai.routin.mytoken.core.ui.MyTokenRadius.pill))
                                .clickable(role = Role.Button, onClick = onRequestPermission)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            },
        )
    }
}

private fun LayoutCoordinates.windowRect(): Rect {
    val origin = localToWindow(Offset.Zero)
    return Rect(
        offset = origin,
        size = Size(size.width.toFloat(), size.height.toFloat()),
    )
}
