package ai.routin.mytoken.diagnostics

import ai.routin.mytoken.core.diagnostics.AppLog
import ai.routin.mytoken.core.diagnostics.AppLogLevel
import ai.routin.mytoken.core.diagnostics.AppLogStore
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LogExporterTest {

    @Test
    fun `导出包包含应用日志与设备信息`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val logDirectory = Files.createTempDirectory("mytoken-export").toFile()
        AppLog.install(AppLogStore(logDirectory))
        AppLog.log(AppLogLevel.Info, "xiaomi_login_page_started", "url=https://account.xiaomi.com/pass")

        val result = LogExporter(
            context = context,
            logcatReader = { "10-15 22:00:00.000 I XiaomiLogin: onPageStarted" },
            downloadsPublisher = { name, _ -> "下载/MyToken/$name" },
        ).export()

        assertTrue(result.fileName.startsWith("MyToken-logs-"))
        assertTrue(result.fileName.endsWith(".zip"))
        assertEquals("下载/MyToken/${result.fileName}", result.savedLocation)
        assertTrue(result.sizeBytes > 0)

        val entries = ZipFile(result.shareFile).use { zip ->
            zip.entries().toList().associate { entry -> entry.name to zip.getInputStream(entry).readBytes().decodeToString() }
        }
        assertTrue("应用日志要打进包里", entries.getValue("app.log").contains("xiaomi_login_page_started"))
        assertTrue(entries.contains("device.txt"))
        assertTrue(entries.contains("logcat.txt"))
        val device = entries.getValue("device.txt")
        assertTrue(device.contains("MyToken 诊断信息"))
        assertTrue(device.contains("Android:"))
        assertTrue(device.contains("设备:"))
    }

    @Test
    fun `日志为空时仍然导出设备信息`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppLog.install(AppLogStore(Files.createTempDirectory("mytoken-export-empty").toFile()))

        val result = LogExporter(
            context = context,
            logcatReader = { "" },
            downloadsPublisher = { _, _ -> null },
        ).export()

        assertFalse(result.includedEntries.contains("app.log"))
        assertFalse(result.includedEntries.contains("logcat.txt"))
        assertTrue(result.includedEntries.contains("device.txt"))
        // 发布到「下载」失败时退回应用私有目录，不能让导出整体失败。
        assertEquals(result.shareFile.absolutePath, result.savedLocation)
    }

    @Test
    fun `下载目录写入失败不影响导出`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppLog.install(AppLogStore(Files.createTempDirectory("mytoken-export-fail").toFile()))

        val result = LogExporter(
            context = context,
            logcatReader = { "" },
            downloadsPublisher = { _, _ -> error("MediaStore 不可用") },
        ).export()

        assertTrue(File(result.shareFile.absolutePath).exists())
        assertEquals(result.shareFile.absolutePath, result.savedLocation)
    }
}
