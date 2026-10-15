package ai.routin.mytoken.core.diagnostics

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppLogLevel(val label: String) {
    Info("info"),
    Warning("warning"),
    Error("error"),
}

/**
 * 应用内的诊断日志文件。
 *
 * 用户可以在「设置 → 关于」长按导出这份日志，所以写入前必须脱敏：
 * 任何 Cookie、Token、API Key、密码、签名参数都只留字段名。
 */
class AppLogStore(
    private val directory: File,
    private val maxBytes: Int = DEFAULT_MAX_BYTES,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val file: File get() = File(directory, FILE_NAME)

    private val lock = Any()

    fun log(level: AppLogLevel, event: String, details: String? = null) {
        val line = buildString {
            append(timestamp())
            append(" [").append(level.label).append("] ").append(event)
            if (!details.isNullOrEmpty()) {
                append(" - ").append(sanitize(details))
            }
            append('\n')
        }
        synchronized(lock) {
            runCatching {
                directory.mkdirs()
                file.appendText(line)
                trimToLimit()
            }
        }
    }

    fun logThrowable(event: String, throwable: Throwable) {
        log(AppLogLevel.Error, event, throwable.stackTraceToString())
    }

    fun recentText(maxCharacters: Int = DEFAULT_MAX_CHARACTERS): String {
        val text = synchronized(lock) {
            runCatching { if (file.exists()) file.readText() else "" }.getOrDefault("")
        }
        return if (maxCharacters <= 0 || text.length <= maxCharacters) {
            text
        } else {
            text.takeLast(maxCharacters)
        }
    }

    private fun timestamp(): String =
        (FORMATTER.get() ?: SimpleDateFormat(TIMESTAMP_PATTERN, Locale.getDefault()))
            .format(Date(clock()))

    /** 超过上限时只保留尾部，并切到整行边界，避免留下半行。 */
    private fun trimToLimit() {
        if (file.length() <= maxBytes) return
        val kept = file.readText().takeLast(maxBytes)
        val firstBreak = kept.indexOf('\n')
        file.writeText(if (firstBreak >= 0) kept.substring(firstBreak + 1) else kept)
    }

    private fun sanitize(raw: String): String {
        var result = raw.take(MAX_DETAIL_CHARS)
        SECRET_PATTERNS.forEach { pattern ->
            result = pattern.replace(result, "$1=<redacted>")
        }
        return result
    }

    companion object {
        const val FILE_NAME = "app.log"
        const val DEFAULT_MAX_BYTES: Int = 256 * 1024
        const val DEFAULT_MAX_CHARACTERS: Int = 60_000
        private const val MAX_DETAIL_CHARS = 2_000
        private const val TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS"

        private val FORMATTER = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat =
                SimpleDateFormat(TIMESTAMP_PATTERN, Locale.getDefault())
        }

        /** 命名凭证：apiKey=…、sessionToken=…、cookie=…、sign=… 等统一只留字段名。 */
        private val SECRET_PATTERNS = listOf(
            Regex(
                """(?i)\b([A-Za-z0-9_-]*(?:apikey|api[_-]?key|token|secret|password|passwd|cookie))""" +
                    """\s*[:=]\s*[^\s;&,"']+""",
            ),
            Regex(
                """(?i)\b(api-platform_servicetoken|api-platform_ph|api-platform_slh|servicetoken|""" +
                    """passtoken|cuserid|userid|authorization|signature|sign)\s*[:=]\s*[^\s;&,"']+""",
            ),
            Regex("""(?i)\b(bearer)\s+[A-Za-z0-9._~+/-]+=*"""),
        )
    }
}

/**
 * 进程级日志入口。未初始化时所有写入静默丢弃，
 * 这样独立模块的单测不会因为缺少日志设施而失败。
 */
object AppLog {
    @Volatile
    private var store: AppLogStore? = null

    fun init(context: Context) = install(AppLogStore(File(context.filesDir, "logs")))

    fun install(appLogStore: AppLogStore) {
        store = appLogStore
    }

    fun log(level: AppLogLevel, event: String, details: String? = null) {
        store?.log(level, event, details)
    }

    fun logThrowable(event: String, throwable: Throwable) {
        store?.logThrowable(event, throwable)
    }

    fun recentText(maxCharacters: Int = AppLogStore.DEFAULT_MAX_CHARACTERS): String =
        store?.recentText(maxCharacters).orEmpty()

    /** 导出时把文件原样打包，没有文件则返回 null。 */
    fun file(): File? = store?.file?.takeIf(File::exists)
}
