package eric.schedule_exporter

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.MainThread
import androidx.core.content.getSystemService
import androidx.core.graphics.Insets
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.behavior.HideViewOnScrollBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.search.SearchView
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar
import eric.schedule_exporter.databinding.ActivityMainBinding
import eric.schedule_exporter.parser.ParserContext
import eric.schedule_exporter.parser.ScheduleParser
import eric.schedule_exporter.url.UrlAdapter
import eric.schedule_exporter.url.UrlItem
import eric.schedule_exporter.util.DO_NOT_CONTINUE
import eric.schedule_exporter.util.DUMPER_NAME
import eric.schedule_exporter.util.DUMP_SOURCES
import eric.schedule_exporter.util.INJECT_CONSOLE
import eric.schedule_exporter.util.MIME_TYPE_CSV
import eric.schedule_exporter.util.MIME_TYPE_ZIP
import eric.schedule_exporter.util.QUOTED_DO_NOT_CONTINUE
import eric.schedule_exporter.util.getDumpDir
import eric.schedule_exporter.util.getScheduleDir
import eric.schedule_exporter.util.resolveUnique
import eric.schedule_exporter.util.runSilently
import eric.schedule_exporter.util.toUri
import eric.schedule_exporter.util.unwrapAndUnescape
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.roundToInt

