package ai.routin.mytoken.provider.opencode

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.CredentialSecret
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import ai.routin.mytoken.domain.model.UsageSnapshot
import ai.routin.mytoken.domain.usage.UsageProvider
import ai.routin.mytoken.domain.usage.UsageProviderException
import ai.routin.mytoken.provider.http.HttpTransport
import ai.routin.mytoken.provider.http.ProviderHttpRequest
import ai.routin.mytoken.provider.http.ProviderHttpResponse
import ai.routin.mytoken.provider.json.ProviderJson
import ai.routin.mytoken.provider.json.asObjectOrNull
import ai.routin.mytoken.provider.json.boolOrNull
import ai.routin.mytoken.provider.json.decimalOrNull
import ai.routin.mytoken.provider.json.objOrNull
import ai.routin.mytoken.provider.json.stringOrNull
import java.math.BigDecimal
import java.math.MathContext
import java.net.URI
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/** OpenCode Console Go / Go Plus 订阅状态适配器。 */
class OpenCodeUsageProvider(
    private val transport: HttpTransport,
    private val clock: Clock = Clock.systemUTC(),
    private val baseURL: URI = URI.create("https://opencode.ai/console"),
) : UsageProvider {

    override val providerId: ProviderId = ProviderId.OpenCode

    override suspend fun fetchUsage(
        credential: Credential,
        secret: CredentialSecret,
    ): Result<UsageSnapshot> {
        if (credential.providerId != ProviderId.OpenCode ||
            credential.credentialKind != CredentialKind.BearerApiKey
        ) {
            return Result.failure(UsageProviderException.InvalidCredential())
        }
        val token = (secret as? CredentialSecret.BearerToken)?.token.orEmpty().trim()
        if (token.isEmpty()) return Result.failure(UsageProviderException.InvalidCredential())

        return runCatching { fetchSnapshot(credential.id, token) }.recoverCatching { error ->
            if (error is UsageProviderException) throw error
            throw UsageProviderException.InvalidResponse()
        }
    }

    private suspend fun fetchSnapshot(
        credentialId: UUID,
        token: String,
    ): UsageSnapshot {
        val status = requestStatus(token)
        val access = status.objOrNull("access")
            ?: throw noSubscription()
        val meters = access.objOrNull("meters")
            ?: throw noSubscription()
        val product = status.stringOrNull("product").orEmpty()
        if (product !in setOf("go", "go-plus")) throw noSubscription()
        val fiveHour = meters.objOrNull("fiveHour")
            ?: throw noSubscription()
        val weekly = meters.objOrNull("week")
            ?: throw noSubscription()
        val monthly = meters.objOrNull("month")
            ?: throw noSubscription()

        return UsageSnapshot(
            credentialId = credentialId,
            fetchedAt = clock.instant(),
            planName = if (product == "go-plus") "OpenCode Go Plus" else "OpenCode Go",
            subscriptionStartAt = access.stringOrNull("startsAt")?.let(Instant::parse),
            subscriptionEndAt = access.stringOrNull("endsAt")?.let(Instant::parse),
            statusText = renewalStatusText(status),
            billingMode = if (status.boolOrNull("cancelAtPeriodEnd") == true) "取消续订" else "自动续费",
            usageKind = "periodic",
            metrics = listOf(
                metric("fiveHour", "5 小时", fiveHour),
                metric("weekly", "周", weekly),
                metric("monthly", "月", monthly),
            ),
        )
    }

    private suspend fun requestStatus(token: String): JsonObject {
        val response = try {
            transport.execute(
                ProviderHttpRequest(
                    method = "GET",
                    url = baseURL.toString().trimEnd('/') + "/api/go/status",
                    headers = mapOf(
                        "Authorization" to "Bearer $token",
                        "Accept" to "application/json",
                    ),
                ),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            throw UsageProviderException.Transport()
        }

        if (response.statusCode !in 200..299) throw mapHTTPStatus(response.statusCode)
        val root = ProviderJson.parse(response.body.decodeToString())
        if (root is JsonNull) throw noSubscription()
        return root.asObjectOrNull() ?: throw UsageProviderException.InvalidResponse()
    }

    private fun renewalStatusText(status: JsonObject): String = when {
        status.boolOrNull("renewalPending") == true -> "续费处理中"
        status.boolOrNull("renewalAuthorizationRequired") == true -> "需要重新授权支付"
        status.boolOrNull("cancelAtPeriodEnd") == true -> "将在当前周期结束后到期"
        else -> "正常续费"
    }

    private fun metric(
        id: String,
        label: String,
        data: JsonObject,
    ): UsageMetric {
        val used = microCentsToDollars(data, "usedMicroCents")
        val limit = microCentsToDollars(data, "limitMicroCents")
        return UsageMetric(
            id = id,
            label = label,
            used = used,
            limit = limit,
            remaining = (limit - used).max(BigDecimal.ZERO),
            unit = UsageMetricUnit.Currency,
            windowStart = data.stringOrNull("startsAt")?.let(Instant::parse),
            windowEnd = data.stringOrNull("resetsAt")?.let(Instant::parse),
            presentation = UsageMetricPresentation.Progress,
            semantic = UsageMetricSemantic.UsedQuota,
            currencyCode = "$",
            healthState = healthState(used, limit),
        )
    }

    private fun microCentsToDollars(
        data: JsonObject,
        key: String,
    ): BigDecimal {
        val value = requireNotNull(data.decimalOrNull(key)) { "missing $key" }
        return value.max(BigDecimal.ZERO).divide(MICRO_CENTS_PER_DOLLAR, MathContext.DECIMAL128)
    }

    private fun healthState(
        used: BigDecimal,
        limit: BigDecimal,
    ): UsageMetricHealthState {
        if (limit <= BigDecimal.ZERO) return UsageMetricHealthState.Unknown
        if (used >= limit) return UsageMetricHealthState.Critical
        if (used.divide(limit, MathContext.DECIMAL128) >= BigDecimal("0.8")) {
            return UsageMetricHealthState.Warning
        }
        return UsageMetricHealthState.Normal
    }

    private fun mapHTTPStatus(statusCode: Int): UsageProviderException = when (statusCode) {
        400 -> UsageProviderException.InvalidResponse()
        401 -> UsageProviderException.Unauthorized()
        403 -> UsageProviderException.ProviderMessage(
            "OpenCode：API Key 没有 Console 状态权限，请创建带读取权限的服务账号 Key",
        )
        404 -> UsageProviderException.ProviderMessage("OpenCode：未找到订阅或接口路径已变化")
        429 -> UsageProviderException.RateLimited()
        in 500..599 -> UsageProviderException.ProviderUnavailable()
        else -> UsageProviderException.InvalidResponse()
    }

    private fun noSubscription(): UsageProviderException =
        UsageProviderException.ProviderMessage(NO_SUBSCRIPTION_MESSAGE)

    private companion object {
        val MICRO_CENTS_PER_DOLLAR = BigDecimal("100000000")
        const val NO_SUBSCRIPTION_MESSAGE = "OpenCode：未找到有效 Go 订阅"
    }
}
