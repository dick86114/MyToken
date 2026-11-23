package ai.routin.mytoken.feature.credentials

import java.math.BigDecimal
import kotlin.math.roundToInt

/**
 * Non-linear discrete gears for the 低余额预警值 slider. The common range (5–50)
 * gets the same physical spacing as the extreme gears; an existing value outside
 * the gear list (e.g. 8.5 or 1500) is inserted at its sorted position so it can
 * still be displayed without rewriting it until the user moves the slider.
 *
 * 0 = off. Slider movement saves a gear amount; untouched custom values keep
 * their original metadata spelling.
 */
object BalanceWarningThreshold {

    /** Ordered gears; index 0 is "off". */
    private val BASE_AMOUNTS: List<BigDecimal> = listOf(
        "0", "5", "10", "20", "30", "50", "80", "100", "150", "200",
        "300", "500", "800", "1000", "2000", "3000", "5000", "10000",
    ).map(::BigDecimal)

    data class SliderModel(
        val amounts: List<BigDecimal>,
        val selectedIndex: Int,
        val rawValue: String,
        val customIndex: Int?,
    ) {
        val maxIndex: Int get() = amounts.lastIndex
        val steps: Int get() = (amounts.size - 2).coerceAtLeast(0)
    }

    fun sliderModel(rawValue: String): SliderModel {
        val raw = rawValue.trim()
        val amount = raw.toBigDecimalOrNull()
        if (amount == null || amount <= BigDecimal.ZERO) {
            return SliderModel(BASE_AMOUNTS, selectedIndex = 0, rawValue = raw, customIndex = null)
        }
        val baseIndex = BASE_AMOUNTS.indexOfFirst { it.compareTo(amount) == 0 }
        if (baseIndex >= 0) {
            return SliderModel(BASE_AMOUNTS, selectedIndex = baseIndex, rawValue = raw, customIndex = null)
        }
        val amounts = (BASE_AMOUNTS + amount).sortedWith(compareBy { it })
        val customIndex = amounts.indexOfFirst { it.compareTo(amount) == 0 }
        return SliderModel(amounts, selectedIndex = customIndex, rawValue = raw, customIndex = customIndex)
    }

    /**
     * Maps a slider index value back to metadata. 0 clears the override; a gear
     * saves its integer amount; the inserted custom slot preserves its spelling.
     */
    fun onValueChanged(model: SliderModel, value: Float): String {
        val index = value.roundToInt().coerceIn(model.amounts.indices)
        if (index == 0) return ""
        if (index == model.customIndex) return model.rawValue
        return model.amounts[index].stripTrailingZeros().toPlainString()
    }

    fun label(rawValue: String): String {
        val amount = rawValue.trim().toBigDecimalOrNull()
        if (amount == null || amount <= BigDecimal.ZERO) return "不提醒"
        return "余额低于 ${rawValue.trim()} 时提醒"
    }
}
