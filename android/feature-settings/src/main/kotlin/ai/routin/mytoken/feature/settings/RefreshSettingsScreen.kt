package ai.routin.mytoken.feature.settings

import ai.routin.mytoken.core.ui.MyTokenSwitch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.runtime.Composable
import ai.routin.mytoken.core.ui.SettingRow
import androidx.compose.ui.unit.dp

/** 刷新设置只保留打开应用时刷新与失败后自动重试。 */
@Composable
fun RefreshSettingsSection(
    settings: RefreshSettings,
    onOpenAppRefreshChange: (Boolean) -> Unit,
    onRetryOnFailureChange: (Boolean) -> Unit,
) {
    Section(title = "刷新") {
        SettingSwitchRow(
            label = "打开应用时刷新",
            supportingText = "启动后立即拉取一次用量",
            icon = Icons.Outlined.Autorenew,
            iconTint = ai.routin.mytoken.core.ui.MyTokenThemeTokens.colors.actionFill,
            iconSize = 22.dp,
            checked = settings.openAppRefresh,
            onChange = onOpenAppRefreshChange,
        )
        SettingSwitchRow(
            label = "失败后自动重试",
            supportingText = "网络波动时静默重试一次",
            icon = Icons.Outlined.SyncProblem,
            iconTint = ai.routin.mytoken.core.ui.MyTokenThemeTokens.colors.actionFill,
            iconSize = 22.dp,
            checked = settings.retryOnFailure,
            onChange = onRetryOnFailureChange,
        )
    }
}

@Composable
internal fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    supportingText: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconContent: (@Composable () -> Unit)? = null,
    iconTint: androidx.compose.ui.graphics.Color = ai.routin.mytoken.core.ui.MyTokenThemeTokens.colors.muted,
    iconSize: androidx.compose.ui.unit.Dp? = null,
    enabled: Boolean = true,
) {
    SettingRow(
        title = label,
        supportingText = supportingText,
        icon = icon,
        iconContent = iconContent,
        iconTint = iconTint,
        iconSize = iconSize,
        trailing = {
            MyTokenSwitch(
                checked = checked,
                onCheckedChange = onChange,
                contentDescription = label,
                enabled = enabled,
            )
        },
    )
}
