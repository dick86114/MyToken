package ai.routin.mytoken.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelIdChipPaletteTest {
    @Test
    fun colorIndex_isStableAndWithinPalette() {
        val modelId = "doubao-seed-2-1-turbo"

        val first = ModelIdChipPalette.colorIndex(modelId)
        val second = ModelIdChipPalette.colorIndex(modelId)

        assertEquals(first, second)
        assertTrue(first in ModelIdChipPalette.emphasisIndices.indices)
        assertTrue(ModelIdChipPalette.emphasis(modelId) in 0f..1f)
    }

    @Test
    fun colorIndex_spreadsDistinctModelIds() {
        val indices = listOf(
            "ark-code-latest",
            "doubao-seed-2-0-lite",
            "glm-5.3",
            "kimi-k2.7-code",
            "deepseek-v4-pro",
        ).map(ModelIdChipPalette::colorIndex)

        assertTrue(indices.toSet().size >= 2)
    }
}
