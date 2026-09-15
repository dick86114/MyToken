package ai.routin.mytoken.feature.settings

import ai.routin.mytoken.core.ui.GlassSwitch
import androidx.compose.runtime.Composable
import ai.routin.mytoken.core.ui.SettingRow

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
            checked = settings.openAppRefresh,
            onChange = onOpenAppRefreshChange,
        )
        SettingSwitchRow(
            label = "失败后自动重试",
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
) {
    SettingRow(title = label) {
        GlassSwitch(
            checked = checked,
            onCheckedChange = onChange,
            contentDescription = label,
        )
    }
}
