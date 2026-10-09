package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.ProviderId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageShareDefaultTest {
    @Test
    fun `default draft uses light ticket`() {
        assertEquals(
            UsageShareTemplate.TicketLight,
            UsageShareDraft(
                displayName = "Main",
                subtitle = "Main",
            ).template,
        )
    }

    @Test
    fun `provider branding maps official logo and configured url`() {
        val branding = ProviderShareBranding.make(
            providerId = ProviderId.OpenCode,
            websiteUrl = "https://opencode.ai/",
        )

        assertEquals(R.drawable.share_provider_opencode_logo, branding?.logoRes)
        assertEquals("https://opencode.ai", branding?.websiteDisplay)
        assertNull(ProviderShareBranding.make(ProviderId.OpenCode, null))
    }
}
