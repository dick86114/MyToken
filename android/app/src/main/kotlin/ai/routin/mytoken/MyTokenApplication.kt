package ai.routin.mytoken

import ai.routin.mytoken.core.notifications.NotificationChannels
import ai.routin.mytoken.core.refresh.RefreshScheduling
import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 应用级依赖图；自动刷新已移除，仅清理旧版本遗留的周期任务。 */
class MyTokenApplication : Application() {

    val graph: AppGraph by lazy { AppGraph(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // 崩溃日志写到外部文件，方便无 adb 时定位
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val dir = getExternalFilesDir(null) ?: filesDir
                val file = java.io.File(dir, "crash_log.txt")
                file.appendText("\n=== ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())} ===\n")
                file.appendText("Thread: ${thread.name}\n")
                file.appendText(throwable.stackTraceToString() + "\n")
                throwable.cause?.let { file.appendText("Caused by:\n${it.stackTraceToString()}\n") }
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }

        NotificationChannels.ensureChannels(this)
        appScope.launch {
            runCatching { RefreshScheduling.apply(this@MyTokenApplication, null) }
        }
    }
}
