package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.ProviderId
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderCatalogTest {
    @Test
    fun providerLogosMapToDistinctDrawables() {
        val expected = mapOf(
            ProviderId.Routin to R.drawable.share_provider_routin_logo,
            ProviderId.DeepSeek to R.drawable.share_provider_deepseek_logo,
            ProviderId.Glm to R.drawable.share_provider_glm_logo,
            ProviderId.Volcengine to R.drawable.share_provider_volcengine_logo,
            ProviderId.NewAPI to R.drawable.share_provider_newapi_logo,
            ProviderId.CommandCode to R.drawable.share_provider_commandcode_logo,
            ProviderId.Xiaomi to R.drawable.share_provider_xiaomi_logo,
            ProviderId.OpenCode to R.drawable.share_provider_opencode_logo,
        )

        assertEquals(expected, ProviderId.entries.associateWith(ProviderCatalog::logoRes))
        assertEquals(expected.size, expected.values.toSet().size)
    }

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
