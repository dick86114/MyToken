package ai.routin.mytoken.feature.settings

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 锁定设置页行图标与 SF 符号的映射，避免后续重构回退。 */
class SettingsIconographyTest {
    private val root = File(System.getProperty("user.dir") ?: error("缺少 user.dir"))

    @Test
    fun `刷新设置使用指定的旋转箭头图标`() {
        val source = sourceText("RefreshSettingsScreen.kt")

        assertTrue(source.contains("Icons.Outlined.Autorenew"))
        assertTrue(source.contains("Icons.Outlined.SyncProblem"))
    }

    @Test
    fun `凭证失效提醒与导出日志使用警告 badge 图标`() {
        val notification = sourceText("NotificationSettingsScreen.kt")
        val settings = sourceText("SettingsScreen.kt")

        assertTrue(notification.contains("SettingsBadgedIcon"))
        assertTrue(notification.contains("Icons.Outlined.CreditCard"))
        assertTrue(settings.contains("SettingsBadgedIcon"))
        assertTrue(settings.contains("Icons.Outlined.IosShare"))
        assertTrue(settings.contains("Icons.Outlined.QrCodeScanner"))
    }

    @Test
    fun `设置页图标统一黄色且放大，数据行不再套底板`() {
        val settings = sourceText("SettingsScreen.kt")
        val refresh = sourceText("RefreshSettingsScreen.kt")
        val notification = sourceText("NotificationSettingsScreen.kt")

        assertTrue(settings.contains("iconTint = MyTokenThemeTokens.colors.actionFill"))
        assertTrue(settings.contains("iconSize = 22.dp"))
        assertTrue(refresh.contains("MyTokenThemeTokens.colors.actionFill"))
        assertTrue(refresh.contains("iconSize = 22.dp"))
        assertTrue(notification.contains("MyTokenThemeTokens.colors.actionFill"))
        assertTrue(notification.contains("size = 22.dp"))
        assertFalse("数据行不再使用 28dp 图标底板", settings.contains("iconPlate = true"))
    }

    @Test
    fun `关于板块使用新的菜单行与更新弹窗`() {
        val settings = sourceText("SettingsScreen.kt")

        assertTrue(settings.contains("Icons.Outlined.Info"))
        assertTrue(settings.contains("MyTokenIcons.GitHub"))
        assertTrue(settings.contains("Icons.Outlined.Handyman"))
        assertTrue(settings.contains("Icons.Filled.History"))
        assertTrue(settings.contains("settings_version_value"))
        assertTrue(settings.contains("settings_update_available_arrow"))
        assertTrue(settings.contains("MyTokenUpdateDialog"))
        assertTrue(settings.contains("UpdateSettingsDialog"))
        assertTrue(settings.contains("update_source_github"))
        assertTrue(settings.contains("update_source_cdn"))
        assertTrue(settings.contains("update_settings_save"))
        assertFalse(settings.contains("settings_check_updates"))
        assertFalse(settings.contains("UpdateSection("))
    }

    private fun sourceText(name: String): String {
        val candidates = listOf(
            root.resolve("src/main/kotlin/ai/routin/mytoken/feature/settings/$name"),
            root.resolve("android/feature-settings/src/main/kotlin/ai/routin/mytoken/feature/settings/$name"),
        )
        val file = candidates.firstOrNull(File::isFile) ?: error("找不到源文件：$name")
        return file.readText()
    }
}