class MainActivity : BaseActivity(), ParserContext {
    private val webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            viewModel.url.value = url
        }

        override fun onPageFinished(view: WebView, url: String?) {
            viewModel.progress.value = null
            val binding = this@MainActivity.binding
            val backward = binding.webview.canGoBack()
            goBackCallback.isEnabled = backward
            binding.backwardButton.isEnabled = backward
            binding.forwardButton.isEnabled = binding.webview.canGoForward()
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            val message =
                "${error.errorCode}: ${error.description}\n${request.method} ${request.url}"
            Log.e("WebView Error", message)
            Snackbar.make(view, message, LENGTH_LONG).setAnchorView(binding.dockedToolbar).show()
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceResponse
        ) {
            val message =
                "${error.statusCode} (${error.mimeType}): ${error.reasonPhrase}\n ${request.url}"
            Log.e("WebView Http Error", message)
            Snackbar.make(view, message, LENGTH_LONG).setAnchorView(binding.dockedToolbar).show()
        }

        @SuppressLint("WebViewClientOnReceivedSslError")
        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            handler.proceed()
        }
    }

    private val networkCallback = object :
        ConnectivityManager.NetworkCallback(),
        UrlItem.Observer {
        override val coroutineScope: CoroutineScope
            get() = this@MainActivity.lifecycleScope

        @MainThread
        override fun onItemChange(item: UrlItem) {
            val index = this@MainActivity.viewModel.urls.indexOf(item)
            if (index < 0) return
            this@MainActivity.urlAdapter.notifyItemChanged(index)
        }

        override fun onAvailable(network: Network) {
            this@MainActivity.viewModel.urls.forEach {
                it.onNetworkAvailable(network, this)
            }
        }
    }
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var urlAdapter: UrlAdapter

    private val goBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            binding.webview.goBack()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(true)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        val binding = ActivityMainBinding.inflate(this.layoutInflater)
        this.binding = binding
        this.setContentView(binding.root)
        val callback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                this@MainActivity.binding.searchView.handleBackInvoked()
            }
        }
        this.onBackPressedDispatcher.apply {
            addCallback(goBackCallback)
            addCallback(callback)
        }
        binding.searchBar.let {
            setSupportActionBar(it)
            binding.searchView.apply {
                inflateMenu(R.menu.menu_search)
                setOnMenuItemClickListener(this@MainActivity::onOptionsItemSelected)
                addTransitionListener { _, _, neo ->
                    callback.isEnabled = neo === SearchView.TransitionState.SHOWN
                }
                editText.setOnEditorActionListener { view, _, _ ->
                    val text = view.text
                    if (text.isBlank()) {
                        this@MainActivity.binding.searchView.hide()
                    } else {
                        this@MainActivity.navigateTo(text)
                    }
                    true
                }
                setupWithSearchBar(it)
            }
            it.setNavigationOnClickListener {
                this.binding.webview.apply {
                    val agent = settings.userAgentString
                    settings.userAgentString = if (agent.contains("Mobile", false)) {
                        "Mozilla/5.0 (X11; Linux x86_64" + agent.substring(
                            agent.indexOf(')', 12)
                        ).replace("Mobile Safari", "Safari")
                    } else WebSettings.getDefaultUserAgent(this@MainActivity)
                    reload()
                }
            }
            it.setOnClickListener {
                this.binding.searchView.apply {
                    show()
                    val url = this@MainActivity.viewModel.url.value ?: return@apply
                    editText.apply {
                        setText(url)
                        setSelection(length())
                        hint = url
                    }
                }
            }
        }
        binding.backwardButton.setOnClickListener {
            this.binding.webview.goBack()
        }
        binding.forwardButton.setOnClickListener {
            this.binding.webview.goForward()
        }
        binding.exportButton.apply {
            setOnClickListener {
                this@MainActivity.exportSchedule(viewModel.defaultParser) { uri ->
                    Intent(Intent.ACTION_VIEW)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .setDataAndType(uri, MIME_TYPE_CSV)
                }
            }
            setOnLongClickListener {
                this@MainActivity.exportSchedule(viewModel.defaultParser) { uri ->
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = MIME_TYPE_CSV
                            putExtra(Intent.EXTRA_STREAM, uri)
                        },
                        null
                    )
                }
                true
            }
        }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Parsing")
            .setCancelable(false)
            .setView(FrameLayout(this).apply {
                addView(
                    CircularProgressIndicator(this@MainActivity).apply {
                        isIndeterminate = true
                    },
                    FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER)
                )
                val padding = (resources.displayMetrics.density * 16).roundToInt()
                setPadding(0, padding, 0, padding)
            }).create()
        viewModel.loading.observe(this) {
            if (it == true) {
                dialog.show()
            } else {
                dialog.dismiss()
            }
        }
        viewModel.title.observe(this) {
            this.binding.searchBar.setText(it)
        }
        viewModel.progress.observe(this) {
            if (it === null) {
                binding.indicator.visibility = View.GONE
            } else {
                binding.indicator.apply {
                    visibility = View.VISIBLE
                    progress = it
                }
            }
        }
        binding.searchSuggestion.let {
            urlAdapter = UrlAdapter(this::navigateTo)
            urlAdapter.submitList(viewModel.urls)
            it.adapter = urlAdapter
            it.layoutManager = LinearLayoutManager(this)
        }
        binding.webview.let {
            this.initWebView(it)
            if (savedInstanceState !== null) {
                val state = savedInstanceState.getBundle("WebViewState")
                if (state !== null) {
                    it.restoreState(state)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_reload -> binding.webview.reload()
            R.id.action_url -> binding.webview.apply {
                getSystemService<ClipboardManager>()?.setPrimaryClip(
                    ClipData.newPlainText(title, url)
                )
            }

            R.id.action_dump -> binding.webview.evaluateJavascript(
                viewModel.defaultParser.injectJavaScript()
            ) {
                this@MainActivity.dumpSource(it.unwrapAndUnescape())
            }

            R.id.action_console -> {
                binding.webview.evaluateJavascript(INJECT_CONSOLE) {
                    Toast.makeText(this@MainActivity, it.unwrapAndUnescape(), Toast.LENGTH_SHORT)
                        .show()
                }
            }

            R.id.toggle_dock -> runSilently {
                val view = binding.dockedToolbar
                val behavior = HideViewOnScrollBehavior.from(view)
                if (behavior.isScrolledIn) {
                    behavior.slideOut(view)
                } else {
                    behavior.slideIn(view)
                }
            }

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun applyContentInsets(window: View, insets: Insets) {
        this.binding.webview.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = if (isIndicatorEnabled) 0 else insets.bottom
        }
    }

    fun navigateTo(dest: CharSequence) {
        val url = if (Patterns.WEB_URL.matcher(dest).matches()) dest.toString() else
            "https://cn.bing.com/search?q=$dest"
        this.binding.apply {
            searchBar.setText(url)
            searchView.hide()
            webview.loadUrl(url)
        }
        viewModel.url.value = url
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun initWebView(view: WebView) {
        view.webViewClient = webViewClient
        view.webChromeClient = viewModel.webChromeClient
        view.addJavascriptInterface(this.viewModel, DUMPER_NAME)
        view.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mixedContentMode = MIXED_CONTENT_ALWAYS_ALLOW
            javaScriptCanOpenWindowsAutomatically = true
            allowFileAccess = true
            allowContentAccess = true
            setSupportMultipleWindows(false)
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(view, true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val state = Bundle()
        this.binding.webview.saveState(state)
        outState.putBundle("WebViewState", state)
    }

    @MainThread
    override fun dumpSource(message: String?) {
        binding.webview.evaluateJavascript(DUMP_SOURCES) eval@{ result ->
            val uuid = result.substring(1, result.lastIndex)
            val dumps = viewModel.reports[uuid] ?: return@eval
            lifecycleScope.launch(Dispatchers.IO) {
                val file = this@MainActivity.getDumpDir().resolveUnique(uuid) { "$it.zip" }
                ZipOutputStream(file.outputStream()).use {
                    it.setComment(message ?: "$DO_NOT_CONTINUE (null)")
                    for (dump in dumps) {
                        it.putNextEntry(ZipEntry(dump.name))
                        it.write(dump.content.toByteArray(Charsets.UTF_8))
                        it.closeEntry()
                    }
                }
                val intent = Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = MIME_TYPE_ZIP
                        putExtra(Intent.EXTRA_STREAM, file.toUri(this@MainActivity))
                    },
                    null
                )
                withContext(Dispatchers.Main) {
                    startActivity(intent)
                }
            }
        }
    }

    private fun exportSchedule(parser: ScheduleParser, launch: (Uri) -> Intent) {
        this.viewModel.loading.value = true
        this.binding.webview.evaluateJavascript(parser.injectJavaScript()) eval@{ result ->
            if (result === null || result == QUOTED_DO_NOT_CONTINUE) {
                viewModel.loading.value = false
                return@eval
            }
            lifecycleScope.launch(Dispatchers.Default) {
                val sessions = parser.parseSchedule(
                    this@MainActivity,
                    result.unwrapAndUnescape()
                )
                // TODO withContext get formatter
                val formatter = viewModel.defaultFormatter
                var file: File
                withContext(Dispatchers.IO) {
                    file = this@MainActivity.getScheduleDir()
                        .resolveUnique(null, formatter::buildFileName)
                    file.outputStream().bufferedWriter(Charsets.UTF_8).use {
                        formatter.format(sessions, it)
                    }
                }
                val intent = launch(file.toUri(this@MainActivity))
                withContext(Dispatchers.Main) {
                    viewModel.loading.value = false
                    startActivity(intent)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        getSystemService<ConnectivityManager>()?.registerDefaultNetworkCallback(this.networkCallback)
    }

    override fun onPause() {
        super.onPause()
        getSystemService<ConnectivityManager>()?.unregisterNetworkCallback(this.networkCallback)
    }
}