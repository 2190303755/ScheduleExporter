@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package eric.schedule_exporter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.util.Patterns
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AppBarMenuState
import androidx.compose.material3.AppBarOverflowIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExpandedFullScreenContainedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import eric.schedule_exporter.model.ExporterViewModel
import eric.schedule_exporter.parser.ParserContext
import eric.schedule_exporter.parser.ScheduleParser
import eric.schedule_exporter.ui.AppBarWithSearch
import eric.schedule_exporter.ui.IconButton
import eric.schedule_exporter.ui.LocalTextFieldColors
import eric.schedule_exporter.ui.SimpleMenuItem
import eric.schedule_exporter.ui.TooltipBox
import eric.schedule_exporter.ui.WebView
import eric.schedule_exporter.ui.WebViewContext
import eric.schedule_exporter.ui.rememberPinnedSearchBarScrollBehavior
import eric.schedule_exporter.ui.theme.setThemedContent
import eric.schedule_exporter.util.DO_NOT_CONTINUE
import eric.schedule_exporter.util.DUMP_SOURCES
import eric.schedule_exporter.util.INJECT_CONSOLE
import eric.schedule_exporter.util.MIME_TYPE_ZIP
import eric.schedule_exporter.util.QUOTED_DO_NOT_CONTINUE
import eric.schedule_exporter.util.getDumpDir
import eric.schedule_exporter.util.resolveUnique
import eric.schedule_exporter.util.startActivity
import eric.schedule_exporter.util.toUri
import eric.schedule_exporter.util.unwrapAndUnescape
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class NewActivity : ComponentActivity(), Exporter {
    override val viewModel by viewModels<ExporterViewModel>()
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            lifecycleScope.launch(Dispatchers.Main) {
                viewModel.urlSuggestions.forEach {
                    it.onNetworkAvailable(network)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        this.getSystemService<ConnectivityManager>()
            ?.registerDefaultNetworkCallback(this.networkCallback)
    }

    override fun onPause() {
        super.onPause()
        this.getSystemService<ConnectivityManager>()
            ?.unregisterNetworkCallback(this.networkCallback)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(true)
        this.enableEdgeToEdge()
        this.setThemedContent {
            val coroutineScope = rememberCoroutineScope()
            var toolbarVisible by rememberSaveable { mutableStateOf(true) }
            var toolbarExpended by rememberSaveable { mutableStateOf(true) }
            val webviewContext = remember { WebViewContext() }
            val textFieldState = rememberTextFieldState()
            val searchBarState = rememberContainedSearchBarState()
            val suggestionState = rememberLazyListState()
            val scrollBehavior = rememberPinnedSearchBarScrollBehavior()
            val appBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(
                searchBarColors = SearchBarDefaults.containedColors(searchBarState)
            )
            val inputField = @Composable {
                SearchBarDefaults.InputField(
                    textFieldState = textFieldState,
                    searchBarState = searchBarState,
                    colors = LocalTextFieldColors.current,
                    onSearch = {
                        coroutineScope.launch {
                            if (!it.isBlank()) {
                                webviewContext.navigateTo(it)
                            }
                            searchBarState.animateToCollapsed()
                        }
                    },
                    leadingIcon = {
                        SearchBarLeadingIcon(
                            webviewContext,
                            searchBarState,
                            coroutineScope
                        )
                    },
                    trailingIcon = {
                        SearchBarTrailingIcon(
                            webviewContext,
                            searchBarState,
                            textFieldState
                        )
                    },
                )
            }
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    LaunchedEffect(searchBarState.targetValue) {
                        if (searchBarState.targetValue == SearchBarValue.Collapsed) {
                            textFieldState.setTextAndPlaceCursorAtEnd(webviewContext.location)
                        }
                    }
                    AppBarWithSearch(
                        state = searchBarState,
                        inputField = inputField,
                        scrollBehavior = scrollBehavior,
                        colors = appBarWithSearchColors,
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        AnimatedVisibility(
                            visible = searchBarState.targetValue == SearchBarValue.Collapsed,
                            enter = slideIn(
                                animationSpec = motionScheme.fastSpatialSpec(),
                                initialOffset = { IntOffset(it.width, 0) },
                            ),
                            exit = slideOut(
                                animationSpec = tween(
                                    durationMillis = 150,
                                    delayMillis = 0
                                ),
                                targetOffset = { IntOffset(it.width, 0) },
                            ),
                        ) {
                            val menuState = remember { AppBarMenuState() }
                            AppBarOverflowIndicator(menuState)
                            DropdownMenu(
                                expanded = menuState.isShowing,
                                onDismissRequest = menuState::dismiss
                            ) {
                                SimpleMenuItem(
                                    Icons.Outlined.ContentCopy,
                                    stringResource(R.string.action_url)
                                ) {
                                    webviewContext.webView?.apply {
                                        context.getSystemService<ClipboardManager>()
                                            ?.setPrimaryClip(
                                                ClipData.newPlainText(title, url)
                                            )
                                    }
                                    menuState.dismiss()
                                }
                                SimpleMenuItem(
                                    if (toolbarVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    stringResource(if (toolbarVisible) R.string.hide_dock else R.string.show_dock)
                                ) {
                                    toolbarVisible = !toolbarVisible
                                    if (toolbarVisible) {
                                        toolbarExpended = true
                                    }
                                    menuState.dismiss()
                                }
                                HorizontalDivider()
                                SimpleMenuItem(
                                    Icons.Outlined.Schedule,
                                    stringResource(R.string.activity_timing)
                                ) {
                                    this@NewActivity.startActivity<TimingActivity>()
                                    menuState.dismiss()
                                }
                                SimpleMenuItem(
                                    Icons.Outlined.Settings,
                                    stringResource(R.string.activity_config)
                                ) {
                                    this@NewActivity.startActivity<ConfigActivity>()
                                    menuState.dismiss()
                                }
                            }
                        }
                    }
                    CompositionLocalProvider(
                        LocalTextFieldColors provides appBarWithSearchColors.searchBarColors.inputFieldColors,
                    ) {
                        ExpandedFullScreenContainedSearchBar(
                            state = searchBarState,
                            inputField = inputField,
                            colors = appBarWithSearchColors.searchBarColors
                        ) {
                            LazyColumn(state = suggestionState) {
                                items(viewModel.urlSuggestions) {
                                    ListItem(
                                        headlineContent = { Text(it.name) },
                                        supportingContent = { Text(it.url) },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                textFieldState.setTextAndPlaceCursorAtEnd(it.url)
                                                coroutineScope.launch {
                                                    webviewContext.navigateTo(it.url)
                                                    searchBarState.animateToCollapsed()
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            ) { padding ->
                Box {
                    AnimatedVisibility(
                        visible = remember {
                            derivedStateOf { webviewContext.progress != null }
                        }.value,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .zIndex(1.0F)
                            .padding(top = padding.calculateTopPadding())
                    ) {
                        LinearProgressIndicator(
                            progress = animateFloatAsState(
                                targetValue = (webviewContext.progress ?: 0) / 100.0F,
                                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                            )::value,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    AnimatedVisibility(
                        visible = toolbarVisible,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = -ScreenOffset)
                            .zIndex(1.0F)
                            .padding(padding)
                    ) {
                        HorizontalFloatingToolbar(
                            expanded = toolbarExpended,
                            leadingContent = {
                                TooltipBox(stringResource(R.string.action_backward)) { tooltip ->
                                    IconButton(Icons.AutoMirrored.Filled.ArrowBack, tooltip) {
                                        webviewContext.webView?.goBack()
                                    }
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            },
                            trailingContent = {
                                Spacer(modifier = Modifier.width(4.dp))
                                TooltipBox(stringResource(R.string.action_forward)) { tooltip ->
                                    IconButton(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        tooltip
                                    ) {
                                        webviewContext.webView?.goForward()
                                    }
                                }
                            },
                            content = {
                                TooltipBox(stringResource(R.string.action_export)) {
                                    val width by animateDpAsState(if (toolbarExpended) 96.dp else 64.dp)
                                    FilledIconButton(
                                        modifier = Modifier.width(width),
                                        onClick = {
                                            lifecycleScope.launch {
                                                viewModel.effectiveHandler.first().handler.parseAndExport(
                                                    this@NewActivity,
                                                    webviewContext
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.UploadFile, it)
                                    }
                                }
                            }
                        )
                    }
                    WebView(
                        state = webviewContext,
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .floatingToolbarVerticalNestedScroll(
                                expanded = toolbarExpended,
                                onExpand = { toolbarExpended = true },
                                onCollapse = { toolbarExpended = false },
                            ),
                        onCreated = {
                            webviewContext.init(it)
                        }
                    )
                }
            }
            viewModel.currentPopup?.handler?.Popup(this, webviewContext)
        }
    }

    override suspend fun parseAndExport(
        context: WebViewContext,
        method: ExportMethod<*>
    ) {
        val parser = context.resolveParser() ?: return
        context.webView?.evaluateJavascript(parser.injectJavaScript()) eval@{ result ->
            if (result === null || result == QUOTED_DO_NOT_CONTINUE) return@eval
            lifecycleScope.launch(Dispatchers.Default) {
                method.exportSchedule(
                    parser.parseSchedule(
                        object : ParserContext {
                            override fun dumpSource(message: String?) {}
                        },
                        result.unwrapAndUnescape()
                    ),
                    this@NewActivity
                )
            }
        }
    }

    suspend fun WebViewContext.resolveParser(): ScheduleParser? {
        return viewModel.parserConfig.first().parser.resolveParser(this.webView?.url)
    }

    suspend fun WebViewContext.navigateTo(dest: String) {
        when (dest) {
            "app://csv" -> {
                val parser = this.resolveParser() ?: return
                this.webView?.evaluateJavascript(parser.injectJavaScript()) eval@{ result ->
                    if (result === null || result == QUOTED_DO_NOT_CONTINUE) return@eval
                    lifecycleScope.launch(Dispatchers.Default) {
                        viewModel.effectiveHandler.first().handler.dumpSchedule(
                            parser.parseSchedule(
                                object : ParserContext {
                                    override fun dumpSource(message: String?) {}
                                },
                                result.unwrapAndUnescape()
                            ),
                            this@NewActivity
                        )
                    }
                }
            }

            "app://dump" -> {
                this.dumpSource(null)
            }

            "app://console" -> {
                this.webView?.evaluateJavascript(INJECT_CONSOLE) { result ->
                    Toast.makeText(
                        this@NewActivity,
                        result.unwrapAndUnescape(),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            else -> {
                val url = if (Patterns.WEB_URL.matcher(dest).matches()) {
                    dest
                } else {
                    "https://cn.bing.com/search?q=$dest"
                }
                this.webView?.loadUrl(url)
                this.location = url
            }
        }
    }

    fun WebViewContext.dumpSource(message: String?) {
        this.webView?.evaluateJavascript(DUMP_SOURCES) { result ->
            val uuid = result.substring(1, result.lastIndex)
            val dumps = this.reports[uuid] ?: return@evaluateJavascript
            lifecycleScope.launch(Dispatchers.IO) {
                val file = this@NewActivity.getDumpDir().resolveUnique(uuid) { "$it.zip" }
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
                        putExtra(Intent.EXTRA_STREAM, file.toUri(this@NewActivity))
                    },
                    null
                )
                withContext(Dispatchers.Main) {
                    this@NewActivity.startActivity(intent)
                }
            }
        }
    }
}

@Composable
private fun SearchBarLeadingIcon(
    webViewContext: WebViewContext,
    searchBarState: SearchBarState,
    scope: CoroutineScope
) {
    val context = LocalContext.current
    val searching = searchBarState.targetValue == SearchBarValue.Expanded
    TooltipBox(if (searching) "Back" else "Swap UA") {
        IconButton(onClick = {
            if (!searching) {
                webViewContext.webView?.apply {
                    val agent = settings.userAgentString
                    settings.userAgentString = if (agent.contains("Mobile", false)) {
                        "Mozilla/5.0 (X11; Linux x86_64" + agent.substring(
                            agent.indexOf(')', 12)
                        ).replace("Mobile Safari", "Safari")
                    } else WebSettings.getDefaultUserAgent(context)
                    reload()
                }
            }
            scope.launch { searchBarState.animateToCollapsed() }
        }) {
            Crossfade(targetState = searching, animationSpec = tween(300)) { state ->
                Icon(
                    imageVector = if (state) {
                        Icons.AutoMirrored.Filled.ArrowBack
                    } else {
                        Icons.Filled.Computer
                    },
                    contentDescription = it
                )
            }
        }
    }
}

@Composable
private fun SearchBarTrailingIcon(
    webViewContext: WebViewContext,
    searchBarState: SearchBarState,
    textFieldState: TextFieldState
) {
    val searching = searchBarState.targetValue == SearchBarValue.Expanded
    TooltipBox(stringResource(if (searching) R.string.clear_text else R.string.action_reload)) {
        IconButton(onClick = {
            if (searching) {
                textFieldState.clearText()
            } else {
                webViewContext.webView?.reload()
            }
        }) {
            Crossfade(targetState = searching, animationSpec = tween(300)) { state ->
                Icon(
                    imageVector = if (state) Icons.Filled.Clear else Icons.Filled.Refresh,
                    contentDescription = it
                )
            }
        }
    }
}

