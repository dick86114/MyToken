package ai.routin.mytoken.feature.credentials

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
 * Full-screen Xiaomi MiMo login surface.
 *
 * Uses a main-window overlay instead of Compose Dialog: WebView inside a Dialog
 * window is flaky on Android (hardware accel / insets) and is the main cause of
 * the persistent blank-page issue on retry.
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
    var reloadKey by remember { mutableStateOf(0) }
    var blankRecoveryAttempts by remember { mutableStateOf(0) }

    BackHandler(onBack = onDismiss)

    DisposableEffect(Unit) {
        onDispose { webView?.destroy() }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("xiaomi_login_dialog"),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                ) {
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

                IconButton(
                    onClick = {
                        blankRecoveryAttempts = 0
                        reloadKey += 1
                    },
                    modifier = Modifier.testTag("xiaomi_login_reload"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "重新加载登录页",
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("xiaomi_login_close"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭",
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                key(reloadKey) {
                    AndroidView(
                        factory = { context ->
                            var authRecoveryAttempted = false
                            @SuppressLint("SetJavaScriptEnabled")
                            WebView(context).apply {
                                // 小米控制台是重 JS SPA；使用移动版 Chrome UA + 宽视口。
                                settings.userAgentString = CHROME_MOBILE_UA
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.javaScriptCanOpenWindowsAutomatically = true
                                settings.setSupportMultipleWindows(false)
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                settings.mixedContentMode =
                                    WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                settings.cacheMode =
                                    if (resetSession || reloadKey > 0) {
                                        WebSettings.LOAD_NO_CACHE
                                    } else {
                                        WebSettings.LOAD_DEFAULT
                                    }
                                isFocusable = true
                                isFocusableInTouchMode = true
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance()
                                    .setAcceptThirdPartyCookies(this, true)
                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(
                                        view: WebView?,
                                        url: String?,
                                        favicon: android.graphics.Bitmap?,
                                    ) {
                                        isLoading = true
                                        errorMessage = null
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                        // SPA 渲染后内容高度为 0 时说明页面实际空白；
                                        // 自动换一个全新 WebView 实例重载一次。
                                        val blank = view?.contentHeight == 0
                                        if (blank && blankRecoveryAttempts < 1) {
                                            blankRecoveryAttempts += 1
                                            reloadKey += 1
                                        }
                                    }

                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: RenderProcessGoneDetail?,
                                    ): Boolean {
                                        errorMessage = "网页渲染进程异常退出，请点击右上角刷新重试"
                                        view?.destroy()
                                        if (webView === view) webView = null
                                        return true
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: android.webkit.WebResourceError?,
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            isLoading = false
                                            errorMessage = "登录页加载失败，请检查网络后刷新重试"
                                        }
                                    }

                                    override fun onReceivedHttpError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        errorResponse: WebResourceResponse?,
                                    ) {
                                        val isUnauthorizedProfile =
                                            request?.url?.toString()
                                                ?.contains("/api/v1/userProfile") == true &&
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
                                }
                                loadUrl(
                                    XiaomiConsoleUrl,
                                    mapOf("Cache-Control" to "no-cache"),
                                )
                                requestFocus()
                            }.also { webView = it }
                        },
                        onRelease = { it.destroy() },
                        update = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(
                    onClick = {
                        blankRecoveryAttempts = 0
                        reloadKey += 1
                    },
                ) {
                    Text(text = "页面空白？点此重新加载")
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("xiaomi_login_capture"),
                    tone = GlassButtonTone.Primary,
                    text = "使用当前登录状态",
                )
            }
        }
    }
}
