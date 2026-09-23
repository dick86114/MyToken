package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenTheme
import ai.routin.mytoken.core.ui.MyTokenVisualPolicy
import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageCardDensity
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import ai.routin.mytoken.domain.model.UsageSnapshot
import ai.routin.mytoken.domain.usage.RefreshStatus
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidVisualRegressionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 方案A材质契约只允许模态投影() {
        assertEquals(MyTokenVisualPolicy.Material.WindowGlass, MyTokenVisualPolicy.materialFor(MyTokenVisualPolicy.SurfaceRole.Window))
        assertEquals(MyTokenVisualPolicy.Material.Solid, MyTokenVisualPolicy.materialFor(MyTokenVisualPolicy.SurfaceRole.Card))
        assertEquals(MyTokenVisualPolicy.Material.ModalGlass, MyTokenVisualPolicy.materialFor(MyTokenVisualPolicy.SurfaceRole.Modal))
        assertFalse(MyTokenVisualPolicy.allowsShadow(MyTokenVisualPolicy.SurfaceRole.Card))
        assertFalse(MyTokenVisualPolicy.allowsShadow(MyTokenVisualPolicy.SurfaceRole.Window))
        assertTrue(MyTokenVisualPolicy.allowsShadow(MyTokenVisualPolicy.SurfaceRole.Modal))
    }

    @Test
    fun 方案A主题锁定品牌蓝与状态色() {
        var lightPrimary = Color.Unspecified
        var darkPrimary = Color.Unspecified
        var lightPositive = Color.Unspecified
        var darkDanger = Color.Unspecified

        composeRule.setContent {
            MyTokenTheme(darkTheme = false) {
                lightPrimary = MaterialTheme.colorScheme.primary
                lightPositive = MaterialTheme.colorScheme.secondary
            }
            MyTokenTheme(darkTheme = true) {
                darkPrimary = MaterialTheme.colorScheme.primary
                darkDanger = MaterialTheme.colorScheme.error
            }
        }

        assertEquals(0xFF2F80ED.toInt(), lightPrimary.toArgb())
        assertEquals(0xFF4D94F5.toInt(), darkPrimary.toArgb())
        assertEquals(0xFF2F9E78.toInt(), lightPositive.toArgb())
        assertEquals(0xFFD15B5B.toInt(), darkDanger.toArgb())
    }

    @Test
    @Config(qualifiers = "w240dp-h640dp")
    fun 窄屏简洁卡片完整展示长重置信息() {
        val end = Instant.now().plus(Duration.ofDays(365))
        val metrics = listOf(
            progressMetric("fiveHour", "5 小时", end),
            progressMetric("weekly", "周", end),
        )
        val card = CredentialCardUi(
            credential = Credential(
                id = UUID.randomUUID(),
                providerId = ProviderId.Routin,
                credentialKind = CredentialKind.BearerApiKey,
                name = "长信息账号",
            ),
            status = RefreshStatus.Ready,
            snapshot = UsageSnapshot(
                credentialId = UUID.randomUUID(),
                fetchedAt = Instant.now(),
                metrics = metrics,
                planName = "长周期套餐",
            ),
            isStale = false,
            error = null,
            freshness = Freshness(FreshnessLevel.JUST_NOW, "刚刚更新"),
        )

        composeRule.setContent {
            MyTokenTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = LocalDensity.current.density,
                        fontScale = 1.35f,
                    ),
                ) {
                    CredentialUsageCard(
                        card = card,
                        onOpen = {},
                        onRetry = {},
                        usageCardDensity = UsageCardDensity.COMPACT,
                        modifier = androidx.compose.ui.Modifier.testTag("compact_card"),
                    )
                }
            }
        }

        val resetText = "重置 ${formatResetTime(end)}"
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        composeRule.onAllNodesWithText(resetText, useUnmergedTree = true)
            .onFirst()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                action(layouts)
            }

        assertFalse(layouts.single().hasVisualOverflow)
    }

    private fun progressMetric(
        id: String,
        label: String,
        end: Instant,
    ) = UsageMetric(
        id = id,
        label = label,
        used = BigDecimal("42"),
        limit = BigDecimal("100"),
        remaining = BigDecimal("58"),
        unit = UsageMetricUnit.Currency,
        windowEnd = end,
        presentation = UsageMetricPresentation.Progress,
        semantic = UsageMetricSemantic.UsedQuota,
        currencyCode = "USD",
        healthState = UsageMetricHealthState.Normal,
    )
}
