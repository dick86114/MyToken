package ai.routin.mytoken.core.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** 锁定开关滑块的主题色：浅色白点，深色黑点。 */
class MyTokenSwitchStyleTest {
    private val source = File(System.getProperty("user.dir") ?: error("缺少 user.dir"))
        .resolve("src/main/kotlin/ai/routin/mytoken/core/ui/Components.kt")
        .readText()

    @Test
    fun `浅色主题开关滑块为白色深色保持黑色`() {
        val switch = source
            .substringAfter("fun MyTokenSwitch(")
            .substringBefore("/** 筛选 chip")

        assertTrue(
            "滑块颜色应按主题切换：浅色白、深色黑",
            switch.contains("if (darkTheme) colors.reportTicketInk else colors.surface"),
        )
        assertTrue(
            "需要读取当前主题",
            switch.contains("LocalMyTokenDarkTheme.current"),
        )
    }
}
