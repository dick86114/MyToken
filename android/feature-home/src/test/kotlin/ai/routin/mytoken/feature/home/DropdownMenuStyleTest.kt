package ai.routin.mytoken.feature.home

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 最新设计把首页供应商筛选改成底部面板；详情页删除改为直接暴露的红色图标按钮，
 * 不再保留“更多”菜单。两边都不直接使用 Material3 DropdownMenu。
 */
class DropdownMenuStyleTest {
    @Test
    fun 首页使用供应商底部面板且详情页直接展示删除() {
        val home = sourceText("HomeChrome.kt")
        assertTrue("首页应使用 ModalBottomSheet", home.contains("ModalBottomSheet("))
        assertFalse("首页不再使用下拉菜单", home.contains("MyTokenDropdownMenu("))
        assertFalse(
            "首页不应直接使用 Material3 DropdownMenu",
            home.contains("androidx.compose.material3.DropdownMenu"),
        )

        val detail = sourceText("CredentialDetailScreen.kt")
        assertFalse("详情页不再使用“更多”菜单", detail.contains("MyTokenDropdownMenu("))
        assertFalse(
            "详情页不应直接使用 Material3 DropdownMenu",
            detail.contains("androidx.compose.material3.DropdownMenu"),
        )
        assertTrue("删除按钮应直接暴露", detail.contains("contentDescription = \"删除凭证\""))
        assertTrue("删除图标应使用危险红", detail.contains("tint = colors.criticalFill"))
    }

    private fun sourceText(name: String): String {
        val workingDir = File(System.getProperty("user.dir") ?: error("缺少 user.dir"))
        val candidates = listOf(
            workingDir.resolve("src/main/kotlin/ai/routin/mytoken/feature/home/$name"),
            workingDir.resolve("android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/$name"),
        )
        val file = candidates.firstOrNull(File::isFile) ?: error("找不到源文件：$name")
        return file.readText()
    }
}
