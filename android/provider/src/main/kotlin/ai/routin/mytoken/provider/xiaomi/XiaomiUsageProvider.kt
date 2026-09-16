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
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonObject

/**
 * 小米 MiMo 开放平台用量适配器。
 *
 * 平台没有公开用量查询 API Key，控制台使用同源 Cookie 登录态：
 * - API 按量模式读取 `/balance` 和 `/usage`。
 * - Token Plan 模式读取 `/tokenPlan/detail` 和 `/tokenPlan/usage`。
 */
class XiaomiUsageProvider(
    private val transport: HttpTransport,
    private val clock: Clock = Clock.systemUTC(),
    private val baseURL: URI = URI.create("https://platform.xiaomimimo.com/api/v1"),
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
        val rawCookie = (secret as? CredentialSecret.BearerToken)?.token?.trim().orEmpty()
        val cookie = normalizeCookie(rawCookie)
            ?: return Result.failure(UsageProviderException.InvalidCredential())
        val usageKind = credential.metadata[CredentialMetadataKey.UsageKind] ?: "api"

        return runCatching {
            when (usageKind) {
                "api" -> fetchAPIUsage(credential, cookie)
                "plan" -> fetchPlanUsage(credential, cookie)
                else -> throw UsageProviderException.InvalidCredential()
            }
        }.recoverCatching { error ->
            if (error is UsageProviderException || error is kotlinx.coroutines.CancellationException) {
                throw error
            }
            throw UsageProviderException.InvalidResponse()
        }
    }

    private suspend fun fetchAPIUsage(credential: Credential, cookie: String): UsageSnapshot = coroutineScope {
        val balanceRequest = async { requestObject("balance", cookie) }
        val usageRequest = async { requestObject("usage", cookie) }
        val balance = balanceRequest.await()
        val usage = usageRequest.await()

        val currency = balance.stringOrNull("currency")
            ?.trim()
            ?.uppercase()
            ?.takeIf { it.isNotEmpty() }
            ?: "CNY"
        val availableBalance = balance.decimalOrNull("balance") ?: BigDecimal.ZERO
        val health = UsageMetricHealthEvaluator.balanceState(
            balance = availableBalance,
            warningThreshold = null,
            isAvailable = true,
        )
        val tokenUsage = usage.objOrNull("tokenUsage")
        val costUsage = usage.objOrNull("costUsage")
        val inputTokens = tokenUsage?.decimalOrNull("inputToken") ?: BigDecimal.ZERO
        val outputTokens = tokenUsage?.decimalOrNull("outputToken") ?: BigDecimal.ZERO
        val cacheTokens = tokenUsage?.decimalOrNull("cacheToken") ?: BigDecimal.ZERO
        val inputMissTokens = (inputTokens - cacheTokens).max(BigDecimal.ZERO)
        val totalTokens = inputTokens + outputTokens

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
                valueMetric("cash-balance", "现金余额", balance.decimalOrNull("cashBalance") ?: BigDecimal.ZERO, UsageMetricUnit.Currency, currency),
                valueMetric("gift-balance", "赠送余额", balance.decimalOrNull("giftBalance") ?: BigDecimal.ZERO, UsageMetricUnit.Currency, currency),
                valueMetric("total-consumption", "累计消费", costUsage?.decimalOrNull("totalCost") ?: BigDecimal.ZERO, UsageMetricUnit.Currency, currency),
                valueMetric("total-tokens", "历史消耗", totalTokens, UsageMetricUnit.Token),
                valueMetric("input-tokens", "未命中缓存", inputMissTokens, UsageMetricUnit.Token),
                valueMetric("output-tokens", "输出", outputTokens, UsageMetricUnit.Token),
                valueMetric("cache-tokens", "命中缓存", cacheTokens, UsageMetricUnit.Token),
            ),
        )
    }

    private suspend fun fetchPlanUsage(credential: Credential, cookie: String): UsageSnapshot = coroutineScope {
        val detailRequest = async { requestObject("tokenPlan/detail", cookie) }
        val usageRequest = async { requestObject("tokenPlan/usage", cookie) }
        val detail = detailRequest.await()
        val usage = usageRequest.await()
        val usageObject = usage.objOrNull("usage") ?: usage
        val items = usageObject.arrayOrNull("items").orEmpty()
        val periodEnd = parseInstant(detail.stringOrNull("currentPeriodEnd"))

        val metrics = items.mapNotNull { element ->
            val item = element.asObjectOrNull() ?: return@mapNotNull null
            val name = item.stringOrNull("name").orEmpty()
            if (name.isBlank()) return@mapNotNull null
            val used = item.decimalOrNull("used") ?: BigDecimal.ZERO
            val limit = item.decimalOrNull("limit") ?: BigDecimal.ZERO
            val isCompensation = name.lowercase().contains("compensation")
            if (limit.signum() <= 0 && !isCompensation) return@mapNotNull null
            val remaining = (limit - used).max(BigDecimal.ZERO)
            val percent = item.decimalOrNull("percent")?.toDouble()
                ?: percent(used, limit)
            val health = percentHealth(percent)
            if (isCompensation) {
                UsageMetric(
                    id = "plan-compensation",
                    label = "补偿 Credits",
                    value = used,
                    unit = UsageMetricUnit.Token,
                    presentation = UsageMetricPresentation.Value,
                    semantic = UsageMetricSemantic.Value,
                    healthState = health,
                )
            } else {
                UsageMetric(
                    id = metricID(name),
                    label = metricLabel(name),
                    used = used,
                    limit = limit,
                    remaining = remaining,
                    unit = UsageMetricUnit.Token,
                    windowEnd = parseInstant(item.stringOrNull("resetTime"))
                        ?: parseInstant(item.stringOrNull("expireTime"))
                        ?: periodEnd,
                    presentation = UsageMetricPresentation.Progress,
                    semantic = UsageMetricSemantic.UsedQuota,
                    healthState = health,
                )
            }
        }
        if (metrics.isEmpty()) throw UsageProviderException.InvalidResponse()

        val expired = detail.boolOrNull("expired") ?: false
        val statusText = if (expired) "已失效" else "有效"
        val enableAutoRenew = detail.boolOrNull("enableAutoRenew")
            ?: detail.boolOrNull("hasAutoRenewSubscribed")
        val billingMode = enableAutoRenew?.let { if (it) "自动续费" else "手动续费" }

        UsageSnapshot(
            credentialId = credential.id,
            fetchedAt = clock.instant(),
            planName = detail.stringOrNull("planName")
                ?.takeIf { it.isNotBlank() }
                ?: planName(detail.stringOrNull("planCode")),
            subscriptionStartAt = parseInstant(detail.stringOrNull("currentPeriodStart")),
            subscriptionEndAt = periodEnd,
            status = if (expired) 0 else 1,
            statusText = statusText,
            billingMode = billingMode,
            usageKind = "plan",
            allowedModels = DEFAULT_MODELS,
            metrics = metrics + UsageMetric(
                id = "plan-status",
                label = "订阅状态",
                value = if (expired) BigDecimal.ZERO else BigDecimal.ONE,
                unit = UsageMetricUnit.Boolean_,
                presentation = UsageMetricPresentation.Status,
                semantic = UsageMetricSemantic.Status,
                healthState = if (expired) UsageMetricHealthState.Unavailable else UsageMetricHealthState.Normal,
            ),
        )
    }

    private suspend fun requestObject(path: String, cookie: String): JsonObject {
        val url = baseURL.toString().trimEnd('/') + "/" + path.trimStart('/')
        val response = try {
            transport.execute(
                ProviderHttpRequest(
                    method = "GET",
                    url = url,
                    headers = mapOf(
                        "Cookie" to cookie,
                        "Accept" to "application/json",
                        "Accept-Language" to "zh-CN,zh;q=0.9,en;q=0.8",
                        "x-timeZone" to "Asia/Shanghai",
                        "Referer" to "https://platform.xiaomimimo.com/",
                    ),
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
        val root = ProviderJson.parse(body).asObjectOrNull()
            ?: throw UsageProviderException.InvalidResponse()
        return root.objOrNull("data") ?: root
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

    private fun normalizeCookie(rawValue: String): String? {
        var value = rawValue.trim()
        if (value.isEmpty() || value.contains('\r') || value.contains('\n')) return null
        if (value.startsWith("Cookie:", ignoreCase = true)) {
            value = value.substringAfter(':').trim()
        }
        if (value.isEmpty()) return null
        return if (value.contains('=')) value else "api-platform_serviceToken=$value"
    }

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

    private fun metricID(name: String): String {
        val normalized = name.lowercase()
        return when {
            normalized.contains("five") || normalized.contains("5h") -> "plan-five-hour"
            normalized.contains("total") -> "plan-total"
            else -> "plan-" + normalized.replace('_', '-').filter { it.isLetterOrDigit() || it == '-' }
        }.ifBlank { "plan-usage" }
    }

    private fun metricLabel(name: String): String {
        val normalized = name.lowercase()
        return when {
            normalized.contains("five") || normalized.contains("5h") -> "每 5 小时"
            normalized.contains("total") -> "套餐总量"
            else -> name.replace('_', ' ').replace("token", "Credits", ignoreCase = true)
        }
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
        return value.replace("_year", " 年度套餐").replace('_', ' ')
    }

    private companion object {
        val MATH_CONTEXT = java.math.MathContext(8)
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
