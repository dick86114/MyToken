package ai.routin.mytoken.feature.credentials

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.CredentialSecret
import ai.routin.mytoken.domain.model.ProviderId
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XiaomiSessionRenewerTest {

    @Test
    fun `renewed ticket replaces the expired secret`() = runTest {
        var seenPrevious: String? = null
        val renewer = XiaomiSessionRenewer(XiaomiCookieExchange { previous ->
            seenPrevious = previous
            "api-platform_serviceToken=new; userId=1"
        })

        val renewed = renewer.renew(
            credential(ProviderId.Xiaomi),
            CredentialSecret.BearerToken("api-platform_serviceToken=old; userId=1"),
        )

        assertEquals(
            CredentialSecret.BearerToken("api-platform_serviceToken=new; userId=1"),
            renewed,
        )
        assertEquals("api-platform_serviceToken=old; userId=1", seenPrevious)
    }

    @Test
    fun `unchanged ticket means the account session is gone`() = runTest {
        val renewer = XiaomiSessionRenewer(
            XiaomiCookieExchange { "api-platform_serviceToken=old; userId=1" }
        )

        val renewed = renewer.renew(
            credential(ProviderId.Xiaomi),
            CredentialSecret.BearerToken("api-platform_serviceToken=old; userId=1"),
        )

        // 同一串票据说明兑换没有发新票据，此时应交给用户重新登录。
        assertNull(renewed)
    }

    @Test
    fun `failed exchange falls back to manual login`() = runTest {
        val renewer = XiaomiSessionRenewer(XiaomiCookieExchange { null })

        assertNull(
            renewer.renew(
                credential(ProviderId.Xiaomi),
                CredentialSecret.BearerToken("api-platform_serviceToken=old"),
            )
        )
    }

    @Test
    fun `other providers are never exchanged`() = runTest {
        var exchangeCalls = 0
        val renewer = XiaomiSessionRenewer(XiaomiCookieExchange {
            exchangeCalls++
            "api-platform_serviceToken=new"
        })

        val renewed = renewer.renew(
            credential(ProviderId.Glm),
            CredentialSecret.BearerToken("glm-token"),
        )

        assertNull(renewed)
        assertEquals(0, exchangeCalls)
    }

    private fun credential(providerId: ProviderId) = Credential(
        id = UUID.randomUUID(),
        providerId = providerId,
        credentialKind = CredentialKind.BearerApiKey,
        name = "示例",
    )

    @Test
    fun `interactive login page is detected so renewal can bail out early`() {
        // 落到小米账号域或控制台登录页时，隐藏 WebView 等下去也不会拿到票据。
        assertTrue(
            XiaomiLoginPage.requiresInteractiveLogin(
                "https://account.xiaomi.com/pass/serviceLogin?callback=https%3A%2F%2Fplatform.xiaomimimo.com"
            )
        )
        assertTrue(
            XiaomiLoginPage.requiresInteractiveLogin(
                "https://platform.xiaomimimo.com/login?redirect=/console/balance"
            )
        )
        assertTrue(XiaomiLoginPage.requiresInteractiveLogin("https://platform.xiaomimimo.com/#/login"))
        // 正常的控制台页面不能误判。
        assertFalse(
            XiaomiLoginPage.requiresInteractiveLogin(
                "https://platform.xiaomimimo.com/console/balance"
            )
        )
        assertFalse(XiaomiLoginPage.requiresInteractiveLogin(null))
        assertFalse(XiaomiLoginPage.requiresInteractiveLogin(""))
    }

    @Test
    fun `failed renewal enters cooldown so refresh does not spin up a webview every time`() = runTest {
        var exchangeCalls = 0
        var now = 1_000L
        val renewer = XiaomiSessionRenewer(
            exchange = XiaomiCookieExchange {
                exchangeCalls++
                null
            },
            nowMillis = { now },
        )
        val credential = credential(ProviderId.Xiaomi)
        val expired = CredentialSecret.BearerToken("api-platform_serviceToken=old")

        assertNull(renewer.renew(credential, expired))
        assertNull(renewer.renew(credential, expired))
        assertEquals(1, exchangeCalls)

        // 冷却期过后再试一次，用户重新登录后能立刻恢复。
        now += XiaomiSessionRenewer.FAILURE_COOLDOWN_MILLIS
        assertNull(renewer.renew(credential, expired))
        assertEquals(2, exchangeCalls)
    }

    @Test
    fun `successful renewal clears the cooldown state`() = runTest {
        var exchangeCalls = 0
        var now = 1_000L
        val renewer = XiaomiSessionRenewer(
            exchange = XiaomiCookieExchange { previous ->
                exchangeCalls++
                previous?.let { "api-platform_serviceToken=new-$exchangeCalls" }
            },
            nowMillis = { now },
        )
        val credential = credential(ProviderId.Xiaomi)
        val expired = CredentialSecret.BearerToken("api-platform_serviceToken=old")

        assertNotNull(renewer.renew(credential, expired))
        assertNotNull(renewer.renew(credential, expired))

        assertEquals(2, exchangeCalls)
    }
}
