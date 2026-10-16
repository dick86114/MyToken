package ai.routin.mytoken.feature.credentials

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import ai.routin.mytoken.core.diagnostics.AppLog
import ai.routin.mytoken.core.diagnostics.AppLogLevel
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
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
/**
 * 小米 SSO 会按 UA 分发不同登录页：移动 UA 会停在只显示 cookie 提示的中间页
 * （表现就是白屏），桌面 UA 直接进入可正常渲染的账号密码页。macOS 端 WKWebView
 * 用的也是桌面 UA，行为一致。
 */
private const val CHROME_DESKTOP_UA =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

/** 同时写入应用内日志（可导出）与 logcat，真机和 USB 调试都能查。 */
/** 每个页面最多纠正两次，避免和页面自身的渲染逻辑反复拉扯。 */
private class LoginPageRepairState {
    var attempts: Int = 0

    fun canAttempt(): Boolean = attempts < MAX_ATTEMPTS

    private companion object {
        const val MAX_ATTEMPTS = 2
    }
}

private fun logEvent(level: AppLogLevel, event: String, details: String?) {
    AppLog.log(level, event, details)
    val message = if (details.isNullOrEmpty()) event else "$event - $details"
    when (level) {
        AppLogLevel.Info -> android.util.Log.i(LOG_TAG, message)
        AppLogLevel.Warning -> android.util.Log.w(LOG_TAG, message)
        AppLogLevel.Error -> android.util.Log.e(LOG_TAG, message)
    }
}

private const val LOG_TAG = "XiaomiLogin"

/**
 * 安全验证页的 #root 在 WebView 里会塌成 0 高度（`height:100%` 链断在 body 上），
 * 它又带 `overflow:auto`，于是整块内容被裁掉、界面纯白。把根节点改成按内容撑开即可恢复。
 */
private const val ROOT_REPAIR_SCRIPT = """
(function () {
  try {
    var root = document.getElementById('root');
    if (!root) { return 'no_root'; }
    root.style.height = 'auto';
    root.style.minHeight = innerHeight + 'px';
    root.style.overflow = 'visible';
    if (document.body) { document.body.style.height = 'auto'; }
    return 'repaired h=' + Math.round(root.getBoundingClientRect().height) + ' vh=' + innerHeight;
  } catch (error) {
    return 'repair_error:' + error;
  }
})()
"""

/**
 * 采集页面 DOM 概要：标题、可见文本、表单控件与弹层数量。
 * 页面白屏但接口正常时，这些字段能说明“页面到底是空的还是被挡住了”。
 */
private const val PAGE_STATE_PROBE = """
(function () {
  try {
    var body = document.body;
    var text = body && body.innerText ? body.innerText.replace(/\s+/g, ' ') : '';
    if (text.length > 140) { text = text.slice(0, 140); }
    function describe(element) {
      if (!element) { return null; }
      var style = getComputedStyle(element);
      var rect = element.getBoundingClientRect();
      return {
        tag: element.tagName,
        cls: String(element.className || '').slice(0, 40),
        w: Math.round(rect.width),
        h: Math.round(rect.height),
        top: Math.round(rect.top),
        opacity: style.opacity,
        visibility: style.visibility,
        display: style.display,
        overflow: style.overflow
      };
    }
    return JSON.stringify({
      title: document.title,
      text: text,
      iframes: document.querySelectorAll('iframe').length,
      inputs: document.querySelectorAll('input').length,
      buttons: document.querySelectorAll('button, a').length,
      overlays: document.querySelectorAll('[role=dialog], .dialog, .modal, .ant-modal, .miui-dialog').length,
      canvases: document.querySelectorAll('canvas').length,
      bodyHeight: body ? body.scrollHeight : -1,
      docHeight: document.documentElement ? document.documentElement.scrollHeight : -1,
      viewport: innerWidth + 'x' + innerHeight,
      vis: document.visibilityState,
      scrollY: Math.round(scrollY),
      body: describe(body),
      root: describe(document.getElementById('root')),
      hit: describe(document.elementFromPoint(innerWidth / 2, innerHeight / 2)),
      ready: document.readyState
    });
  } catch (error) {
    return 'probe_error:' + error;
  }
})()
"""

