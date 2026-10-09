package ai.routin.mytoken.provider

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.CredentialSecret
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import ai.routin.mytoken.domain.usage.UsageProviderException
import ai.routin.mytoken.provider.http.HttpTransport
import ai.routin.mytoken.provider.http.ProviderHttpRequest
import ai.routin.mytoken.provider.http.ProviderHttpResponse
import ai.routin.mytoken.provider.opencode.OpenCodeUsageProvider
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class OpenCodeUsageProviderTest {
    private val fixedClock = Clock.fixed(Instant.parse("2026-10-09T12:00:00Z"), ZoneOffset.UTC)

    private fun credential() = Credential(
        id = UUID.fromString("22222222-2222-4222-8222-222222222222"),
        providerId = ProviderId.OpenCode,
        credentialKind = CredentialKind.BearerApiKey,
        name = "OpenCode",
    )

    private fun statusBody(
        product: String = "go",
        cancelAtPeriodEnd: Boolean = false,
        fiveHourUsed: String = "600000000",
        fiveHourLimit: String = "3000000000",
        weeklyUsed: String = "1500000000",
        weeklyLimit: String = "7500000000",
        monthlyUsed: String = "4500000000",
        monthlyLimit: String = "15000000000",
    ) = """
        {
          "subscriberUserId": "user_1",
          "product": "$product",
          "renewalProduct": "$product",
          "cancelAtPeriodEnd": $cancelAtPeriodEnd,
          "renewalPending": false,
          "access": {
            "startsAt": "2026-10-01T00:00:00Z",
            "endsAt": "2026-11-01T00:00:00Z",
            "meters": {
              "fiveHour": {
                "startsAt": "2026-10-09T08:00:00Z",
                "resetsAt": "2026-10-09T13:00:00Z",
                "limitMicroCents": "$fiveHourLimit",
                "usedMicroCents": "$fiveHourUsed"
              },
              "week": {
                "startsAt": "2026-10-05T00:00:00Z",
                "resetsAt": "2026-10-12T00:00:00Z",
                "limitMicroCents": "$weeklyLimit",
                "usedMicroCents": "$weeklyUsed"
              },
              "month": {
                "resetsAt": "2026-11-01T00:00:00Z",
                "limitMicroCents": "$monthlyLimit",
                "usedMicroCents": "$monthlyUsed"
              }
            }
          }
        }
    """.trimIndent()

    @Test
    fun `maps go status to three usage metrics`() = runTest {
        val request = transport(200, statusBody())
        val provider = OpenCodeUsageProvider(request, fixedClock)

        val snapshot = provider.fetchUsage(
            credential(),
            CredentialSecret.BearerToken("oc_sk_test"),
        ).getOrThrow()

        assertEquals("OpenCode Go", snapshot.planName)
        assertEquals("正常续费", snapshot.statusText)
        assertEquals("自动续费", snapshot.billingMode)
        assertEquals(Instant.parse("2026-10-01T00:00:00Z"), snapshot.subscriptionStartAt)
        assertEquals(Instant.parse("2026-11-01T00:00:00Z"), snapshot.subscriptionEndAt)
        assertEquals(listOf("fiveHour", "weekly", "monthly"), snapshot.metrics.map { it.id })

        val fiveHour = snapshot.metrics.single { it.id == "fiveHour" }
        assertDecimal("6", fiveHour.used)
        assertDecimal("30", fiveHour.limit)
        assertDecimal("24", fiveHour.remaining)
        assertEquals(Instant.parse("2026-10-09T08:00:00Z"), fiveHour.windowStart)
        assertEquals(Instant.parse("2026-10-09T13:00:00Z"), fiveHour.windowEnd)
        assertEquals(UsageMetricUnit.Currency, fiveHour.unit)
        assertEquals(UsageMetricPresentation.Progress, fiveHour.presentation)
        assertEquals(UsageMetricSemantic.UsedQuota, fiveHour.semantic)
        assertEquals("$", fiveHour.currencyCode)

        assertDecimal("15", snapshot.metrics.single { it.id == "weekly" }.used)
        assertDecimal("75", snapshot.metrics.single { it.id == "weekly" }.limit)
        assertDecimal("45", snapshot.metrics.single { it.id == "monthly" }.used)
    }

    @Test
    fun `maps go plus product and lifecycle dates`() = runTest {
        val provider = OpenCodeUsageProvider(
            transport(
                200,
                statusBody(
                    product = "go-plus",
                    cancelAtPeriodEnd = true,
                    monthlyUsed = "120000000",
                    monthlyLimit = "100000000",
                ),
            ),
            fixedClock,
        )

        val snapshot = provider.fetchUsage(
            credential(),
            CredentialSecret.BearerToken("oc_sk_test"),
        ).getOrThrow()

        assertEquals("OpenCode Go Plus", snapshot.planName)
        assertEquals("将在当前周期结束后到期", snapshot.statusText)
        assertEquals("取消续订", snapshot.billingMode)
        val monthly = snapshot.metrics.single { it.id == "monthly" }
        assertDecimal("1.2", monthly.used)
        assertDecimal("1", monthly.limit)
        assertDecimal("0", monthly.remaining)
        assertEquals(UsageMetricHealthState.Critical, monthly.healthState)
    }

    @Test
    fun `rejects null status with provider message`() = runTest {
        val provider = OpenCodeUsageProvider(transport(200, "null"), fixedClock)

        val result = provider.fetchUsage(credential(), CredentialSecret.BearerToken("oc_sk_test"))

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is UsageProviderException.ProviderMessage)
        assertEquals("OpenCode：未找到有效 Go 订阅", error.message)
    }

    @Test
    fun `maps http errors to diagnostic exceptions`() = runTest {
        val cases = listOf<Pair<Int, UsageProviderException>>(
            400 to UsageProviderException.InvalidResponse(),
            401 to UsageProviderException.Unauthorized(),
            403 to UsageProviderException.ProviderMessage(
                "OpenCode：API Key 没有 Console 状态权限，请创建带读取权限的服务账号 Key",
            ),
            404 to UsageProviderException.ProviderMessage("OpenCode：未找到订阅或接口路径已变化"),
            429 to UsageProviderException.RateLimited(),
            500 to UsageProviderException.ProviderUnavailable(),
        )

        for ((statusCode, expected) in cases) {
            val provider = OpenCodeUsageProvider(transport(statusCode, """{"_tag":"Error"}"""), fixedClock)
            val result = provider.fetchUsage(credential(), CredentialSecret.BearerToken("oc_sk_test"))

            val error = requireNotNull(result.exceptionOrNull()) { "HTTP $statusCode must have an error" }
            assertTrue(result.isFailure, "HTTP $statusCode should fail")
            assertEquals(expected::class, error::class, "HTTP $statusCode mapping")
            assertEquals(expected.message, error?.message, "HTTP $statusCode message")
        }
    }

    @Test
    fun `request uses bearer authorization without credential in url`() = runTest {
        val request = transport(200, statusBody())
        val provider = OpenCodeUsageProvider(request, fixedClock)

        provider.fetchUsage(credential(), CredentialSecret.BearerToken("oc_sk_secret")).getOrThrow()

        val captured = request.captured
        assertEquals("https://opencode.ai/console/api/go/status", captured.url)
        assertEquals("Bearer oc_sk_secret", captured.headers["Authorization"])
        assertTrue(!captured.url.contains("oc_sk_secret"))
    }

    private fun transport(statusCode: Int, body: String) = object : HttpTransport {
        lateinit var captured: ProviderHttpRequest

        override suspend fun execute(request: ProviderHttpRequest): ProviderHttpResponse {
            captured = request
            return ProviderHttpResponse(statusCode, body.toByteArray())
        }
    }

    private fun assertDecimal(expected: String, actual: BigDecimal?) {
        assertEquals(0, BigDecimal(expected).compareTo(requireNotNull(actual)))
    }
}
