package ai.routin.mytoken.widget

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WidgetUsageSyncTest {

    @Test
    fun `usage changes are debounced into a single widget redraw`() = runTest {
        val states = MutableStateFlow(0)
        var redraws = 0
        val job = syncWidgetsOnUsageChanges(
            states = states,
            debounceMillis = 1_000,
            redrawAll = { redraws++ },
        )

        states.value = 1
        advanceTimeBy(300)
        states.value = 2
        advanceTimeBy(300)
        states.value = 3
        advanceTimeBy(1_500)

        assertEquals(1, redraws)
        job.cancel()
    }

    @Test
    fun `later usage changes trigger another redraw`() = runTest {
        val states = MutableStateFlow(0)
        var redraws = 0
        val job = syncWidgetsOnUsageChanges(
            states = states,
            debounceMillis = 1_000,
            redrawAll = { redraws++ },
        )

        states.value = 1
        advanceTimeBy(1_500)
        states.value = 2
        advanceTimeBy(1_500)

        assertEquals(2, redraws)
        job.cancel()
    }
}
