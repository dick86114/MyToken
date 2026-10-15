package ai.routin.mytoken.core.diagnostics

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLogStoreTest {

    private fun temporaryStore(maxBytes: Int = 64 * 1024): AppLogStore {
        val directory = Files.createTempDirectory("mytoken-log-test").toFile()
        return AppLogStore(directory = directory, maxBytes = maxBytes)
    }

    @Test
    fun `记录内容包含时间级别事件与详情`() {
        val store = temporaryStore()

        store.log(AppLogLevel.Info, "xiaomi_login_page_started", "url=https://account.xiaomi.com/pass")

        val text = store.recentText()
        assertTrue(text.contains("[info]"))
        assertTrue(text.contains("xiaomi_login_page_started"))
        assertTrue(text.contains("url=https://account.xiaomi.com/pass"))
        // 时间戳能让用户和我对齐事件顺序。
        assertTrue(Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}""").containsMatchIn(text))
    }

    @Test
    fun `日志导出前必须脱敏凭证`() {
        val store = temporaryStore()

        store.log(
            AppLogLevel.Info,
            "cookie_captured",
            "Cookie: api-platform_serviceToken=SECRET1; userId=12345; sessionToken=SECRET2",
        )
        store.log(AppLogLevel.Info, "request", "apiKey=SECRET3&token=SECRET4 password=SECRET5")

        val text = store.recentText()
        assertFalse("不能把票据原文写进日志", text.contains("SECRET1"))
        assertFalse(text.contains("SECRET2"))
        assertFalse(text.contains("SECRET3"))
        assertFalse(text.contains("SECRET4"))
        assertFalse(text.contains("SECRET5"))
        assertTrue(text.contains("<redacted>"))
    }

    @Test
    fun `导出只保留最新的部分内容`() {
        val store = temporaryStore()
        repeat(50) { index -> store.log(AppLogLevel.Info, "event_$index") }

        val text = store.recentText(maxCharacters = 200)

        assertTrue(text.length <= 200)
        assertTrue("应保留最新事件", text.contains("event_49"))
        assertFalse("不应包含最早的事件", text.contains("event_0\n"))
    }

    @Test
    fun `日志文件超过上限时自动裁剪且保留最新记录`() {
        val store = temporaryStore(maxBytes = 2 * 1024)

        repeat(200) { index -> store.log(AppLogLevel.Warning, "event_$index", "payload" + "x".repeat(50)) }

        assertTrue("文件必须受上限约束", store.file.length() <= 4 * 1024)
        val text = store.recentText()
        assertTrue(text.contains("event_199"))
        assertFalse(text.contains("event_0 "))
    }

    @Test
    fun `未初始化时调用不抛异常`() {
        val file = File(Files.createTempDirectory("mytoken-applog-uninit").toFile(), "app.log")
        AppLog.install(AppLogStore(file.parentFile!!))

        AppLog.log(AppLogLevel.Error, "boom", "details")

        assertTrue(AppLog.recentText().contains("boom"))
    }

    @Test
    fun `崩溃堆栈也会进日志`() {
        val store = temporaryStore()

        store.logThrowable("uncaught_exception", IllegalStateException("测试崩溃"))

        val text = store.recentText()
        assertTrue(text.contains("uncaught_exception"))
        assertTrue(text.contains("IllegalStateException"))
        assertTrue(text.contains("测试崩溃"))
    }
}
