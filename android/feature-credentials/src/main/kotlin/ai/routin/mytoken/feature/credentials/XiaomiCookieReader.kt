package ai.routin.mytoken.feature.credentials

import android.webkit.CookieManager

private const val XiaomiConsoleUrl = "https://platform.xiaomimimo.com/console/balance"
private const val XiaomiRenewUrl =
    "https://platform.xiaomimimo.com/api/v1/genLoginUrl?currentPath=%2Fconsole%2Fbalance"

/** Reads the current Xiaomi MiMo web session cookie without exposing it to logs. */
object XiaomiCookieReader {
    /** 登录用的控制台地址。 */
    const val consoleUrl: String = XiaomiConsoleUrl

    /** 与网页登录态失效时一致，先进入 SSO 入口再自动换票。 */
    const val renewUrl: String = XiaomiRenewUrl

    fun readCookieHeader(): String? {
        val cookie = CookieManager.getInstance().getCookie(XiaomiConsoleUrl).orEmpty()
        return cookie
            .takeIf { it.contains("api-platform_serviceToken") }
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }
}
