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
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
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
        assertEquals(MyTokenVisualPolicy.Material.Solid, MyTokenVisualPolicy.materialFor(MyTokenVisualPolicy.SurfaceRole.Window))
        assertEquals(MyTokenVisualPolicy.Material.Solid, MyTokenVisualPolicy.materialFor(MyTokenVisualPolicy.SurfaceRole.Card))
        assertEquals(MyTokenVisualPolicy.Material.Overlay, MyTokenVisualPolicy.materialFor(MyTokenVisualPolicy.SurfaceRole.Modal))
        assertFalse(MyTokenVisualPolicy.allowsShadow(MyTokenVisualPolicy.SurfaceRole.Card))
        assertFalse(MyTokenVisualPolicy.allowsShadow(MyTokenVisualPolicy.SurfaceRole.Window))
        assertTrue(MyTokenVisualPolicy.allowsShadow(MyTokenVisualPolicy.SurfaceRole.Modal))
    }

    @Test
    fun 方案A主题锁定报告黄与状态色() {
        var lightPrimary = Color.Unspecified
        var darkPrimary = Color.Unspecified
        var lightPositive = Color.Unspecified
        var darkDanger = Color.Unspecified
        var darkOnPrimary = Color.Unspecified

        composeRule.setContent {
            MyTokenTheme(darkTheme = false) {
                lightPrimary = MaterialTheme.colorScheme.primary
                lightPositive = MaterialTheme.colorScheme.secondary
            }
            MyTokenTheme(darkTheme = true) {
                darkPrimary = MaterialTheme.colorScheme.primary
                darkDanger = MaterialTheme.colorScheme.error
                darkOnPrimary = MaterialTheme.colorScheme.onPrimary
            }
        }

        assertEquals(0xFFF3CB73.toInt(), lightPrimary.toArgb())
        assertEquals(0xFFF3CB73.toInt(), darkPrimary.toArgb())
        assertEquals(0xFF30C48D.toInt(), lightPositive.toArgb())
        assertEquals(0xFFFF6369.toInt(), darkDanger.toArgb())
        assertEquals(0xFF17181A.toInt(), darkOnPrimary.toArgb())
    }

    @Test
    @Config(qualifiers = "w240dp-h640dp")
    fun 窄屏简洁卡片完整展示长重置信息() {
        val end = Instant.now().plus(Duration.ofDays(365))
        val expiry = Instant.now().plus(Duration.ofDays(45))
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
                subscriptionEndAt = expiry,
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

        val resetText = formatResetTime(end)
        composeRule.onNodeWithText("长周期套餐").assertExists()
        composeRule.onAllNodesWithText("后重置", substring = true, useUnmergedTree = true)
            .onFirst()
            .assertExists()
        composeRule.onAllNodesWithText(resetText, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .forEach { node ->
                val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                val config = node.config
                val getLayoutAction = config[SemanticsActions.GetTextLayoutResult]
                getLayoutAction?.action?.invoke(layouts)
                if (layouts.isNotEmpty()) {
                    assertFalse(
                        "重置时间不应被截断",
                        layouts.single().hasVisualOverflow,
                    )
                }
            }
    }

    @Test
    @Config(qualifiers = "w280dp-h1200dp")
    fun 窄屏完整配额卡片保留窗口与重置信息() {
        val end = Instant.now().plus(Duration.ofHours(2))
        val credential = Credential(
            id = UUID.randomUUID(),
            providerId = ProviderId.Glm,
            credentialKind = CredentialKind.ApiKey,
            name = "GLM 窄屏",
        )
        val card = CredentialCardUi(
            credential = credential,
            status = RefreshStatus.Ready,
            snapshot = UsageSnapshot(
                credentialId = credential.id,
                fetchedAt = Instant.now(),
                metrics = listOf(
                    progressMetric("five-hour", "5 小时用量", end).copy(
                        resetCards = listOf(
                            ai.routin.mytoken.domain.model.UsageResetCard(
                                expiresAt = Instant.now().plus(Duration.ofDays(3)),
                            ),
                        ),
                    ),
                    progressMetric("weekly", "每周用量", end.plus(Duration.ofDays(4))),
                    UsageMetric(
                        id = "model-calls",
                        label = "模型调用量",
                        value = BigDecimal("1888"),
                        unit = UsageMetricUnit.Request,
                        presentation = UsageMetricPresentation.Value,
                        semantic = UsageMetricSemantic.Value,
                        healthState = UsageMetricHealthState.Normal,
                    ),
                ),
                planName = "Coding Plan",
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
                        fontScale = 1.25f,
                    ),
                ) {
                    CredentialUsageCard(
                        card = card,
                        onOpen = {},
                        onRetry = {},
                        usageCardDensity = UsageCardDensity.FULL,
                    )
                }
            }
        }

        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        listOf("5 小时用量", "本周用量").forEach { label ->
            val bounds = composeRule.onNodeWithText(label).getUnclippedBoundsInRoot()
            assertTrue("$label should stay inside the card", bounds.left >= root.left)
            assertTrue("$label should stay inside the card", bounds.right <= root.right)
        }
        composeRule.onNodeWithContentDescription("1 张重置卡").assertExists()
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
