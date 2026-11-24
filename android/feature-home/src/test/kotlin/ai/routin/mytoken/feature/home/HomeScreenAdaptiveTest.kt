package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenTheme
import ai.routin.mytoken.core.ui.rememberMyTokenLayoutMode
import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageCardDensity
import ai.routin.mytoken.domain.model.UsageHealthThresholdConfig
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import ai.routin.mytoken.domain.model.UsageSnapshot
import ai.routin.mytoken.domain.usage.RefreshStatus
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeScreenAdaptiveTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(density: UsageCardDensity = UsageCardDensity.COMPACT) {
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(
                            sampleCard("Routin 主账号", ProviderId.Routin, used = 20.0),
                            sampleCard("DeepSeek 主账号", ProviderId.DeepSeek, used = 20.0),
                            sampleCard("GLM 主账号", ProviderId.Glm, used = 20.0),
                        ),
                        groups = emptyList(),
                        credentialCount = 3,
                    ),
                    layoutMode = rememberMyTokenLayoutMode(),
                    usageCardDensity = density,
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun 手机简洁模式使用双列首页() {
        setContent()
        composeRule.onNodeWithTag("home_grid_2_columns").assertExists()
    }

    @Test
    @Config(qualifiers = "w700dp-h1100dp")
    fun 阔折叠简洁模式使用两列瀑布流首页() {
        setContent()
        composeRule.onNodeWithTag("home_grid_2_columns").assertExists()
        composeRule.onNodeWithTag("home_wide_rail").assertExists()
        composeRule.onNodeWithTag("home_pager").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1100dp")
    fun 平板简洁模式使用三列瀑布流首页() {
        setContent()
        composeRule.onNodeWithTag("home_grid_3_columns").assertExists()
        composeRule.onNodeWithTag("home_wide_rail").assertExists()
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun 普通手机保持现有分页首页不启用三栏() {
        setContent()
        composeRule.onNodeWithTag("home_wide_rail").assertDoesNotExist()
        composeRule.onNodeWithTag("home_pager").assertExists()
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun 手机完整模式使用单列列表() {
        setContent(density = UsageCardDensity.FULL)
        composeRule.onNodeWithTag("home_list_full").assertExists()
    }

    @Test
    @Config(qualifiers = "w700dp-h1100dp")
    fun 阔屏完整模式使用双列列表() {
        setContent(density = UsageCardDensity.FULL)
        composeRule.onNodeWithTag("home_grid_2_columns").assertExists()
    }

    @Test
    @Config(qualifiers = "w390dp-h1100dp")
    fun fontScale13FallsBackToSingleColumn() {
        composeRule.setContent {
            MyTokenTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(LocalDensity.current.density, 1.3f),
                ) {
                    HomeScreen(
                        state = HomeUiState(
                            isLoading = false,
                            cards = listOf(
                                sampleCard("Routin 主账号", ProviderId.Routin, used = 20.0),
                                sampleCard("GLM 主账号", ProviderId.Glm, used = 20.0),
                            ),
                            groups = emptyList(),
                            credentialCount = 2,
                        ),
                        usageCardDensity = UsageCardDensity.COMPACT,
                        onRefreshAll = {},
                        onRefreshCredential = {},
                        onOpenCredential = {},
                        onImportFromMac = {},
                        onAddManually = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag("home_grid_1_columns").assertExists()
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun statusRowShowsAttentionCountAndFilterMenu() {
        val routin = sampleCard("Routin 主账号", ProviderId.Routin, used = 20.0)
        val deepSeek = sampleCard("DeepSeek 主账号", ProviderId.DeepSeek, used = 85.0)
        val glm = sampleCard("GLM 主账号", ProviderId.Glm, used = 96.0)
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(routin, deepSeek, glm),
                        groups = listOf(
                            ProviderGroupUi(ProviderId.Routin, "Routin", false, listOf(routin)),
                            ProviderGroupUi(ProviderId.DeepSeek, "DeepSeek", false, listOf(deepSeek)),
                            ProviderGroupUi(ProviderId.Glm, "智谱 GLM", false, listOf(glm)),
                        ),
                        credentialCount = 3,
                    ),
                    usageCardDensity = UsageCardDensity.COMPACT,
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                )
            }
        }

        composeRule.onNodeWithTag("home_provider_toolbar").assertIsDisplayed()
        composeRule.onNodeWithTag("report_attention_count").assertTextEquals("2")
        composeRule.onNodeWithTag("provider_filter_menu").performClick()
        composeRule.onNodeWithTag("provider_filter_item_deepseek").assertExists()
    }
}

internal fun sampleCard(
    name: String,
    provider: ProviderId,
    used: Double = 42.0,
): CredentialCardUi {
    val credential = Credential(
        id = UUID.randomUUID(),
        providerId = provider,
        credentialKind = CredentialKind.BearerApiKey,
        name = name,
    )
    val now = Instant.parse("2026-09-14T00:00:00Z")
    val metric = UsageMetric(
        id = "fiveHour",
        label = "5 小时",
        used = BigDecimal.valueOf(used),
        limit = BigDecimal.valueOf(100),
        remaining = BigDecimal.valueOf(100 - used),
        unit = UsageMetricUnit.Currency,
        presentation = UsageMetricPresentation.Progress,
        semantic = UsageMetricSemantic.UsedQuota,
        currencyCode = "USD",
        healthState = UsageHealthThresholdConfig.DEFAULT.stateForUsedPercent(used),
    )
    return CredentialCardUi(
        credential = credential,
        status = RefreshStatus.Ready,
        snapshot = UsageSnapshot(
            credentialId = credential.id,
            fetchedAt = now,
            metrics = listOf(metric),
            planName = "测试套餐",
        ),
        isStale = false,
        error = null,
        freshness = Freshness(FreshnessLevel.JUST_NOW, "刚刚更新"),
    )
}
