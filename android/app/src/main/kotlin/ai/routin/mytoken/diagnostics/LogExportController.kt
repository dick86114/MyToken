package ai.routin.mytoken.diagnostics

import ai.routin.mytoken.core.diagnostics.AppLog
import ai.routin.mytoken.core.diagnostics.AppLogLevel
import ai.routin.mytoken.feature.settings.LogExportUiState
import android.content.Context
import android.content.ClipData
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 驱动「关于」长按导出日志：打包、提示结果、并按需分享。 */
class LogExportController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val exporter: LogExporter = LogExporter(context),
    private val shareLauncher: (Intent) -> Unit = { intent ->
        context.startActivity(
            Intent.createChooser(intent, "分享 MyToken 日志")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    },
) {
    private val _state = MutableStateFlow<LogExportUiState>(LogExportUiState.Idle)
    val state: StateFlow<LogExportUiState> = _state.asStateFlow()

    private var lastResult: LogExportResult? = null

    fun export() {
        if (_state.value is LogExportUiState.Working) return
        _state.value = LogExportUiState.Working
        AppLog.log(AppLogLevel.Info, "log_export_started", null)
        scope.launch {
            runCatching { exporter.export() }.fold(
                onSuccess = { result ->
                    lastResult = result
                    AppLog.log(
                        AppLogLevel.Info,
                        "log_export_succeeded",
                        "file=${result.fileName} entries=${result.includedEntries.size} location=${result.savedLocation}",
                    )
                    _state.value = LogExportUiState.Done(
                        fileName = result.fileName,
                        savedLocation = result.savedLocation,
                        sizeBytes = result.sizeBytes,
                    )
                },
                onFailure = { error ->
                    AppLog.logThrowable("log_export_failed", error)
                    _state.value = LogExportUiState.Failed(error.message ?: "导出日志失败")
                },
            )
        }
    }

    fun share() {
        val result = lastResult ?: return
        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.update.fileprovider",
                result.shareFile,
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "MyToken 诊断日志 ${result.fileName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // 分享面板与目标应用都通过 clipData 取授权，否则对方读不到这个文件。
                clipData = ClipData.newUri(context.contentResolver, result.fileName, uri)
            }
            shareLauncher(intent)
        }.onFailure { error ->
            _state.value = LogExportUiState.Failed("分享失败：${error.message ?: "无法打开分享面板"}")
        }
    }

    fun dismiss() {
        _state.value = LogExportUiState.Idle
    }
}
