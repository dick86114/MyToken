package ai.routin.mytoken.feature.credentials

import android.webkit.CookieManager

private const val XiaomiConsoleUrl = "https://platform.xiaomimimo.com/console/balance"

/** Reads the current Xiaomi MiMo web session cookie without exposing it to logs. */
object XiaomiCookieReader {
    fun readCookieHeader(): String? {
        val cookie = CookieManager.getInstance().getCookie(XiaomiConsoleUrl).orEmpty()
        return cookie
            .takeIf { it.contains("api-platform_serviceToken") }
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }
}
