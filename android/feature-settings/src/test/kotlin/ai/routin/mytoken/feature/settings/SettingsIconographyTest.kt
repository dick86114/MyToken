package ai.routin.mytoken.feature.settings

import java.io.File
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

    private fun sourceText(name: String): String {
        val candidates = listOf(
            root.resolve("src/main/kotlin/ai/routin/mytoken/feature/settings/$name"),
            root.resolve("android/feature-settings/src/main/kotlin/ai/routin/mytoken/feature/settings/$name"),
        )
        val file = candidates.firstOrNull(File::isFile) ?: error("找不到源文件：$name")
        return file.readText()
    }
}
