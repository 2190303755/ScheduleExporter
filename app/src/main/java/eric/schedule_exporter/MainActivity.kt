package eric.schedule_exporter

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.activity.OnBackPressedCallback
import androidx.annotation.MainThread
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
import eric.schedule_exporter.util.DUMPER_NAME
import eric.schedule_exporter.util.DUMP_SOURCES
import eric.schedule_exporter.util.MIME_TYPE_CSV
import eric.schedule_exporter.util.MIME_TYPE_ZIP
import eric.schedule_exporter.util.WRAPPED_DO_NOT_CONTINUE
import eric.schedule_exporter.util.encodeAsCSV
import eric.schedule_exporter.util.getDumpDir
import eric.schedule_exporter.util.getScheduleDir
import eric.schedule_exporter.util.runSilently
import eric.schedule_exporter.util.toUri
import eric.schedule_exporter.util.unescapeScriptResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
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

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    private val goBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            binding.webview.goBack()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                addTransitionListener { view, old, neo ->
                    callback.isEnabled = neo === SearchView.TransitionState.SHOWN
                }
                editText.setOnEditorActionListener { view, id, event ->
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
                this@MainActivity.exportSchedule { uri ->
                    Intent(Intent.ACTION_VIEW)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .setDataAndType(uri, MIME_TYPE_CSV)
                }
            }
            setOnLongClickListener {
                this@MainActivity.exportSchedule { uri ->
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
            val adapter = UrlAdapter(this::navigateTo)
            adapter.submitList(
                listOf(
                    UrlAdapter.Item(
                        "汕头大学",
                        "https://sso.stu.edu.cn/login?service=http%3A%2F%2Fjw.stu.edu.cn%2F"
                    ),
                    UrlAdapter.Item(
                        "GitHub",
                        "https://github.com/2190303755/ScheduleExporter"
                    )
                )
            )
            it.adapter = adapter
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
                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                    ClipData.newPlainText(title, url)
                )
            }

            R.id.action_dump -> binding.webview.evaluateJavascript(
                viewModel.parser.injectJavaScript()
            ) {
                this@MainActivity.dumpSource(it)
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

    suspend fun exportSchedule(message: String): Uri {
        val sessions = this.viewModel.parser.parseSchedule(this, message)
        var file: File
        withContext(Dispatchers.IO) {
            val dir = this@MainActivity.getScheduleDir()
            dir.mkdirs()
            do {
                file = File(dir, "${UUID.randomUUID()}.csv")
            } while (file.exists())
            file.outputStream().bufferedWriter(Charsets.UTF_8).use(sessions::encodeAsCSV)
        }
        return file.toUri(this@MainActivity)
    }

    @MainThread
    override fun dumpSource(message: String?) {
        binding.webview.evaluateJavascript(DUMP_SOURCES) eval@{
            val uuid = it.substring(1, it.lastIndex)
            val dumps = viewModel.reports[uuid] ?: return@eval
            lifecycleScope.launch(Dispatchers.IO) {
                val dir = getDumpDir()
                dir.mkdirs()
                var file = File(dir, "$it.zip")
                while (file.exists()) {
                    file = File(dir, "${UUID.randomUUID()}.zip")
                }
                ZipOutputStream(file.outputStream()).use { zip ->
                    for (dump in dumps) {
                        zip.putNextEntry(ZipEntry(dump.name))
                        zip.write(dump.content.toByteArray(Charsets.UTF_8))
                        zip.closeEntry()
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

    private inline fun exportSchedule(crossinline launch: (Uri) -> Intent) {
        this.viewModel.loading.value = true
        this.binding.webview.evaluateJavascript(
            this.viewModel.parser.injectJavaScript()
        ) eval@{
            if (it === null || it == WRAPPED_DO_NOT_CONTINUE) {
                viewModel.loading.value = false
                return@eval
            }
            lifecycleScope.launch(Dispatchers.Default) {
                val intent = launch(exportSchedule(it.unescapeScriptResult()))
                withContext(Dispatchers.Main) {
                    viewModel.loading.value = false
                    startActivity(intent)
                }
            }
        }
    }
}