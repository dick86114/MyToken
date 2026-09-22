package ai.routin.mytoken.feature.credentials

import android.annotation.SuppressLint
import android.graphics.RenderEffect
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ai.routin.mytoken.core.ui.GlassButton
import ai.routin.mytoken.core.ui.GlassButtonTone

private const val XiaomiConsoleUrl = "https://platform.xiaomimimo.com/console/balance"
private const val CHROME_MOBILE_UA =
    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

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

/**
 * In-app Xiaomi MiMo login surface. The WebView owns the official login
 * session; only the user-triggered capture reads the cookie back out.
 */
@Composable
fun XiaomiLoginDialog(
    onCaptured: (String) -> Unit,
    onDismiss: () -> Unit,
    resetSession: Boolean = false,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose { webView?.destroy() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .testTag("xiaomi_login_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "登录小米 MiMo",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "登录完成后点击“使用当前登录状态”读取 Cookie。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(text = "关闭")
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(
                        factory = { context ->
                            var authRecoveryAttempted = false
                            @SuppressLint("SetJavaScriptEnabled")
                            WebView(context).apply {
                                // 小米控制台是重 JS SPA，默认 WebView UA 可能被前端拒绝渲染。
                                // 使用移动版 Chrome UA + 宽视口，显著降低白屏概率。
                                settings.userAgentString = CHROME_MOBILE_UA
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.javaScriptCanOpenWindowsAutomatically = true
                                settings.setSupportMultipleWindows(false)
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                settings.cacheMode =
                                    if (resetSession) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
                                isFocusable = true
                                isFocusableInTouchMode = true
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                        isLoading = true
                                        errorMessage = null
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                    }

                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: RenderProcessGoneDetail?,
                                    ): Boolean {
                                        errorMessage = "网页渲染进程异常退出，请重新打开登录窗口"
                                        view?.destroy()
                                        webView = null
                                        return true
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: android.webkit.WebResourceError?,
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            isLoading = false
                                            errorMessage = "登录页加载失败，请检查网络后重试"
                                        }
                                    }

                                    override fun onReceivedHttpError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        errorResponse: WebResourceResponse?,
                                    ) {
                                        val isUnauthorizedProfile =
                                            request?.url?.toString()?.contains("/api/v1/userProfile") == true &&
                                                errorResponse?.statusCode == 401
                                        if (!isUnauthorizedProfile || authRecoveryAttempted) return

                                        authRecoveryAttempted = true
                                        CookieManager.getInstance().removeAllCookies(null)
                                        CookieManager.getInstance().flush()
                                        view?.loadUrl(
                                            XiaomiConsoleUrl,
                                            mapOf("Cache-Control" to "no-cache"),
                                        )
                                    }
                                }
                                if (resetSession) {
                                    clearCache(true)
                                    clearHistory()
                                    WebStorage.getInstance().deleteAllData()
                                    CookieManager.getInstance().removeAllCookies(null)
                                    CookieManager.getInstance().flush()
                                    loadUrl(
                                        XiaomiConsoleUrl,
                                        mapOf("Cache-Control" to "no-cache"),
                                    )
                                } else {
                                    loadUrl(
                                        XiaomiConsoleUrl,
                                        mapOf("Cache-Control" to "no-cache"),
                                    )
                                }
                                requestFocus()
                            }.also { webView = it }
                        },
                        update = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    GlassButton(
                        onClick = {
                            CookieManager.getInstance().flush()
                            val cookie = XiaomiCookieReader.readCookieHeader()
                            if (cookie == null) {
                                errorMessage = "未读取到有效登录态，请确认已登录控制台"
                            } else {
                                onCaptured(cookie)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("xiaomi_login_capture"),
                        tone = GlassButtonTone.Primary,
                        text = "使用当前登录状态",
                    )
                }
            }
        }
    }
}
