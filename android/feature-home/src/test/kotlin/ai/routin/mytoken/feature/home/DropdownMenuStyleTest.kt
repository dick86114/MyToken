package ai.routin.mytoken.feature.home

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 首页供应商筛选与详情页“更多”菜单必须共用 core-ui 的菜单样式。 */
class DropdownMenuStyleTest {
    @Test
    fun 首页与详情页菜单使用统一样式() {
        listOf("HomeChrome.kt", "CredentialDetailScreen.kt").forEach { name ->
            val source = sourceText(name)
            assertTrue("$name 应使用 MyTokenDropdownMenu", source.contains("MyTokenDropdownMenu("))
            assertFalse(
                "$name 不应直接使用 Material3 DropdownMenu",
                source.contains("androidx.compose.material3.DropdownMenu"),
            )
        }
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
