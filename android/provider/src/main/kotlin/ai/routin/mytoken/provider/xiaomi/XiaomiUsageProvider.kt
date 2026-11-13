package ai.routin.mytoken.provider.xiaomi

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.CredentialMetadataKey
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
import ai.routin.mytoken.provider.health.UsageMetricHealthEvaluator
import ai.routin.mytoken.provider.http.HttpTransport
import ai.routin.mytoken.provider.http.ProviderHttpRequest
import ai.routin.mytoken.provider.http.ProviderHttpResponse
import ai.routin.mytoken.provider.json.ProviderJson
import ai.routin.mytoken.provider.json.arrayOrNull
import ai.routin.mytoken.provider.json.asObjectOrNull
import ai.routin.mytoken.provider.json.boolOrNull
import ai.routin.mytoken.provider.json.decimalOrNull
import ai.routin.mytoken.provider.json.objOrNull
import ai.routin.mytoken.provider.json.stringOrNull
import java.math.BigDecimal
import java.net.URI
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonObject

/**
 * 小米 MiMo 官方财务接口适配器。
 *
 * - `sk-` 普通 API Key：账户余额 + 最近 31 天日用量。
 * - `tp-` 专用 API Key：Token Plan Credits、周期和过期时间。
 * 不支持网页 Cookie 或任何网页登录态续期。
 */
