package ai.routin.mytoken.provider

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.CredentialMetadataKey
import ai.routin.mytoken.domain.model.CredentialSecret
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.usage.UsageProviderException
import ai.routin.mytoken.provider.http.HttpTransport
import ai.routin.mytoken.provider.http.ProviderHttpRequest
import ai.routin.mytoken.provider.http.ProviderHttpResponse
import ai.routin.mytoken.provider.xiaomi.XiaomiUsageProvider
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class XiaomiUsageProviderTest {
    @Test
    fun officialApiKeyReadsBalanceAndDailyUsage() = runTest {
        val requests = mutableListOf<ProviderHttpRequest>()
        val transport = HttpTransport { request ->
            requests += request
            val body = when {
                request.url.endsWith("/v1/platform/balance") ->
                    """{"object":"balance","is_available":true,"balance_infos":[{"currency":"CNY","cash_balance":"10.00","gift_balance":"2.50","total_available":"12.50"}]}"""
                request.url.contains("/v1/platform/usage/daily") ->
                    """{"object":"list","data":[{"date":"2026-11-12","resource_type":"model","name":"mimo-v2.6","currency":"CNY","consumed_amount":"1.250000","total_tokens":1000,"input_cache_hit_tokens":200,"input_cache_miss_tokens":600,"output_tokens":200},{"date":"2026-11-11","resource_type":"model","name":"mimo-v2.6","currency":"CNY","consumed_amount":"0.750000","total_tokens":500,"input_cache_hit_tokens":100,"input_cache_miss_tokens":300,"output_tokens":100}]}"""
                else -> error("unexpected request: ${request.url}")
            }
            ProviderHttpResponse(200, body.toByteArray())
        }
        val provider = XiaomiUsageProvider(
            transport = transport,
            clock = Clock.fixed(Instant.parse("2026-11-13T03:00:00Z"), ZoneOffset.UTC),
        )
        val snapshot = provider.fetchUsage(
            credential("api"),
            CredentialSecret.BearerToken("sk-official-key"),
        ).getOrThrow()

        assertEquals("API 按量", snapshot.planName)
        assertEquals(BigDecimal("12.50"), snapshot.metrics.first { it.id == "account-balance" }.value)
        assertEquals(BigDecimal("2.000000"), snapshot.metrics.first { it.id == "total-consumption" }.value)
        assertEquals("近 31 天消费", snapshot.metrics.first { it.id == "total-consumption" }.label)
        assertEquals(BigDecimal("1500"), snapshot.metrics.first { it.id == "total-tokens" }.value)
        assertEquals(BigDecimal("900"), snapshot.metrics.first { it.id == "input-tokens" }.value)
        assertEquals(BigDecimal("300"), snapshot.metrics.first { it.id == "cache-tokens" }.value)
        assertTrue(requests.all { it.headers["Authorization"] == "Bearer sk-official-key" })
        assertTrue(requests.all { it.headers["Cookie"] == null })
        val usageRequest = requests.first { it.url.contains("/platform/usage/daily") }
        assertTrue(usageRequest.url.contains("start_date=2026-10-14"))
        assertTrue(usageRequest.url.contains("end_date=2026-11-13"))
    }

    @Test
    fun officialTokenPlanKeyReadsCreditsAndExpiry() = runTest {
        val requests = mutableListOf<ProviderHttpRequest>()
        val transport = HttpTransport { request ->
            requests += request
            check(request.url.endsWith("/v1/platform/token_plan"))
            val body =
                """{"object":"token_plan","plan_code":"pro_month","period":{"start_at":"2026-11-01 00:00:00","end_at":"2026-12-01 00:00:00"},"quota":{"total_credits":20000,"used_credits":5000,"remaining_credits":15000},"auto_renew":true,"claw_enabled":false}"""
            ProviderHttpResponse(200, body.toByteArray())
        }
        val provider = XiaomiUsageProvider(
            transport = transport,
            clock = Clock.fixed(Instant.parse("2026-11-13T03:00:00Z"), ZoneOffset.UTC),
        )
        val snapshot = provider.fetchUsage(
            credential("plan"),
            CredentialSecret.BearerToken("tp-plan-key"),
        ).getOrThrow()
        val total = snapshot.metrics.first { it.id == "plan-total" }

        assertEquals("Pro 月度套餐", snapshot.planName)
        assertEquals("自动续费", snapshot.billingMode)
        assertEquals(1, snapshot.status)
        assertEquals(BigDecimal("5000"), total.used)
        assertEquals(BigDecimal("20000"), total.limit)
        assertEquals(BigDecimal("15000"), total.remaining)
        assertEquals(Instant.parse("2026-12-01T00:00:00Z"), snapshot.subscriptionEndAt)
        assertTrue(requests.all { it.headers["Authorization"] == "Bearer tp-plan-key" })
    }

    @Test
    fun officialKeyUsageKindMismatchReturnsClearMessage() = runTest {
        val provider = XiaomiUsageProvider(HttpTransport { error("不应发起请求") })
        val apiResult = provider.fetchUsage(
            credential("api"),
            CredentialSecret.BearerToken("tp-plan-key"),
        )
        val planResult = provider.fetchUsage(
            credential("plan"),
            CredentialSecret.BearerToken("sk-api-key"),
        )

        assertEquals(
            "小米 MiMo：API 按量请使用 sk- 开头的普通 API Key",
            (apiResult.exceptionOrNull() as UsageProviderException.ProviderMessage).message,
        )
        assertEquals(
            "小米 MiMo：Token Plan 请使用 tp- 开头的专用 API Key",
            (planResult.exceptionOrNull() as UsageProviderException.ProviderMessage).message,
        )
    }

    @Test
    fun legacyCookieIsRejected() = runTest {
        val provider = XiaomiUsageProvider(HttpTransport { error("不应发起请求") })
        val result = provider.fetchUsage(
            credential("api"),
            CredentialSecret.BearerToken("api-platform_serviceToken=legacy; userId=1"),
        )

        assertEquals(
            "小米 MiMo：请使用官方 API Key（API 按量 sk-，Token Plan tp-）",
            (result.exceptionOrNull() as UsageProviderException.ProviderMessage).message,
        )
    }

    private fun credential(usageKind: String) = Credential(
        id = UUID.randomUUID(),
        providerId = ProviderId.Xiaomi,
        credentialKind = CredentialKind.BearerApiKey,
        name = "小米 MiMo",
        metadata = mapOf(CredentialMetadataKey.UsageKind to usageKind),
    )
}
