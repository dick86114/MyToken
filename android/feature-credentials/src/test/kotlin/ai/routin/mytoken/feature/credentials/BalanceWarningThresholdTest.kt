package ai.routin.mytoken.feature.credentials

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure gear mapping for the 低余额预警值 slider. */
class BalanceWarningThresholdTest {

    private fun amounts(rawValue: String): List<String> =
        BalanceWarningThreshold.sliderModel(rawValue).amounts.map {
            it.stripTrailingZeros().toPlainString()
        }

    @Test
    fun gearsAreOrderedWithOffFirstAndCommonValuesEvenlySpaced() {
        assertEquals(
            listOf(
                "0", "5", "10", "20", "30", "50", "80", "100", "150", "200",
                "300", "500", "800", "1000", "2000", "3000", "5000", "10000",
            ),
            amounts(""),
        )
    }

    @Test
    fun blankAndZeroSelectOff() {
        assertEquals(0, BalanceWarningThreshold.sliderModel("").selectedIndex)
        assertEquals(0, BalanceWarningThreshold.sliderModel("0").selectedIndex)
        assertEquals(0, BalanceWarningThreshold.sliderModel("0.0").selectedIndex)
        assertEquals("不提醒", BalanceWarningThreshold.label("0"))
        assertEquals("不提醒", BalanceWarningThreshold.label(""))
    }

    @Test
    fun gearIndexMapsToItsAmountAndSavesAnInteger() {
        val model = BalanceWarningThreshold.sliderModel("10")
        assertEquals(2, model.selectedIndex)
        assertEquals(16, model.steps)
        assertEquals("10", BalanceWarningThreshold.onValueChanged(model, 2f))
        assertEquals("20", BalanceWarningThreshold.onValueChanged(model, 3f))
        assertEquals("", BalanceWarningThreshold.onValueChanged(model, 0f))
        assertEquals("余额低于 10 时提醒", BalanceWarningThreshold.label("10"))
    }

    @Test
    fun decimalValueIsInsertedBetweenNeighborGearsAndPreserved() {
        val model = BalanceWarningThreshold.sliderModel("8.5")

        assertEquals(2, model.selectedIndex)
        assertEquals(17, model.steps)
        assertEquals("8.5", model.amounts[2].stripTrailingZeros().toPlainString())
        assertEquals("10", model.amounts[3].stripTrailingZeros().toPlainString())
        assertEquals("余额低于 8.5 时提醒", BalanceWarningThreshold.label("8.5"))

        // Untouched custom slot keeps the decimal; moving to a gear writes integers.
        assertEquals("8.5", BalanceWarningThreshold.onValueChanged(model, 2f))
        assertEquals("10", BalanceWarningThreshold.onValueChanged(model, 3f))
        assertEquals("5", BalanceWarningThreshold.onValueChanged(model, 1f))
    }

    @Test
    fun largeValuesAreInsertedAtTheirSortedPosition() {
        val fifteenHundred = BalanceWarningThreshold.sliderModel("1500")
        assertEquals("1500", fifteenHundred.amounts[14].stripTrailingZeros().toPlainString())
        assertEquals(14, fifteenHundred.selectedIndex)
        assertEquals("2000", fifteenHundred.amounts[15].stripTrailingZeros().toPlainString())
        assertEquals("1500", BalanceWarningThreshold.onValueChanged(fifteenHundred, 14f))
        assertEquals("2000", BalanceWarningThreshold.onValueChanged(fifteenHundred, 15f))

        val overMax = BalanceWarningThreshold.sliderModel("20000")
        assertEquals(18, overMax.maxIndex)
        assertEquals(18, overMax.selectedIndex)
        assertEquals("20000", overMax.amounts.last().stripTrailingZeros().toPlainString())
        assertEquals("余额低于 20000 时提醒", BalanceWarningThreshold.label("20000"))
        assertEquals("20000", BalanceWarningThreshold.onValueChanged(overMax, 18f))
        assertEquals("10000", BalanceWarningThreshold.onValueChanged(overMax, 17f))
    }
}
