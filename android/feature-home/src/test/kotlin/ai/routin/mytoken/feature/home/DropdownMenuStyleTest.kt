package ai.routin.mytoken.feature.home

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 最新设计把首页供应商筛选改成底部面板；详情页“更多”仍使用 core-ui 的菜单样式。
 * 两边都不直接使用 Material3 DropdownMenu。
 */
class DropdownMenuStyleTest {
    @Test
    fun 首页使用供应商底部面板且详情页保留统一菜单() {
        val home = sourceText("HomeChrome.kt")
        assertTrue("首页应使用 ModalBottomSheet", home.contains("ModalBottomSheet("))
        assertFalse("首页不再使用下拉菜单", home.contains("MyTokenDropdownMenu("))
        assertFalse(
            "首页不应直接使用 Material3 DropdownMenu",
            home.contains("androidx.compose.material3.DropdownMenu"),
        )

        val detail = sourceText("CredentialDetailScreen.kt")
        assertTrue("详情页应继续使用 MyTokenDropdownMenu", detail.contains("MyTokenDropdownMenu("))
        assertFalse(
            "详情页不应直接使用 Material3 DropdownMenu",
            detail.contains("androidx.compose.material3.DropdownMenu"),
        )
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
