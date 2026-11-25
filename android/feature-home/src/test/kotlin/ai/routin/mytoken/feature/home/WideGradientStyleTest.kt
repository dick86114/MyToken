package ai.routin.mytoken.feature.home

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 锁定大屏整窗渐变按当前可用高度铺满，而不是固定 200dp 的半屏渐变。 */
class WideGradientStyleTest {
    @Test
    fun `大屏渐变铺满导航栏上方的整屏高度`() {
        val source = sourceText("WideHomeLayout.kt")

        // 不允许再出现固定渐变距离。
        assertFalse(source.contains("headerFadeDistance"))
        // 渐变终点由窗口高度减去系统导航栏高度得到。
        assertTrue(source.contains("size.height - systemBarBottomDp.toPx()"))
        assertTrue(source.contains("gradientBottomPx"))
        assertTrue(source.contains("canvasStop = (gradientBottomPx / height)"))
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
