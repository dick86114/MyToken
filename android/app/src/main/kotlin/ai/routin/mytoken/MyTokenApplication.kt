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

        NotificationChannels.ensureChannels(this)
        appScope.launch {
            runCatching { RefreshScheduling.apply(this@MyTokenApplication, null) }
        }
    }
}
