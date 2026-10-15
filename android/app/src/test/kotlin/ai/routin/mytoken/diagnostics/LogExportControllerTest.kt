package ai.routin.mytoken.diagnostics

import ai.routin.mytoken.feature.settings.LogExportUiState
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LogExportControllerTest {

    private fun exporter(
        context: Context,
        ioDispatcher: CoroutineDispatcher,
        downloadsPublisher: (String, ByteArray) -> String? = { _, _ -> null },
    ) = LogExporter(
        context = context,
        logcatReader = { "" },
        downloadsPublisher = downloadsPublisher,
        ioDispatcher = ioDispatcher,
    )

    @Test
    fun `导出成功后状态带上文件名位置与大小`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = LogExportController(
            context = context,
            scope = this,
            exporter = exporter(context, UnconfinedTestDispatcher(testScheduler)) { name, _ ->
                "下载/MyToken/$name"
            },
        )

        controller.export()
        assertEquals(LogExportUiState.Working, controller.state.value)
        advanceUntilIdle()

        val state = controller.state.value
        assertTrue("导出应完成，实际为 $state", state is LogExportUiState.Done)
        state as LogExportUiState.Done
        assertTrue(state.fileName.startsWith("MyToken-logs-"))
        assertEquals("下载/MyToken/${state.fileName}", state.savedLocation)
        assertTrue(state.sizeBytes > 0)
    }

    @Test
    fun `导出失败时给出失败状态`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = LogExportController(
            context = context,
            scope = this,
            exporter = LogExporter(
                context = context,
                clock = { error("时钟不可用") },
                logcatReader = { "" },
                downloadsPublisher = { _, _ -> null },
                ioDispatcher = UnconfinedTestDispatcher(testScheduler),
            ),
        )

        controller.export()
        advanceUntilIdle()

        assertTrue(controller.state.value is LogExportUiState.Failed)
    }

    @Test
    fun `导出完成后可以分享并回到空闲`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shared = mutableListOf<String>()
        val controller = LogExportController(
            context = context,
            scope = this,
            exporter = exporter(context, UnconfinedTestDispatcher(testScheduler)),
            shareLauncher = { intent -> shared += intent.type.orEmpty() },
        )

        controller.export()
        advanceUntilIdle()
        controller.share()
        assertEquals(listOf("application/zip"), shared)

        controller.dismiss()
        assertEquals(LogExportUiState.Idle, controller.state.value)
    }

    @Test
    fun `没有导出结果时分享不触发`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var shareInvoked = false
        val controller = LogExportController(
            context = context,
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
            exporter = exporter(
                context,
                ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
            ),
            shareLauncher = { shareInvoked = true },
        )

        controller.share()

        assertFalse(shareInvoked)
        assertEquals(LogExportUiState.Idle, controller.state.value)
    }
}