class XiaomiUsageProvider(
    private val transport: HttpTransport,
    private val clock: Clock = Clock.systemUTC(),
    private val officialBaseURL: URI = URI.create("https://api.xiaomimimo.com/v1"),
    private val tokenPlanBaseURL: URI = URI.create("https://token-plan-cn.xiaomimimo.com/v1"),
) : UsageProvider {

    override val providerId: ProviderId = ProviderId.Xiaomi

    override suspend fun fetchUsage(
        credential: Credential,
        secret: CredentialSecret,
    ): Result<UsageSnapshot> {
        if (credential.providerId != ProviderId.Xiaomi ||
            credential.credentialKind != CredentialKind.BearerApiKey
        ) {
            return Result.failure(UsageProviderException.InvalidCredential())
        }
        val apiKey = (secret as? CredentialSecret.BearerToken)?.token?.trim().orEmpty()
        if (apiKey.isEmpty()) return Result.failure(UsageProviderException.InvalidCredential())
        val usageKind = credential.metadata[CredentialMetadataKey.UsageKind] ?: "api"

        return runCatching {
            when {
                apiKey.startsWith("sk-", ignoreCase = true) && usageKind == "api" ->
                    fetchAPIUsage(credential, apiKey)
                apiKey.startsWith("tp-", ignoreCase = true) && usageKind == "plan" ->
                    fetchPlanUsage(credential, apiKey)
                apiKey.startsWith("sk-", ignoreCase = true) ->
                    throw UsageProviderException.ProviderMessage(
                        "小米 MiMo：Token Plan 请使用 tp- 开头的专用 API Key",
                    )
                apiKey.startsWith("tp-", ignoreCase = true) ->
                    throw UsageProviderException.ProviderMessage(
                        "小米 MiMo：API 按量请使用 sk- 开头的普通 API Key",
                    )
                else -> throw UsageProviderException.ProviderMessage(
                    "小米 MiMo：请使用官方 API Key（API 按量 sk-，Token Plan tp-）",
                )
            }
        }.recoverCatching { error ->
            if (error is UsageProviderException || error is kotlinx.coroutines.CancellationException) {
                throw error
            }
            throw UsageProviderException.InvalidResponse()
        }
    }

    private suspend fun fetchAPIUsage(
        credential: Credential,
        apiKey: String,
    ): UsageSnapshot = coroutineScope {
        val balanceURL = officialBaseURL.toString().trimEnd('/') + "/platform/balance"
        val usageURL = officialUsageURL()
        val balanceRequest = async { requestRoot(balanceURL, officialHeaders(apiKey)) }
        val usageRequest = async { requestRoot(usageURL, officialHeaders(apiKey)) }
        val balanceRoot = balanceRequest.await()
        val usageRoot = usageRequest.await()

        val balance = balanceRoot.objOrNull("data") ?: balanceRoot
        val balanceInfo = balance.arrayOrNull("balance_infos")
            ?.firstOrNull()
            ?.asObjectOrNull()
            ?: throw UsageProviderException.InvalidResponse()
        val currency = balanceInfo.stringOrNull("currency")
            ?.trim()
            ?.uppercase()
            ?.takeIf { it.isNotEmpty() }
            ?: "CNY"
        val cashBalance = balanceInfo.decimalOrNull("cash_balance") ?: BigDecimal.ZERO
        val giftBalance = balanceInfo.decimalOrNull("gift_balance") ?: BigDecimal.ZERO
        val availableBalance = balanceInfo.decimalOrNull("total_available")
            ?: cashBalance + giftBalance + (balanceInfo.decimalOrNull("credit_available") ?: BigDecimal.ZERO)
        val isAvailable = balance.boolOrNull("is_available") ?: (availableBalance.signum() > 0)
        val health = UsageMetricHealthEvaluator.balanceState(
            balance = availableBalance,
            warningThreshold = null,
            isAvailable = isAvailable,
        )

        var totalCost = BigDecimal.ZERO
        var totalTokens = BigDecimal.ZERO
        var inputMissTokens = BigDecimal.ZERO
        var outputTokens = BigDecimal.ZERO
        var cacheTokens = BigDecimal.ZERO
        for (element in usageRoot.arrayOrNull("data").orEmpty()) {
            val record = element.asObjectOrNull() ?: continue
            totalCost += record.decimalOrNull("consumed_amount") ?: BigDecimal.ZERO
            val recordCache = record.decimalOrNull("input_cache_hit_tokens") ?: BigDecimal.ZERO
            val recordMiss = record.decimalOrNull("input_cache_miss_tokens") ?: BigDecimal.ZERO
            val recordOutput = record.decimalOrNull("output_tokens") ?: BigDecimal.ZERO
            cacheTokens += recordCache
            inputMissTokens += recordMiss
            outputTokens += recordOutput
            val components = recordCache + recordMiss + recordOutput
            totalTokens += (record.decimalOrNull("total_tokens") ?: BigDecimal.ZERO).max(components)
        }

        UsageSnapshot(
            credentialId = credential.id,
            fetchedAt = clock.instant(),
            planName = "API 按量",
            usageKind = "api",
            statusText = "可用",
            billingMode = "按量计费",
            allowedModels = DEFAULT_MODELS,
            metrics = listOf(
                balanceMetric("account-balance", "账户余额", availableBalance, currency, health),
                valueMetric("cash-balance", "现金余额", cashBalance, UsageMetricUnit.Currency, currency),
                valueMetric("gift-balance", "赠送余额", giftBalance, UsageMetricUnit.Currency, currency),
                valueMetric("total-consumption", "近 31 天消费", totalCost, UsageMetricUnit.Currency, currency),
                valueMetric("total-tokens", "近 31 天消耗", totalTokens, UsageMetricUnit.Token),
                valueMetric("input-tokens", "未命中缓存", inputMissTokens, UsageMetricUnit.Token),
                valueMetric("output-tokens", "输出", outputTokens, UsageMetricUnit.Token),
                valueMetric("cache-tokens", "命中缓存", cacheTokens, UsageMetricUnit.Token),
            ),
        )
    }

    private suspend fun fetchPlanUsage(
        credential: Credential,
        apiKey: String,
    ): UsageSnapshot = coroutineScope {
        val url = tokenPlanBaseURL.toString().trimEnd('/') + "/platform/token_plan"
        val root = requestRoot(url, officialHeaders(apiKey))
        val data = root.objOrNull("data") ?: root
        val quota = data.objOrNull("quota")
            ?: throw UsageProviderException.InvalidResponse()
        val totalCredits = quota.decimalOrNull("total_credits") ?: BigDecimal.ZERO
        val usedCredits = quota.decimalOrNull("used_credits") ?: BigDecimal.ZERO
        val remainingCredits = quota.decimalOrNull("remaining_credits")
            ?: (totalCredits - usedCredits).max(BigDecimal.ZERO)
        val period = data.objOrNull("period")
        val periodStart = parseInstant(period?.stringOrNull("start_at"))
        val periodEnd = parseInstant(period?.stringOrNull("end_at"))
        val expired = periodEnd?.let { !it.isAfter(clock.instant()) } ?: false
        val planMetric = UsageMetric(
            id = "plan-total",
            label = "套餐总量",
            used = usedCredits,
            limit = totalCredits,
            remaining = remainingCredits,
            unit = UsageMetricUnit.Token,
            windowStart = periodStart,
            windowEnd = periodEnd,
            presentation = UsageMetricPresentation.Progress,
            semantic = UsageMetricSemantic.UsedQuota,
            healthState = percentHealth(percent(usedCredits, totalCredits)),
        )
        val statusMetric = UsageMetric(
            id = "plan-status",
            label = "订阅状态",
            value = if (expired) BigDecimal.ZERO else BigDecimal.ONE,
            unit = UsageMetricUnit.Boolean_,
            presentation = UsageMetricPresentation.Status,
            semantic = UsageMetricSemantic.Status,
            healthState = if (expired) UsageMetricHealthState.Unavailable else UsageMetricHealthState.Normal,
        )
        val autoRenew = data.boolOrNull("auto_renew")
        val billingMode = autoRenew?.let { if (it) "自动续费" else "手动续费" }

        UsageSnapshot(
            credentialId = credential.id,
            fetchedAt = clock.instant(),
            planName = planName(data.stringOrNull("plan_code")),
            subscriptionStartAt = periodStart,
            subscriptionEndAt = periodEnd,
            status = if (expired) 0 else 1,
            statusText = if (expired) "已失效" else "有效",
            billingMode = billingMode,
            usageKind = "plan",
            allowedModels = DEFAULT_MODELS,
            metrics = listOf(planMetric, statusMetric),
        )
    }

    private fun officialHeaders(apiKey: String): Map<String, String> = mapOf(
        "Authorization" to "Bearer $apiKey",
        "Accept" to "application/json",
        "Accept-Language" to "zh-CN,zh;q=0.9,en;q=0.8",
    )

    private fun officialUsageURL(): String {
        val endDate = clock.instant().atZone(ZoneOffset.UTC).toLocalDate()
        val startDate = endDate.minusDays(30).coerceAtLeast(LocalDate.parse(OFFICIAL_USAGE_EARLIEST_DATE))
        return buildString {
            append(officialBaseURL.toString().trimEnd('/'))
            append("/platform/usage/daily?start_date=")
            append(startDate)
            append("&end_date=")
            append(endDate)
        }
    }

    private suspend fun requestRoot(
        url: String,
        headers: Map<String, String>,
    ): JsonObject {
        val response = try {
            transport.execute(
                ProviderHttpRequest(
                    method = "GET",
                    url = url,
                    headers = headers,
                ),
            )
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            throw error as? UsageProviderException ?: UsageProviderException.Transport()
        }
        val body = response.body.decodeToString()
        if (response.statusCode !in 200..299) {
            errorMessage(body)?.let {
                throw UsageProviderException.ProviderMessage("小米 MiMo：$it")
            }
            throw when (response.statusCode) {
                401, 403 -> UsageProviderException.Unauthorized()
                429 -> UsageProviderException.RateLimited()
                in 500..599 -> UsageProviderException.ProviderUnavailable()
                else -> UsageProviderException.InvalidResponse()
            }
        }
        return ProviderJson.parse(body).asObjectOrNull()
            ?: throw UsageProviderException.InvalidResponse()
    }

    private fun errorMessage(body: String): String? = runCatching {
        val root = ProviderJson.parse(body).asObjectOrNull()
        root?.stringOrNull("message")
            ?: root?.objOrNull("error")?.stringOrNull("message")
    }.getOrNull()?.takeIf { it.isNotBlank() }

    private fun balanceMetric(
        id: String,
        label: String,
        value: BigDecimal,
        currency: String,
        health: UsageMetricHealthState,
    ) = UsageMetric(
        id = id,
        label = label,
        value = value,
        unit = UsageMetricUnit.Currency,
        presentation = UsageMetricPresentation.Balance,
        semantic = UsageMetricSemantic.Balance,
        currencyCode = currency,
        healthState = health,
    )

    private fun valueMetric(
        id: String,
        label: String,
        value: BigDecimal,
        unit: UsageMetricUnit,
        currency: String? = null,
    ) = UsageMetric(
        id = id,
        label = label,
        value = value,
        unit = unit,
        presentation = UsageMetricPresentation.Value,
        semantic = UsageMetricSemantic.Value,
        currencyCode = currency,
        healthState = UsageMetricHealthState.Normal,
    )

    private fun parseInstant(rawValue: String?): Instant? {
        val value = rawValue?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        value.toDoubleOrNull()?.let {
            val seconds = if (it > 10_000_000_000) it / 1_000 else it
            return Instant.ofEpochMilli((seconds * 1_000).toLong())
        }
        return runCatching { Instant.parse(value) }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(value.replace(' ', 'T')).toInstant(ZoneOffset.UTC)
            }.getOrNull()
    }

    private fun percent(used: BigDecimal, limit: BigDecimal): Double =
        if (limit.signum() <= 0) 0.0 else used.divide(limit, MATH_CONTEXT).toDouble() * 100

    private fun percentHealth(percent: Double): UsageMetricHealthState = when {
        percent < 70 -> UsageMetricHealthState.Normal
        percent < 90 -> UsageMetricHealthState.Warning
        else -> UsageMetricHealthState.Critical
    }

    private fun planName(planCode: String?): String {
        val value = planCode?.takeIf { it.isNotBlank() } ?: return "Token Plan"
        val normalized = value.lowercase()
        val suffix = listOf(
            "_year" to " 年度套餐",
            "_month" to " 月度套餐",
            "_week" to " 周套餐",
        ).firstOrNull { normalized.endsWith(it.first) }
        if (suffix == null) return value.replace('_', ' ')
        val base = normalized
            .dropLast(suffix.first.length)
            .replace('_', ' ')
            .split(' ')
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercaseChar() } }
        return base + suffix.second
    }

    private companion object {
        val MATH_CONTEXT = java.math.MathContext(8)
        const val OFFICIAL_USAGE_EARLIEST_DATE = "2026-09-01"
        val DEFAULT_MODELS = listOf(
            "mimo-v2.5-pro",
            "mimo-v2.5",
            "mimo-v2.5-asr",
            "mimo-v2.5-tts",
            "mimo-v2.5-tts-voicedesign",
            "mimo-v2.5-tts-voiceclone",
        )
    }
}
