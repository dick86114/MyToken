package ai.routin.mytoken.feature.settings

import ai.routin.mytoken.core.ui.MyTokenRow
import ai.routin.mytoken.core.ui.MyTokenStatus
import ai.routin.mytoken.core.ui.MyTokenStatusTone
import ai.routin.mytoken.core.ui.MyTokenSwitch
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ai.routin.mytoken.core.ui.MyTokenSize

/** 通知设置暂时只保留凭证失效提醒及系统权限状态。 */
@Composable
fun NotificationSettingsSection(
    settings: NotificationSettings,
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onCredentialFailureAlertsChange: (Boolean) -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    Section(title = "通知") {
        SettingSwitchRow(
            label = "凭证失效提醒",
            checked = settings.credentialFailureAlertsEnabled,
            onChange = onCredentialFailureAlertsChange,
        )
        MyTokenRow(
            icon = null,
            title = "系统通知",
            trailing = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MyTokenStatus(
                        text = if (permissionGranted) "已允许" else "未授予",
                        tone = if (permissionGranted) {
                            MyTokenStatusTone.Neutral
                        } else {
                            MyTokenStatusTone.Warning
                        },
                        modifier = Modifier.testTag("settings_permission_status"),
                    )
                    if (!permissionGranted) {
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
