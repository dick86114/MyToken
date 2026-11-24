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
        assertTrue("有更新时使用圆圈上箭头", settings.contains("Icons.Outlined.ArrowCircleUp"))
        assertFalse(settings.contains("Icons.Filled.ArrowUpward"))
        assertTrue(settings.contains("MyTokenUpdateDialog"))
        assertTrue(settings.contains("UpdateSettingsDialog"))
        assertTrue(settings.contains("update_source_github"))
        assertTrue(settings.contains("update_source_cdn"))
        assertTrue(settings.contains("update_settings_save"))
        assertFalse(settings.contains("settings_check_updates"))
        assertFalse(settings.contains("UpdateSection("))
    }

    @Test
    fun `设置资料卡使用上黄下黑的票据铭牌`() {
        val settings = sourceText("SettingsScreen.kt")
        val profile = settings
            .substringAfter("private fun SettingsProfileCard")
            .substringBefore("@Composable\nprivate fun ThemeSelector")

        assertTrue(
            "铭牌外形应与凭证报告一致（圆角票据）",
            profile.contains(".background(colors.reportSurface, MyTokenReportBodyShape)"),
        )
        assertTrue(
            "黄色票面承载 Logo 与名称",
            profile.contains(".background(colors.reportTicket, MyTokenReportTicketShape)"),
        )
        assertTrue("铭牌 Logo 固定使用黑色版本", profile.contains("darkTheme = false"))
        assertTrue("铭牌名称必须固定为黑色", profile.contains("color = colors.reportTicketInk"))
        assertTrue("黑色部分显示版本号", profile.contains("text = \"v\$appVersion\""))
        assertTrue("版本号使用报告反色墨水", profile.contains("color = colors.reportInk"))
        assertFalse("铭牌不再使用整块黄底", profile.contains("colors.actionFill"))
    }

    @Test
    fun `支持项目星标带文案和黄色描边`() {
        val settings = sourceText("SettingsScreen.kt")
        val support = settings
            .substringAfter("title = \"支持项目\"")
            .substringBefore("Icons.Outlined.Handyman")

        assertTrue("星标应带「加星标」文案", support.contains("text = \"加星标\""))
        assertTrue("星标应套黄色外框", support.contains(".border(1.dp, colors.actionFill"))
        assertTrue(support.contains("settings_support_star"))
    }

    @Test
    fun `设置页行距收紧`() {
        val settings = sourceText("SettingsScreen.kt")

        assertTrue(
            "分节间距应使用紧凑值",
            settings.contains("Arrangement.spacedBy(MyTokenSpacing.settingsSectionCompact)"),
        )
        assertTrue(
            "卡片内边距应使用紧凑值",
            settings.contains("contentPadding = MyTokenSpacing.cardCompact"),
        )
        assertTrue(
            "设置行间距应使用紧凑值",
            settings.contains("contentSpacing = MyTokenSpacing.sm"),
        )
    }

    @Test
    fun `外观选项点击不显示灰色遮罩`() {
        val settings = sourceText("SettingsScreen.kt")
        val selector = settings
            .substringAfter("private fun ThemeSelector")
            .substringBefore("/** 三张主题预览卡")

        assertTrue("外观选项不显示按压遮罩", selector.contains("indication = null"))
        assertTrue(
            "外观选项应显式提供交互源以关闭默认 indication",
            selector.contains("interactionSource = remember { MutableInteractionSource() }"),
        )
    }

    @Test
    fun `外观选中边框使用黄色`() {
        val settings = sourceText("SettingsScreen.kt")
        val scene = settings
            .substringAfter("private fun ThemePreviewScene")
            .substringBefore("private val DEFAULT_UPDATE_CDN_BASES")

        assertTrue("选中的主题预览应使用黄色描边", scene.contains("selected -> colors.actionFill"))
        assertFalse("选中的主题预览不再使用墨色描边", scene.contains("selected -> colors.ink"))
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
