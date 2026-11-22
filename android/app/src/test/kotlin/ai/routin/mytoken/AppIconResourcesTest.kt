package ai.routin.mytoken

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** 锁定新版启动图标资源，避免自适应图标退回旧色块版本。 */
class AppIconResourcesTest {
    private val resRoot = File(System.getProperty("user.dir") ?: error("缺少 user.dir"))
        .resolve("src/main/res")

    private fun res(path: String): File {
        val file = resRoot.resolve(path)
        assertTrue("找不到资源：$path", file.isFile)
        return file
    }

    @Test
    fun `自适应图标包含背景前景与单色层`() {
        listOf("mipmap-anydpi-v26/ic_launcher.xml", "mipmap-anydpi-v26/ic_launcher_round.xml")
            .forEach { path ->
                val xml = res(path).readText()
                assertTrue("$path 应引用背景图标", xml.contains("@mipmap/ic_launcher_background"))
                assertTrue("$path 应引用前景图标", xml.contains("@mipmap/ic_launcher_foreground"))
                assertTrue("$path 应包含 Android 13 单色层", xml.contains("<monochrome"))
            }
    }

    @Test
    fun `各密度包含完整的新图标素材`() {
        val names = listOf(
            "ic_launcher.png",
            "ic_launcher_round.png",
            "ic_launcher_foreground.png",
            "ic_launcher_background.png",
            "ic_launcher_monochrome.png",
        )
        listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi").forEach { density ->
            names.forEach { name ->
                res("mipmap-$density/$name")
            }
        }
        res("drawable-nodpi/playstore_icon_512.png")
    }
}
