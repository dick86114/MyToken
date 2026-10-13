package ai.routin.mytoken.widget

import ai.routin.mytoken.domain.model.UsageSnapshot
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetConfigurationPreviewLoaderTest {

    @Test
    fun `preview loader falls back to cached snapshot when memory state is empty`() = runTest {
        val id = UUID.randomUUID()
        val cached = UsageSnapshot(
            credentialId = id,
            fetchedAt = Instant.parse("2026-01-01T00:00:00Z"),
            metrics = emptyList(),
        )

        val loaded = loadWidgetConfigurationSnapshots(
            credentialIds = listOf(id),
            stateSnapshot = { null },
            cachedSnapshot = { cached },
        )

        assertEquals(cached, loaded[id])
    }

    @Test
    fun `preview loader prefers in-memory snapshot over cache`() = runTest {
        val id = UUID.randomUUID()
        val fresh = UsageSnapshot(
            credentialId = id,
            fetchedAt = Instant.parse("2026-01-02T00:00:00Z"),
            metrics = emptyList(),
        )
        val cached = UsageSnapshot(
            credentialId = id,
            fetchedAt = Instant.parse("2026-01-01T00:00:00Z"),
            metrics = emptyList(),
        )

        val loaded = loadWidgetConfigurationSnapshots(
            credentialIds = listOf(id),
            stateSnapshot = { fresh },
            cachedSnapshot = { cached },
        )

        assertEquals(fresh, loaded[id])
    }
}