/** 加载完成后立刻采一次，1.5 秒后再采一次，避免错过延迟渲染。 */
private fun probePageState(view: WebView?, url: String?, repairState: LoginPageRepairState) {
    val target = view ?: return
    target.evaluateJavascript(PAGE_STATE_PROBE) { result ->
        logEvent(AppLogLevel.Info, "login_page_state", result)
        handleBlankPage(target, url, result, repairState)
    }
    target.postDelayed(
        {
            target.evaluateJavascript(PAGE_STATE_PROBE) { result ->
                logEvent(AppLogLevel.Info, "login_page_state_late", result)
                // 页面自己的 JS 可能重渲染后又把布局压回去，所以复查也要修一次。
                handleBlankPage(target, url, result, repairState)
            }
        },
        1_500,
    )
}

/**
 * 内容在 DOM 里却没显示时，先请求一次重新布局；如果确认是小米 SSO 页面
 * 的 #root 高度塌成 0 把内容裁掉了，就把根节点撑回内容高度。
 * 只改样式，不重新导航、不动登录态。
 */
private fun handleBlankPage(
    view: WebView,
    url: String?,
    probeResult: String?,
    repairState: LoginPageRepairState,
) {
    val probe = LoginPageHealth.parseProbeResult(probeResult) ?: return
    if (!LoginPageHealth.centerIsEmpty(probe)) return
    logEvent(AppLogLevel.Warning, "login_repaint_nudged", probe.toString().take(200))
    view.requestLayout()
    view.invalidate()
    if (!LoginPageHealth.needsRootRepair(url, probe)) return
    if (!repairState.canAttempt()) {
        logEvent(AppLogLevel.Warning, "login_root_repair_skipped", "attempts=${repairState.attempts}")
        return
    }
    repairState.attempts += 1
    view.evaluateJavascript(ROOT_REPAIR_SCRIPT) { result ->
        logEvent(
            AppLogLevel.Info,
            "login_root_repair_result",
            "attempt=${repairState.attempts} result=$result",
        )
        view.postDelayed(
            {
                view.evaluateJavascript(PAGE_STATE_PROBE) { after ->
                    logEvent(AppLogLevel.Info, "login_page_state_after_repair", after)
                }
            },
            800,
        )
    }
}

/**
 * 独立 Activity 承载小米登录 WebView：脱离主窗口的 Dialog/Overlay，
 * 拥有自己的 Window、Insets 和返回栈，彻底解决白屏与系统栏遮挡。
 */
class XiaomiLoginActivity : ComponentActivity() {

