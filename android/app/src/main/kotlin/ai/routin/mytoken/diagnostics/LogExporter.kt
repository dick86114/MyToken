package ai.routin.mytoken.diagnostics

import ai.routin.mytoken.core.diagnostics.AppLog
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.WebView
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

data class LogExportResult(
    val fileName: String,
    /** 用户可见位置，例如“下载/MyToken-logs-….zip”。 */
    val savedLocation: String,
    val shareFile: File,
    val sizeBytes: Long,
    val includedEntries: List<String>,
)

/**
 * 把诊断日志打包成 zip，方便用户在真机上直接导出后发给我们。
 *
 * 内容：应用日志、设备信息、最近的应用内 logcat（尽力而为）、崩溃记录。
 * 同时发布一份到「下载」，并保留应用私有副本用于分享。
 */
class LogExporter(
    private val context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
    private val logcatReader: () -> String = ::readOwnLogcat,
    private val downloadsPublisher: (String, ByteArray) -> String? = { name, bytes ->
        publishToDownloads(context, name, bytes)
    },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun export(): LogExportResult = withContext(ioDispatcher) {
        val fileName = "MyToken-logs-${timestamp()}.zip"
        val entries = linkedMapOf<String, ByteArray>()

        AppLog.recentText().takeIf(String::isNotEmpty)?.let { entries["app.log"] = it.toByteArray() }
        entries["device.txt"] = deviceSummary().toByteArray()
        crashLog()?.let { entries["crash_log.txt"] = it.toByteArray() }
        logcatReader().takeIf(String::isNotBlank)?.let { entries["logcat.txt"] = it.toByteArray() }

        val bytes = zip(entries)
        val shareDirectory = File(context.getExternalFilesDir(null) ?: context.filesDir, LOG_DIRECTORY)
        shareDirectory.mkdirs()
        val shareFile = File(shareDirectory, fileName)
        shareFile.writeBytes(bytes)

        val published = runCatching { downloadsPublisher(fileName, bytes) }.getOrNull()
        LogExportResult(
            fileName = fileName,
            savedLocation = published ?: shareFile.absolutePath,
            shareFile = shareFile,
            sizeBytes = bytes.size.toLong(),
            includedEntries = entries.keys.toList(),
        )
    }

    private fun deviceSummary(): String = buildString {
        appendLine("MyToken 诊断信息")
        appendLine("导出时间: ${displayTimestamp()}")
        appendLine("应用版本: ${appVersion()}")
        appendLine("包名: ${context.packageName}")
        appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        appendLine("设备: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
        appendLine("WebView: ${webViewVersion()}")
        appendLine("语言: ${Locale.getDefault()}")
        appendLine("时区: ${TimeZone.getDefault().id}")
    }

    private fun appVersion(): String = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        "${info.versionName} (${info.longVersionCode})"
    }.getOrDefault("未知")

    private fun webViewVersion(): String = runCatching {
        WebView.getCurrentWebViewPackage()?.let { "${it.packageName} ${it.versionName}" }
    }.getOrNull() ?: "未知"

    private fun crashLog(): String? {
        val file = File(context.getExternalFilesDir(null) ?: context.filesDir, CRASH_FILE)
        return runCatching { file.takeIf(File::exists)?.readText() }.getOrNull()
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(clock()))

    private fun displayTimestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(clock()))

    private fun zip(entries: Map<String, ByteArray>): ByteArray {
        val buffer = ByteArrayOutputStream()
        ZipOutputStream(buffer).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return buffer.toByteArray()
    }

    private companion object {
        const val LOG_DIRECTORY = "logs"
        const val CRASH_FILE = "crash_log.txt"
        const val LOGCAT_LINE_LIMIT = 3000
        const val LOGCAT_CHAR_LIMIT = 200_000

        /** 应用只能读到自己的日志，这里拿最近一段用于排查启动/网络问题。 */
        fun readOwnLogcat(): String = runCatching {
            val process = ProcessBuilder(
                "logcat", "-d", "-v", "threadtime", "-t", LOGCAT_LINE_LIMIT.toString(),
            ).redirectErrorStream(true).start()
            val text = process.inputStream.bufferedReader().use { it.readText() }
            process.destroy()
            text.takeLast(LOGCAT_CHAR_LIMIT)
        }.getOrDefault("")

        /** 发布到「下载」目录，用户在文件管理器里能直接找到。 */
        fun publishToDownloads(context: Context, name: String, bytes: ByteArray): String? {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/MyToken")
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null
            resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return null
            return "下载/MyToken/$name"
        }
    }
}
