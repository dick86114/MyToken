package ai.routin.mytoken.widget

import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetConfigurationSaveUseCaseTest {

    @Test
    fun `saving widget config persists and enqueues refresh without blocking on redraw`() {
        val calls = mutableListOf<String>()
        val config = WidgetConfig(
            kind = WidgetKind.Credential,
            credentialId = UUID.randomUUID(),
        )
        val useCase = WidgetConfigurationSaveUseCase(
            save = { appWidgetId, savedConfig ->
                assertEquals(7, appWidgetId)
                assertEquals(config, savedConfig)
                calls += "save"
            },
            enqueueRefresh = { calls += "refresh" },
            redrawWidget = { calls += "redraw" },
        )

        useCase.apply(7, config)

        // 重绘不再计入保存路径，配置页可以立刻返回桌面。
        assertEquals(listOf("save", "refresh"), calls)
    }

    @Test
    fun `redraw swallows launcher rejection so refresh stays the durable path`() = runTest {
        var refreshCalls = 0
        val useCase = WidgetConfigurationSaveUseCase(
            save = { _, _ -> Unit },
            enqueueRefresh = { refreshCalls++ },
            redrawWidget = { error("launcher rejected first draw") },
        )

        useCase.apply(11, WidgetConfig())
        useCase.redraw(11)

        assertEquals(1, refreshCalls)
    }
}
