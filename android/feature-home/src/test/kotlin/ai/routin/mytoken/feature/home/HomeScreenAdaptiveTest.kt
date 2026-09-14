package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenTheme
import ai.routin.mytoken.core.ui.rememberMyTokenLayoutMode
import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import ai.routin.mytoken.domain.model.UsageSnapshot
import ai.routin.mytoken.domain.usage.RefreshStatus
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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

    private fun setContent() {
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(
                            sampleCard("Routin 主账号", ProviderId.Routin),
                            sampleCard("DeepSeek 主账号", ProviderId.DeepSeek),
                            sampleCard("GLM 主账号", ProviderId.Glm),
                        ),
                        groups = emptyList(),
                        credentialCount = 3,
                    ),
                    layoutMode = rememberMyTokenLayoutMode(),
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
    fun 手机使用单列首页() {
        setContent()
        composeRule.onNodeWithTag("home_grid_1_columns").assertExists()
    }

    @Test
    @Config(qualifiers = "w700dp-h1100dp")
    fun 阔折叠或横屏使用双列首页() {
        setContent()
        composeRule.onNodeWithTag("home_grid_2_columns").assertExists()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1100dp")
    fun 平板使用三列首页() {
        setContent()
        composeRule.onNodeWithTag("home_grid_3_columns").assertExists()
    }
}

private fun sampleCard(name: String, provider: ProviderId): CredentialCardUi {
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
        used = BigDecimal("42"),
        limit = BigDecimal("100"),
        remaining = BigDecimal("58"),
        unit = UsageMetricUnit.Currency,
        presentation = UsageMetricPresentation.Progress,
        semantic = UsageMetricSemantic.UsedQuota,
        currencyCode = "USD",
        healthState = UsageMetricHealthState.Normal,
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
