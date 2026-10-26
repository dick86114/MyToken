package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageSnapshot
import ai.routin.mytoken.domain.usage.RefreshStatus
import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProviderLogoAvatarBitmapTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val now = Instant.parse("2026-10-26T10:00:00Z")

    @Test
    fun `share avatar uses provider logo instead of alias initial`() {
        val aliasA = renderAvatar(displayName = "甲", providerId = ProviderId.Glm)
        val aliasB = renderAvatar(displayName = "乙", providerId = ProviderId.Glm)
        val otherProvider = renderAvatar(displayName = "甲", providerId = ProviderId.OpenCode)

        assertTrue("同一供应商更换别名后头像不应变化", aliasA.sameAs(aliasB))
        assertFalse("不同供应商应显示不同 Logo", aliasA.sameAs(otherProvider))
    }

    private fun renderAvatar(displayName: String, providerId: ProviderId): Bitmap {
        val credential = Credential(
            id = UUID.randomUUID(),
            providerId = providerId,
            credentialKind = CredentialKind.ApiKey,
            name = displayName,
        )
        val cardUi = CredentialCardUi(
            credential = credential,
            status = RefreshStatus.Ready,
            snapshot = UsageSnapshot(
                credentialId = credential.id,
                fetchedAt = now,
                metrics = emptyList(),
                planName = "Coding Plan",
            ),
            isStale = false,
            error = null,
            freshness = Freshness(FreshnessLevel.JUST_NOW, "刚刚更新"),
        )
        val content = requireNotNull(UsageShareContentBuilder.build(cardUi, now))
        val draft = UsageShareContentBuilder.makeDraft(content).copy(template = UsageShareTemplate.Dark)
        val rendered = UsageShareContentBuilder.render(content, draft)
        val density = context.resources.displayMetrics.density
        val bitmap = renderUsageShareBitmap(rendered, context = context)
        val origin = (16f * density).toInt()
        val size = (36f * density).toInt()
        return Bitmap.createBitmap(bitmap, origin, origin, size, size).also {
            bitmap.recycle()
        }
    }
}
