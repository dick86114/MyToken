package ai.routin.mytoken.widget

import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetMetricRankerTest {
    private val fixture = Json.parseToJsonElement(
        File(
            requireNotNull(System.getProperty("widget.contracts.dir")),
            "widget-cases.json",
        ).readText(),
    ).jsonObject

    @Test
    fun `ranker matches shared provider contracts`() {
        for (case in fixture.getValue("cases").jsonArray.map { it.jsonObject }) {
            val credential = case.getValue("credential").jsonObject
            val metrics = credential.getValue("metrics").jsonArray.map { metric(it.jsonObject) }
            val selection = WidgetMetricRanker.rank(
                providerId = ProviderId.fromRawValue(case.getValue("providerID").jsonPrimitive.content),
                metrics = metrics,
                fixedMetricId = case["fixedMetricID"]?.jsonPrimitive?.contentOrNull,
                capacity = case.getValue("capacity").jsonPrimitive.int,
            )

            assertEquals(
                case.getValue("expectedMetricIDs").jsonArray.map { it.jsonPrimitive.content },
                selection.orderedMetrics.map { it.id },
            )
            assertEquals(
                case.getValue("expectedPrimaryMetricID").jsonPrimitive.content,
                selection.primary?.id,
            )
            assertEquals(
                case.getValue("expectedFixedMetricUnavailable").jsonPrimitive.boolean,
                selection.fixedMetricUnavailable,
            )
        }
    }

    @Test
    fun `missing fixed metric is reported while real metrics remain`() {
        val selection = WidgetMetricRanker.rank(
            providerId = ProviderId.CommandCode,
            metrics = listOf(progress("five-hour", 86), progress("weekly", 43)),
            fixedMetricId = "not-present",
            capacity = 2,
        )

        assertEquals("five-hour", selection.primary?.id)
        assertEquals(true, selection.fixedMetricUnavailable)
        assertEquals(listOf("five-hour", "weekly"), selection.orderedMetrics.map { it.id })
    }

    private fun metric(json: JsonObject) = UsageMetric(
        id = json.getValue("id").jsonPrimitive.content,
        label = json.getValue("label").jsonPrimitive.content,
        used = json["used"]?.jsonPrimitive?.content?.toBigDecimalOrNull(),
        limit = json["limit"]?.jsonPrimitive?.content?.toBigDecimalOrNull(),
        remaining = json["remaining"]?.jsonPrimitive?.content?.toBigDecimalOrNull(),
        value = json["value"]?.jsonPrimitive?.content?.toBigDecimalOrNull(),
        unit = UsageMetricUnit.fromRawValue(json.getValue("unit").jsonPrimitive.content)!!,
        presentation = UsageMetricPresentation.fromRawValue(
            json.getValue("presentation").jsonPrimitive.content
        )!!,
        semantic = UsageMetricSemantic.fromRawValue(json.getValue("semantic").jsonPrimitive.content)!!,
        currencyCode = json["currencyCode"]?.jsonPrimitive?.contentOrNull,
        healthState = UsageMetricHealthState.Normal,
    )

    private fun progress(id: String, percent: Int) = UsageMetric(
        id = id,
        label = id,
        used = percent.toBigDecimal(),
        limit = 100.toBigDecimal(),
        remaining = (100 - percent).toBigDecimal(),
        unit = UsageMetricUnit.Token,
        presentation = UsageMetricPresentation.Progress,
        semantic = UsageMetricSemantic.UsedQuota,
        healthState = UsageMetricHealthState.Normal,
    )
}
