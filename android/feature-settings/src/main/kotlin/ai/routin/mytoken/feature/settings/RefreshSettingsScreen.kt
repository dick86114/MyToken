package ai.routin.mytoken.feature.settings

import ai.routin.mytoken.core.ui.MyTokenSwitch
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
            supportingText = "启动后立即拉取一次用量",
            checked = settings.openAppRefresh,
            onChange = onOpenAppRefreshChange,
        )
        SettingSwitchRow(
            label = "失败后自动重试",
            supportingText = "网络波动时静默重试一次",
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
) {
    SettingRow(title = label, supportingText = supportingText) {
        MyTokenSwitch(
            checked = checked,
            onCheckedChange = onChange,
            contentDescription = label,
        )
    }
}
