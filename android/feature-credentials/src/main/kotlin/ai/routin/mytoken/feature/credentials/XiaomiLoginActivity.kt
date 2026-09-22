package ai.routin.mytoken.feature.credentials

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.core.view.WindowCompat
import ai.routin.mytoken.core.ui.GlassButton
import ai.routin.mytoken.core.ui.GlassButtonTone

private const val XiaomiConsoleUrl = "https://platform.xiaomimimo.com/console/balance"
private const val CHROME_MOBILE_UA =
    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

/**
 * 独立 Activity 承载小米登录 WebView：脱离主窗口的 Dialog/Overlay，
 * 拥有自己的 Window、Insets 和返回栈，彻底解决白屏与系统栏遮挡。
 */
class XiaomiLoginActivity : ComponentActivity() {

    companion object {
        const val EXTRA_RESET_SESSION = "reset_session"
        const val RESULT_COOKIE = "cookie"
        val XiaomiConsoleUrlForLogin get() = "https://platform.xiaomimimo.com/console/balance"
        private const val CHROME_MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val resetSession = intent?.getBooleanExtra(EXTRA_RESET_SESSION, false) ?: false

        setContent {
            MaterialTheme {
                XiaomiLoginScreen(
                    resetSession = resetSession,
                    onCaptured = { cookie ->
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(RESULT_COOKIE, cookie),
                        )
                        finish()
                    },
                    onDismiss = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun XiaomiLoginScreen(
    resetSession: Boolean,
    onCaptured: (String) -> Unit,
    onDismiss: () -> Unit,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "登录小米 MiMo")
                        Text(
                            text = "登录后点击“使用当前登录状态”",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "关闭",
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            blankRecoveryAttempts = 0
                            reloadKey += 1
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "重新加载登录页",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                key(reloadKey) {
                    AndroidView(
                        factory = { context ->
                            var authRecoveryAttempted = false
                            @SuppressLint("SetJavaScriptEnabled")
                            WebView(context).apply {
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
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
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
                                        if (view?.contentHeight == 0 && blankRecoveryAttempts < 1) {
                                            blankRecoveryAttempts += 1
                                            reloadKey += 1
                                        }
                                    }

                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: RenderProcessGoneDetail?,
                                    ): Boolean {
                                        errorMessage = "渲染进程异常退出，请点击右上角刷新重试"
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
                                        val isUnauthorized =
                                            request?.url?.toString()
                                                ?.contains("/api/v1/userProfile") == true &&
                                                errorResponse?.statusCode == 401
                                        if (!isUnauthorized || authRecoveryAttempted) return
                                        authRecoveryAttempted = true
                                        CookieManager.getInstance().removeAllCookies(null)
                                        CookieManager.getInstance().flush()
                                        view?.loadUrl(
                                            "https://platform.xiaomimimo.com/console/balance",
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
