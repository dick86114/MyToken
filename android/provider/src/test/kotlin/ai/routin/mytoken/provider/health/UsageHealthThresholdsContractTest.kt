package ai.routin.mytoken.provider.health

import ai.routin.mytoken.domain.model.UsageHealthThresholds
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** 共享能力契约是阈值的唯一来源；Android 常量必须与 shared JSON 保持一致。 */
class UsageHealthThresholdsContractTest {

    private val contractDir = requireNotNull(System.getProperty("shared.contracts.dir")) {
        "shared.contracts.dir system property is required"
    }

    private val thresholds = Json
        .parseToJsonElement(File(contractDir, "provider-capabilities.json").readText())
        .jsonObject
        .getValue("healthThresholds")
        .jsonObject

    @Test
    fun `android thresholds match shared provider contract`() {
        assertEquals(thresholds.getValue("warningUsedPercent").jsonPrimitive.int, UsageHealthThresholds.WARNING_USED_PERCENT)
        assertEquals(thresholds.getValue("criticalUsedPercent").jsonPrimitive.int, UsageHealthThresholds.CRITICAL_USED_PERCENT)
    }

    @Test
    fun `used percent boundaries map to health states`() {
        assertEquals(UsageMetricHealthState.Normal, UsageHealthThresholds.fromUsedPercent(79.9))
        assertEquals(UsageMetricHealthState.Warning, UsageHealthThresholds.fromUsedPercent(80.0))
        assertEquals(UsageMetricHealthState.Warning, UsageHealthThresholds.fromUsedPercent(94.9))
        assertEquals(UsageMetricHealthState.Critical, UsageHealthThresholds.fromUsedPercent(95.0))
        assertEquals(UsageMetricHealthState.Unknown, UsageHealthThresholds.fromUsedPercent(null))
    }
}
