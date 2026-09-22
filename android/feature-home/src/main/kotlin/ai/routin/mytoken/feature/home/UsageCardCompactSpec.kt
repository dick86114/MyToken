package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.CredentialMetadataKey
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageCardDensity
import ai.routin.mytoken.domain.model.UsageMetric

/**
 * 简洁卡片字段策略：与 macOS UsageCardDensityPolicy 保持同一套指标 ID 和
 * 短周期 → 长周期顺序；完整模式不走该策略。
 */
internal object UsageCardCompactSpec {

    fun orderedMetrics(
        providerId: ProviderId,
        metadata: Map<CredentialMetadataKey, String>,
        metrics: List<UsageMetric>,
    ): List<UsageMetric> {
        val usageKind = metadata[CredentialMetadataKey.UsageKind]
        val ids = when (providerId) {
            ProviderId.Routin -> if (usageKind == "tokenPack") {
                listOf("token")
            } else {
                listOf("fiveHour", "weekly")
            }
            ProviderId.DeepSeek -> listOf("balance")
            ProviderId.Xiaomi -> if (usageKind == "plan") {
                listOf("plan-total")
            } else {
                listOf("account-balance")
            }
            ProviderId.Glm -> listOf("five-hour", "weekly")
            ProviderId.Volcengine -> listOf("fiveHour", "weekly", "monthly")
            ProviderId.NewAPI -> listOf(
                "today-token",
                "one-day-token",
                "seven-day-token",
                "thirty-day-token",
            )
            ProviderId.CommandCode -> listOf("five-hour", "weekly", "credit-progress")
        }
        val byId = metrics.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }
}

/** 密度切换时给排序测试和 UI 共用的最小入口。 */
internal fun UsageCardDensity.isCompact(): Boolean = this == UsageCardDensity.COMPACT
