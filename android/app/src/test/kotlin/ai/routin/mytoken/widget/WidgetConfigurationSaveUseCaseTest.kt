package ai.routin.mytoken.widget

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetConfigurationSaveUseCaseTest {

    @Test
    fun `saving widget config enqueues refresh before redrawing`() = runTest {
        val calls = mutableListOf<String>()
        val config = WidgetConfig(
            kind = WidgetKind.Credential,
            credentialId = java.util.UUID.randomUUID(),
            style = WidgetDisplayStyle.SignalTiles,
        )
        val useCase = WidgetConfigurationSaveUseCase(
            save = { appWidgetId, savedConfig ->
                assertEquals(7, appWidgetId)
                assertEquals(config, savedConfig)
                calls += "save"
            },
            enqueueRefresh = { calls += "refresh" },
            updateWidget = { appWidgetId ->
                assertEquals(7, appWidgetId)
                calls += "update"
            },
        )

        useCase.apply(7, config)

        assertEquals(listOf("save", "refresh", "update"), calls)
    }

    @Test
    fun `refresh still runs when direct redraw fails`() = runTest {
        var refreshCalls = 0
        val useCase = WidgetConfigurationSaveUseCase(
            save = { _, _ -> Unit },
            enqueueRefresh = { refreshCalls++ },
            updateWidget = { error("launcher rejected first draw") },
        )

        useCase.apply(11, WidgetConfig())

        assertEquals(1, refreshCalls)
    }
}
