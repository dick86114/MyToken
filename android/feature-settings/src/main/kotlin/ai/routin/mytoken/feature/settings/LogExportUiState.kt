package ai.routin.mytoken.feature.settings

/** 「关于」长按导出诊断日志的状态。 */
sealed interface LogExportUiState {
    data object Idle : LogExportUiState

    data object Working : LogExportUiState

    data class Done(
        val fileName: String,
        val savedLocation: String,
        val sizeBytes: Long,
    ) : LogExportUiState

    data class Failed(val message: String) : LogExportUiState
}
