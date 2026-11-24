package ai.routin.mytoken.feature.settings

import ai.routin.mytoken.core.alerts.PercentThresholdRange
import ai.routin.mytoken.core.ui.MyTokenButton
import ai.routin.mytoken.core.ui.MyTokenButtonTone
import ai.routin.mytoken.core.ui.MyTokenDialog
import ai.routin.mytoken.core.ui.MyTokenDialogSection
import ai.routin.mytoken.core.ui.MyTokenDialogTone
import ai.routin.mytoken.core.ui.MyTokenRadius
import ai.routin.mytoken.core.ui.MyTokenRangeSlider
import ai.routin.mytoken.core.ui.MyTokenRow
import ai.routin.mytoken.core.ui.MyTokenStatus
import ai.routin.mytoken.core.ui.MyTokenStatusTone
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Timer
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
) {
    val colors = MyTokenThemeTokens.colors
    var thresholdRange by remember(settings.lowThresholdPercent, settings.highThresholdPercent) {
        mutableStateOf(
            settings.lowThresholdPercent.toFloat()..settings.highThresholdPercent.toFloat(),
        )
    }
    var showThresholdDialog by remember { mutableStateOf(false) }
    var draftRange by remember { mutableStateOf(thresholdRange) }
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
            MyTokenRow(
                icon = Icons.Outlined.Timer,
                title = "用量提醒阈值",
                supportingText = PercentThresholdRange.label(lowPercent, highPercent),
                iconTint = colors.actionFill,
                iconSize = 22.dp,
                onClick = {
                    draftRange = thresholdRange
                    showThresholdDialog = true
                },
                modifier = Modifier
                    .testTag("settings_alert_threshold_edit")
                    .semantics { contentDescription = "编辑用量提醒阈值" },
                trailing = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.muted,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
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
        // 权限已授予时不再占用设置项；只有需要用户处理时才显示。
        if (!permissionGranted) {
            MyTokenRow(
                icon = null,
                title = "系统通知",
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
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
                                .clip(RoundedCornerShape(MyTokenRadius.pill))
                                .clickable(role = Role.Button, onClick = onRequestPermission)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                },
            )
        }
    }

    if (showThresholdDialog) {
        val draftLow = draftRange.start.roundToInt()
        val draftHigh = draftRange.endInclusive.roundToInt()
        MyTokenDialog(
            title = "用量提醒阈值",
            subtitle = "同时影响首页状态与通知",
            onDismissRequest = { showThresholdDialog = false },
            icon = Icons.Outlined.Notifications,
            tone = MyTokenDialogTone.Info,
            modifier = Modifier.testTag("settings_threshold_dialog"),
            content = {
                MyTokenDialogSection {
                    Text(
                        text = PercentThresholdRange.label(draftLow, draftHigh),
                        modifier = Modifier.testTag("settings_threshold_dialog_values"),
                        style = MyTokenType.label,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink,
                    )
                    MyTokenRangeSlider(
                        value = draftRange,
                        onValueChange = { proposed ->
                            val normalized = PercentThresholdRange.normalize(draftRange, proposed)
                            draftRange = normalized.first.toFloat()..normalized.last.toFloat()
                        },
                        valueRange = PercentThresholdRange.VALUE_RANGE,
                        contentDescription = "用量提醒阈值",
                        valueDescription = PercentThresholdRange.label(draftLow, draftHigh),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_alert_threshold_range"),
                    )
                }
            },
            actions = {
                MyTokenButton(
                    text = "取消",
                    onClick = { showThresholdDialog = false },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_threshold_cancel"),
                    tone = MyTokenButtonTone.Neutral,
                )
                MyTokenButton(
                    text = "保存",
                    onClick = {
                        thresholdRange = draftRange
                        onThresholdsChange(
                            draftRange.start.roundToInt(),
                            draftRange.endInclusive.roundToInt(),
                        )
                        showThresholdDialog = false
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_threshold_save"),
                    tone = MyTokenButtonTone.Primary,
                )
            },
        )
    }
}
