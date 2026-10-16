package ai.routin.mytoken.feature.credentials

import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LoginPageHealthTest {

    private val ssoUrl = "https://account.xiaomi.com/fe/service/identity/authStart?sid=api-platform"

    private fun probe(
        rootHeight: Int,
        text: String,
        hitTag: String,
    ) = JSONObject(
        """
        {"title":"小米账号安全验证","text":"$text","root":{"tag":"DIV","h":$rootHeight,"overflow":"auto"},
         "hit":{"tag":"$hitTag"}}
        """.trimIndent()
    )

    @Test
    fun `解析 evaluateJavascript 返回的转义字符串`() {
        val raw = """"{\"title\":\"小米账号安全验证\",\"root\":{\"h\":0}}""""

        val parsed = LoginPageHealth.parseProbeResult(raw)

        assertNotNull(parsed)
        assertTrue(parsed!!.optJSONObject("root")!!.optInt("h") == 0)
    }

    @Test
    fun `验证码页内容被裁剪时需要修复`() {
        val needsRepair = LoginPageHealth.needsRootRepair(
            url = ssoUrl,
            probe = probe(rootHeight = 0, text = "发送验证码 为确认身份", hitTag = "HTML"),
        )

        assertTrue("根节点塌成 0 且内容在 DOM 里，必须修复", needsRepair)
    }

    @Test
    fun `正常渲染的登录页不修复`() {
        val loginProbe = probe(rootHeight = 834, text = "小米账号 登录 密码", hitTag = "INPUT")

        assertFalse(LoginPageHealth.needsRootRepair(ssoUrl, loginProbe))
    }

    @Test
    fun `没有内容的加载态不修复`() {
        val loadingProbe = probe(rootHeight = 0, text = "", hitTag = "HTML")

        assertFalse(LoginPageHealth.needsRootRepair(ssoUrl, loadingProbe))
    }

    @Test
    fun `MiMo 控制台根节点为 0 属正常不修复`() {
        val consoleProbe = probe(rootHeight = 0, text = "MiMo-V2.5-Pro", hitTag = "HTML")

        assertFalse(
            LoginPageHealth.needsRootRepair("https://platform.xiaomimimo.com/console/balance", consoleProbe),
        )
    }

    @Test
    fun `视口中心是真实控件时不判定为空白`() {
        assertFalse(LoginPageHealth.centerIsEmpty(probe(834, "登录", "LABEL")))
        assertTrue(LoginPageHealth.centerIsEmpty(probe(0, "登录", "HTML")))
    }
}
