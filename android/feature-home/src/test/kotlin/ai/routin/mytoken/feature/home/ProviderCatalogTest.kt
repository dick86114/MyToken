package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.ProviderId
import kotlin.math.sqrt
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderCatalogTest {
    @Test
    fun xiaomiAccentColorIsVisuallyDistinctFromCommandCode() {
        val commandCode = ProviderCatalog.accentColor(ProviderId.CommandCode)
        val xiaomi = ProviderCatalog.accentColor(ProviderId.Xiaomi)
        val redDelta = commandCode.red - xiaomi.red
        val greenDelta = commandCode.green - xiaomi.green
        val blueDelta = commandCode.blue - xiaomi.blue
        val distance = sqrt(
            redDelta * redDelta +
                greenDelta * greenDelta +
                blueDelta * blueDelta
        )

        assertTrue("Command Code 和小米 MiMo 主题色过于接近", distance > 0.25)
    }
}
