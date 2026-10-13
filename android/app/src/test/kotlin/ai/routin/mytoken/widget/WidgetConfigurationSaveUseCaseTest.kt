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
            refreshNow = { config ->
                assertEquals(WidgetKind.Credential, config.kind)
                calls += "refresh-now"
            },
            updateWidget = { appWidgetId ->
                assertEquals(7, appWidgetId)
                calls += "update"
            },
        )

        useCase.apply(7, config)

        assertEquals(listOf("save", "refresh", "refresh-now", "update"), calls)
    }
}
