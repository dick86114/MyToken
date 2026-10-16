package ai.routin.mytoken.feature.credentials

import org.json.JSONObject
import org.json.JSONTokener

/**
 * 判断小米 SSO 页面是不是「内容在 DOM 里、但因为 #root 高度塌成 0 而没画出来」。
 *
 * 真机实测：安全验证页（authStart）的 #root 计算高度是 0 且 overflow 为 auto，
 * 内容被整块裁剪，界面表现为纯白；而正常的登录页 #root 高度是 834。
 * MiMo 控制台虽然 #root 也是 0，但它的内容在固定层里，属于正常情况，所以按域名排除。
 */
internal object LoginPageHealth {

    const val SSO_HOST = "account.xiaomi.com"

    /** evaluateJavascript 返回的是 JSON 字符串字面量，这里解出里面的 JSON 对象。 */
    fun parseProbeResult(rawResult: String?): JSONObject? {
        if (rawResult.isNullOrBlank()) return null
        val payload = runCatching { JSONTokener(rawResult).nextValue() as? String }.getOrNull()
            ?: return null
        return runCatching { JSONObject(payload) }.getOrNull()
    }

    fun isSsoPage(url: String?): Boolean = url?.contains(SSO_HOST) == true

    /** 只有 SSO 页面、内容非空、视口中心没有真实元素、且 #root 高度塌陷时才需要修复。 */
    fun needsRootRepair(url: String?, probe: JSONObject?): Boolean {
        if (probe == null || !isSsoPage(url)) return false
        val rootHeight = probe.optJSONObject("root")?.optInt("h", -1) ?: return false
        if (rootHeight > 0) return false
        if (probe.optString("text").isEmpty()) return false
        val hitTag = probe.optJSONObject("hit")?.optString("tag").orEmpty()
        return hitTag == "HTML" || hitTag == "BODY"
    }

    /** 视口中心没有任何真实元素，说明这一帧没画出来。 */
    fun centerIsEmpty(probe: JSONObject?): Boolean {
        val hitTag = probe?.optJSONObject("hit")?.optString("tag").orEmpty()
        return hitTag == "HTML" || hitTag == "BODY"
    }
}
