package ai.routin.mytoken.widget

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import ai.routin.mytoken.domain.model.UsageSnapshot
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WidgetConfigurationUiStateTest {

    @Test
    fun `overview configuration defaults to all enabled and auto style`() {
        val enabled = credential("11111111-1111-4111-8111-111111111111", sortOrder = 1)
        val disabled = credential("22222222-2222-4222-8222-222222222222", enabled = false)

        val state = resolveConfigurationUiState(
            config = WidgetConfig(kind = WidgetKind.Overview),
            credentials = listOf(disabled, enabled),
            snapshots = emptyMap(),
        )

        assertEquals(WidgetDisplayStyle.FocusRing, state.selectedStyle)
        assertEquals(WidgetOverviewSource.AllEnabled, state.selectedOverviewSource)
        assertEquals(listOf(enabled.id), state.overviewCredentials.map { it.id })
    }

    @Test
    fun `manual selection preserves order and caps at six`() {
        val credentials = (0 until 8).map { index ->
            credential("0000000%d-1111-4111-8111-11111111111%d".format(index, index), sortOrder = index)
        }
        val selected = listOf(5, 1, 7, 0, 3, 2, 6, 4).map { credentials[it].id }

        val state = resolveConfigurationUiState(
            config = WidgetConfig(
                kind = WidgetKind.Overview,
                overviewSource = WidgetOverviewSource.Manual,
                overviewCredentialIds = selected,
            ),
            credentials = credentials,
            snapshots = emptyMap(),
        )

        assertEquals(selected.take(6), state.overviewCredentials.map { it.id })
    }

    @Test
    fun `configuration ui exposes only approved options`() {
        val state = resolveConfigurationUiState(
            config = WidgetConfig(),
            credentials = emptyList(),
            snapshots = emptyMap(),
        )

        assertEquals(
            listOf(WidgetDisplayStyle.FocusRing, WidgetDisplayStyle.ScanBand, WidgetDisplayStyle.SignalTiles),
            state.allowedStyles,
        )
        assertEquals(WidgetOverviewSource.entries, state.allowedOverviewSources)
        assertFalse(state.supportsSecondaryMetricOrdering)
        assertFalse(state.supportsColorTheme)
        assertFalse(state.supportsRefreshInterval)
    }

    @Test
    fun `overview renderer receives risk sorted presentations`() {
        val unavailable = credential("11111111-1111-4111-8111-111111111111")
        val critical = credential("22222222-2222-4222-8222-222222222222")
        val normal = credential("33333333-3333-4333-8333-333333333333")

        val state = resolveConfigurationUiState(
            config = WidgetConfig(kind = WidgetKind.Overview),
            credentials = listOf(normal, critical, unavailable),
            snapshots = mapOf(
                unavailable.id to snapshot(unavailable.id, UsageMetricHealthState.Unavailable, 10),
                critical.id to snapshot(critical.id, UsageMetricHealthState.Critical, 96),
                normal.id to snapshot(normal.id, UsageMetricHealthState.Normal, 25),
            ),
            size = WidgetSizeKind.Expanded,
        )

        assertEquals(
            listOf(unavailable.id, critical.id, normal.id),
            state.preview?.credentials?.map { it.id },
        )
    }

    private fun credential(
        rawId: String,
        enabled: Boolean = true,
        sortOrder: Int = 0,
    ) = Credential(
        id = UUID.fromString(rawId),
        providerId = ProviderId.Routin,
        credentialKind = CredentialKind.BearerApiKey,
        name = "凭证 $sortOrder",
        isEnabled = enabled,
        sortOrder = sortOrder,
    )

    private fun snapshot(
        id: UUID,
        health: UsageMetricHealthState,
        percent: Int,
    ) = UsageSnapshot(
        credentialId = id,
        fetchedAt = Instant.parse("2026-01-01T00:00:00Z"),
        metrics = listOf(
            UsageMetric(
                id = "five-hour",
                label = "五小时",
                used = percent.toBigDecimal(),
                limit = 100.toBigDecimal(),
                remaining = (100 - percent).toBigDecimal(),
                unit = UsageMetricUnit.Token,
                presentation = UsageMetricPresentation.Progress,
                semantic = UsageMetricSemantic.UsedQuota,
                healthState = health,
            ),
        ),
    )
}
