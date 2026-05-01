package eric.schedule_exporter.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.view.ViewGroup.LayoutParams
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.telefonica.nestedscrollwebview.NestedScrollWebView
import eric.schedule_exporter.util.DUMPER_NAME
import eric.schedule_exporter.util.Dump
import eric.schedule_exporter.util.randomUniqueString
import eric.schedule_exporter.util.resolveLayoutParams


@Composable
fun WebView(
    state: WebViewContext,
    modifier: Modifier = Modifier,
    onCreated: (WebView) -> Unit = {}
) {
    BackHandler(state.canGoBack) {
        state.webView?.goBack()
    }

    val runningInPreview = LocalInspectionMode.current

    BoxWithConstraints(modifier) {
        AndroidView(
            factory = { context ->
                NestedScrollWebView(context).apply {
                    onCreated(this)

                    // WebView changes it's layout strategy based on
                    // it's layoutParams. We convert from Compose Modifier to
                    // layout params here.
                    layoutParams = LayoutParams(
                        resolveLayoutParams { hasFixedWidth },
                        resolveLayoutParams { hasFixedHeight }
                    )

                    webChromeClient = state.client
                    webViewClient = state
                    state.webView = this
                }
            }
        ) { view ->
            // AndroidViews are not supported by preview, bail early
            if (runningInPreview) return@AndroidView
            state.canGoBack = view.canGoBack()
            state.canGoForward = view.canGoForward()
        }
    }
}

class WebViewContext : WebViewClient() {
    var webView: WebView? by mutableStateOf(null)
        internal set
    var canGoBack: Boolean by mutableStateOf(false)
        internal set
    var canGoForward: Boolean by mutableStateOf(false)
        internal set
    var progress: Int? by mutableStateOf(null)
        internal set
    var title: String by mutableStateOf("")
        internal set
    var location: String by mutableStateOf("")
        internal set
    val errors: SnapshotStateList<WebViewError> = mutableStateListOf()
    val reports = mutableMapOf<String, List<Dump>>()

    @Suppress("unused")
    @JavascriptInterface
    fun uuid(): String {
        return randomUniqueString()
    }

    @Suppress("unused")
    @JavascriptInterface
    fun dump(uuid: String, url: String, html: String?) {
        val dump = Dump(url, html ?: "! NO ACCESS !")
        val existing = reports[uuid] ?: emptyList()
        reports[uuid] = existing + dump
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun init(webView: WebView) {
        webView.addJavascriptInterface(this, DUMPER_NAME)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mixedContentMode = MIXED_CONTENT_ALWAYS_ALLOW
            javaScriptCanOpenWindowsAutomatically = true
            allowFileAccess = true
            allowContentAccess = true
            setSupportMultipleWindows(false)
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        this.errors.clear()
        if (url !== null) {
            this.location = url
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        this.progress = null
        this.canGoBack = view?.canGoBack() ?: false
        this.canGoForward = view?.canGoForward() ?: false
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        if (error != null) {
            this.errors.add(WebViewError(request, error))
        }
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceResponse?
    ) {
    }

    @SuppressLint("WebViewClientOnReceivedSslError")
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.proceed()
    }

    val client = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            this@WebViewContext.progress = newProgress
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            this@WebViewContext.title = title ?: ""
        }
    }
}

@Immutable
data class WebViewError(val request: WebResourceRequest?, val error: WebResourceError)
