package ai.routin.mytoken.feature.credentials

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialSecret
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.usage.CredentialSessionRenewer
import ai.routin.mytoken.core.diagnostics.AppLog
import ai.routin.mytoken.core.diagnostics.AppLogLevel
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** 真正执行兑换的一步：给定旧票据，返回兑换后的 Cookie；无变化或失败返回 null。 */
fun interface XiaomiCookieExchange {
    suspend fun exchange(previous: String?): String?
}

/**
 * 判断隐藏 WebView 是否已确认停在必须人工操作的登录页。
 *
 * account.xiaomi.com/pass/serviceLogin 也可能只是浏览器自动换票的 SSO 中转页：
 * 持久 passToken 仍有效时会继续跳回控制台并换发新的短期票据。这里不能仅凭
 * 账号域提前终止，否则每次 24 小时服务票据过期都会误判为需要重新登录。
 */
object XiaomiLoginPage {
    private const val ACCOUNT_HOST = "account.xiaomi.com"
    private const val LOGIN_HINT = "login"

    fun requiresInteractiveLogin(url: String?): Boolean {
        val target = url?.trim()?.takeIf(String::isNotEmpty) ?: return false
        val lowercased = target.lowercase()
        val host = runCatching { java.net.URI(lowercased).host.orEmpty() }.getOrDefault("")
        if (host == ACCOUNT_HOST || host.endsWith(".$ACCOUNT_HOST")) return false
        if (host.endsWith("xiaomimimo.com") || host == "xiaomimimo.com") {
            return lowercased.contains("/login") || lowercased.contains("#/login")
        }
        return lowercased.contains(LOGIN_HINT) && lowercased.contains("xiaomi")
    }
}

/**
 * 小米 MiMo 静默续期：控制台只认短期服务票据（api-platform_serviceToken），
 * 但小米账号登录态（passToken 等）存在同一个持久 Cookie 罐里。票据过期时用隐藏
 * WebView 重新走一遍控制台，站点会静默换发新票据，不需要用户再次登录。
 */
class XiaomiSessionRenewer(
    private val exchange: XiaomiCookieExchange,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : CredentialSessionRenewer {

    constructor(
        context: Context,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
        pollIntervalMillis: Long = DEFAULT_POLL_INTERVAL_MILLIS,
    ) : this(
        XiaomiCookieExchange { previous ->
            WebViewCookieExchange(context.applicationContext, timeoutMillis, pollIntervalMillis)
                .execute(previous)
        }
    )

    private val lastFailureAtMillis = mutableMapOf<java.util.UUID, Long>()
    private val failureLock = Any()

    private fun failureCooldownActive(id: java.util.UUID): Boolean = synchronized(failureLock) {
        val last = lastFailureAtMillis[id] ?: return false
        nowMillis() - last < FAILURE_COOLDOWN_MILLIS
    }

    private fun recordFailure(id: java.util.UUID) {
        synchronized(failureLock) { lastFailureAtMillis[id] = nowMillis() }
    }

    private fun clearFailure(id: java.util.UUID) {
        synchronized(failureLock) { lastFailureAtMillis.remove(id) }
    }

    override suspend fun renew(
        credential: Credential,
        previousSecret: CredentialSecret,
    ): CredentialSecret? {
        if (credential.providerId != ProviderId.Xiaomi) return null
        // 账号登录态真失效时兑换必然失败；冷却期内直接放弃，避免每次刷新都拉一个 WebView。
        if (failureCooldownActive(credential.id)) {
            AppLog.log(AppLogLevel.Info, "xiaomi_renew_skipped", "冷却中")
            return null
        }
        val previous = (previousSecret as? CredentialSecret.BearerToken)?.token?.trim()

        val candidate = exchange.exchange(previous)?.trim()?.takeIf(String::isNotEmpty)
        if (candidate == null || candidate == previous) {
            AppLog.log(
                AppLogLevel.Warning,
                "xiaomi_renew_failed",
                if (candidate == null) "未换到新票据" else "票据未变化，账号登录态已失效",
            )
            recordFailure(credential.id)
            return null
        }
        // 拿到的还是同一串票据说明账号登录态也已失效，交给用户重新登录。
        AppLog.log(AppLogLevel.Info, "xiaomi_renew_succeeded", "已换发新的服务票据")
        clearFailure(credential.id)
        return CredentialSecret.BearerToken(candidate)
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS: Long = 25_000
        const val DEFAULT_POLL_INTERVAL_MILLIS: Long = 500
        const val FAILURE_COOLDOWN_MILLIS: Long = 10 * 60 * 1000
    }
}

/**
 * 用不可见的 WebView 打开控制台，等站点换发新的服务票据后读回 Cookie。
 * 整个过程只在主线程操作 WebView，结束后立刻销毁。
 */
private class WebViewCookieExchange(
    private val context: Context,
    private val timeoutMillis: Long,
    private val pollIntervalMillis: Long,
) {
    suspend fun execute(previous: String?): String? = withContext(Dispatchers.Main) {
        withTimeoutOrNull(timeoutMillis) {
            val webView = WebView(context)
            try {
                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true
                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(webView, true)
                }

                val loaded = CompletableDeferred<Unit>()
                var loginPageReached = false
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        if (XiaomiLoginPage.requiresInteractiveLogin(url)) {
                            // 落到交互式登录页：立即放弃，不再等满超时。
                            loginPageReached = true
                            loaded.complete(Unit)
                            view?.stopLoading()
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        loaded.complete(Unit)
                    }
                }
                webView.loadUrl(XiaomiCookieReader.renewUrl)
                loaded.await()
                if (loginPageReached) return@withTimeoutOrNull null

                var cookie = XiaomiCookieReader.readCookieHeader()
                while ((cookie == null || cookie == previous) &&
                    !loginPageReached &&
                    currentCoroutineContext().isActive
                ) {
                    delay(pollIntervalMillis)
                    cookie = XiaomiCookieReader.readCookieHeader()
                }
                CookieManager.getInstance().flush()
                if (loginPageReached) null else cookie?.takeIf { it != previous }
            } finally {
                webView.stopLoading()
                webView.destroy()
            }
        }
    }
}