    companion object {
        const val EXTRA_RESET_SESSION = "reset_session"
        const val RESULT_COOKIE = "cookie"
        val XiaomiConsoleUrlForLogin get() = "https://platform.xiaomimimo.com/console/balance"
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val resetSession = intent?.getBooleanExtra(EXTRA_RESET_SESSION, false) ?: false
        // Debug 构建开放 WebView 调试，便于在真机上排查登录页白屏。
        if ((applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

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
    val repairState = remember { LoginPageRepairState() }

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
                            @SuppressLint("SetJavaScriptEnabled")
                            WebView(context).apply {
                                settings.userAgentString = CHROME_DESKTOP_UA
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.javaScriptCanOpenWindowsAutomatically = true
                                // 小米 SSO 的二次验证（验证码）页会用 window.open 打开弹层；
                                // 关掉多窗口会让调用静默失败，页面就一直停在空白。
                                settings.setSupportMultipleWindows(true)
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
                                webChromeClient = object : WebChromeClient() {
                                    override fun onCreateWindow(
                                        view: WebView?,
                                        isDialog: Boolean,
                                        isUserGesture: Boolean,
                                        resultMsg: android.os.Message?,
                                    ): Boolean {
                                        val transport = resultMsg?.obj as? WebView.WebViewTransport
                                            ?: return false
                                        logEvent(
                                            AppLogLevel.Info,
                                            "login_popup_requested",
                                            "isDialog=$isDialog isUserGesture=$isUserGesture",
                                        )
                                        // 用一个临时 WebView 接收弹窗目标，再把地址交回主 WebView 加载，
                                        // 这样验证码页仍然留在同一个 WebView 里，登录态不丢。
                                        val popup = WebView(view?.context ?: return false)
                                        popup.webViewClient = object : WebViewClient() {
                                            override fun shouldOverrideUrlLoading(
                                                popupView: WebView?,
                                                request: WebResourceRequest?,
                                            ): Boolean {
                                                val url = request?.url?.toString()
                                                logEvent(AppLogLevel.Info, "login_popup_redirected", url)
                                                url?.let { view?.loadUrl(it) }
                                                popupView?.destroy()
                                                return true
                                            }
                                        }
                                        transport.webView = popup
                                        resultMsg.sendToTarget()
                                        return true
                                    }

                                    override fun onConsoleMessage(
                                        consoleMessage: android.webkit.ConsoleMessage?,
                                    ): Boolean {
                                        logEvent(
                                            AppLogLevel.Warning,
                                            "login_page_console",
                                            "${consoleMessage?.messageLevel()} ${consoleMessage?.message()}",
                                        )
                                        return false
                                    }
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                    ): Boolean {
                                        val url = request?.url?.toString().orEmpty()
                                        // WebView 处理不了的 scheme（tel/sms/intent 等）直接拒绝，
                                        // 但必须留日志：验证码页可能就卡在这里。
                                        val scheme = request?.url?.scheme?.lowercase()
                                        if (scheme != null && scheme != "http" && scheme != "https") {
                                            logEvent(
                                                AppLogLevel.Warning,
                                                "login_non_http_navigation",
                                                url,
                                            )
                                            return true
                                        }
                                        logEvent(AppLogLevel.Info, "login_navigation", url)
                                        return false
                                    }
                                    override fun onPageStarted(
                                        view: WebView?,
                                        url: String?,
                                        favicon: android.graphics.Bitmap?,
                                    ) {
                                        logEvent(AppLogLevel.Info, "login_page_started", url)
                                        isLoading = true
                                        errorMessage = null
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        logEvent(
                                            AppLogLevel.Info,
                                            "login_page_finished",
                                            "contentHeight=${view?.contentHeight} url=${url?.take(160)}",
                                        )
                                        isLoading = false
                                        // 页面已经加载完成却看不到内容时（例如二次验证页），
                                        // 把 DOM 关键信息写进日志，真机排查不用连 adb。
                                        repairState.attempts = 0
                                        probePageState(view, url, repairState)
                                    }

                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: RenderProcessGoneDetail?,
                                    ): Boolean {
                                        logEvent(
                                            AppLogLevel.Error,
                                            "login_renderer_gone",
                                            "didCrash=${detail?.didCrash()}",
                                        )
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
                                        logEvent(
                                            AppLogLevel.Error,
                                            "login_page_error",
                                            "main=${request?.isForMainFrame} code=${error?.errorCode} " +
                                                "desc=${error?.description} url=${request?.url}",
                                        )
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
                                        logEvent(
                                            AppLogLevel.Warning,
                                            "login_page_http_error",
                                            "status=${errorResponse?.statusCode} url=${request?.url}",
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
                            logEvent(
                                AppLogLevel.Warning,
                                "login_capture_failed",
                                "未读取到 api-platform_serviceToken",
                            )
                            errorMessage = "未读取到有效登录态，请确认已登录控制台"
                        } else {
                            logEvent(AppLogLevel.Info, "login_capture_succeeded", "cookie 已读取")
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
