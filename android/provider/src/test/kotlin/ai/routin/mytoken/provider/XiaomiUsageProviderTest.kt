package ai.routin.mytoken.provider

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.CredentialMetadataKey
import ai.routin.mytoken.domain.model.CredentialSecret
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.provider.http.HttpTransport
import ai.routin.mytoken.provider.http.ProviderHttpRequest
import ai.routin.mytoken.provider.http.ProviderHttpResponse
import ai.routin.mytoken.provider.xiaomi.XiaomiUsageProvider
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class XiaomiUsageProviderTest {
    private val requests = mutableListOf<ProviderHttpRequest>()
    private val transport = HttpTransport { request ->
        requests += request
        val body = when {
            request.url.endsWith("/balance") ->
                """{"balance":12.5,"cashBalance":10,"giftBalance":2.5,"currency":"CNY"}"""
            request.url.endsWith("/tokenPlan/detail") ->
                """{"code":0,"data":{"planCode":"pro_month","planName":"Pro 月度套餐","expired":false,"enableAutoRenew":true,"currentPeriodStart":"2026-09-01T00:00:00Z","currentPeriodEnd":"2026-10-01T00:00:00Z"}}"""
            request.url.endsWith("/tokenPlan/usage") ->
                """{"code":0,"data":{"usage":{"items":[{"name":"five_hour_token","used":200,"limit":1000,"percent":20},{"name":"total_token","used":5000,"limit":20000,"percent":25},{"name":"compensation_total_token","used":300,"limit":300,"percent":100}]}}}"""
            request.url.endsWith("/usage") ->
                """{"costUsage":{"totalCost":3.25},"tokenUsage":{"inputToken":1000,"outputToken":250,"cacheToken":400}}"""
            else -> error("unexpected request: ${request.url}")
        }
        ProviderHttpResponse(200, body.toByteArray())
    }

    @Test
    fun apiModeReadsBalanceAndUsage() = runTest {
        val provider = XiaomiUsageProvider(transport)
        val snapshot = provider.fetchUsage(
            credential("api"),
            CredentialSecret.BearerToken("session-token"),
        ).getOrThrow()

        assertEquals("API 按量", snapshot.planName)
        assertEquals("api", snapshot.usageKind)
        val balance = snapshot.metrics.first { it.id == "account-balance" }
        assertEquals(BigDecimal("12.5"), balance.value)
        assertEquals("CNY", balance.currencyCode)
        assertEquals(UsageMetricPresentation.Balance, balance.presentation)
        assertEquals(UsageMetricHealthState.Normal, balance.healthState)
        assertEquals(BigDecimal("1250"), snapshot.metrics.first { it.id == "total-tokens" }.value)
        assertEquals("历史消耗", snapshot.metrics.first { it.id == "total-tokens" }.label)
        assertEquals(BigDecimal("600"), snapshot.metrics.first { it.id == "input-tokens" }.value)
        assertEquals("未命中缓存", snapshot.metrics.first { it.id == "input-tokens" }.label)
        assertEquals("命中缓存", snapshot.metrics.first { it.id == "cache-tokens" }.label)
        assertTrue(requests.all { it.headers["Cookie"] == "api-platform_serviceToken=session-token" })
    }

    @Test
    fun planModeReadsSubscriptionAndCreditWindows() = runTest {
        val provider = XiaomiUsageProvider(transport)
        val snapshot = provider.fetchUsage(
            credential("plan"),
            CredentialSecret.BearerToken("Cookie: api-platform_serviceToken=abc; api-platform_ph=def"),
        ).getOrThrow()

        assertEquals("Pro 月度套餐", snapshot.planName)
        assertEquals("plan", snapshot.usageKind)
        assertEquals("有效", snapshot.statusText)
        assertEquals("自动续费", snapshot.billingMode)
        assertEquals(BigDecimal("800"), snapshot.metrics.first { it.id == "plan-five-hour" }.remaining)
        assertEquals(BigDecimal("20000"), snapshot.metrics.first { it.id == "plan-total" }.limit)
        assertEquals(BigDecimal("300"), snapshot.metrics.first { it.id == "plan-compensation" }.value)
        assertTrue(
            requests.all {
                it.headers["Cookie"] == "api-platform_serviceToken=abc; api-platform_ph=def"
            },
        )
    }

    @Test
    fun cookieWithNewlineIsRejected() = runTest {
        val provider = XiaomiUsageProvider(transport)
        val result = provider.fetchUsage(
            credential("api"),
            CredentialSecret.BearerToken("cookie=a\nInjected: true"),
        )
        assertTrue(result.isFailure)
    }

    private fun credential(usageKind: String) = Credential(
        id = UUID.randomUUID(),
        providerId = ProviderId.Xiaomi,
        credentialKind = CredentialKind.BearerApiKey,
        name = "小米 MiMo",
        metadata = mapOf(CredentialMetadataKey.UsageKind to usageKind),
    )
}
