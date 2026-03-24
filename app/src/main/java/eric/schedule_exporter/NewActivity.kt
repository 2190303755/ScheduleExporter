@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package eric.schedule_exporter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.AppBarWithSearch
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
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import eric.schedule_exporter.ScheduleExporterApplication.Companion.SCHEDULE_HANDLER
import eric.schedule_exporter.ScheduleExporterApplication.Companion.SCHEDULE_PARSER
import eric.schedule_exporter.ScheduleExporterApplication.Companion.URL_SUGGESTIONS
import eric.schedule_exporter.handler.dumpSchedule
import eric.schedule_exporter.handler.exportSchedule
import eric.schedule_exporter.parser.ParserContext
import eric.schedule_exporter.parser.ScheduleParser
import eric.schedule_exporter.ui.SimpleMenuItem
import eric.schedule_exporter.ui.TooltipBox
import eric.schedule_exporter.ui.WebView
import eric.schedule_exporter.ui.WebViewContext
import eric.schedule_exporter.ui.theme.setThemedContent
import eric.schedule_exporter.util.DO_NOT_CONTINUE
import eric.schedule_exporter.util.DUMP_SOURCES
import eric.schedule_exporter.util.MIME_TYPE_ZIP
import eric.schedule_exporter.util.QUOTED_DO_NOT_CONTINUE
import eric.schedule_exporter.util.getDumpDir
import eric.schedule_exporter.util.resolveUnique
import eric.schedule_exporter.util.startActivity
import eric.schedule_exporter.util.toUri
import eric.schedule_exporter.util.unwrapAndUnescape
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class NewActivity : ComponentActivity() {
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            this@NewActivity.lifecycleScope.launch(Dispatchers.Main) {
                URL_SUGGESTIONS.forEach {
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
            val scope = rememberCoroutineScope()
            var toolbarExpended by rememberSaveable { mutableStateOf(true) }
            val webViewContext = remember { WebViewContext() }
            val textFieldState = rememberTextFieldState()
            val searchBarState = rememberContainedSearchBarState()
            val suggestionState = rememberLazyListState()
            val appBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(
                searchBarColors = SearchBarDefaults.containedColors(searchBarState)
            )
            val inputField = @Composable {
                SearchBarDefaults.InputField(
                    textFieldState = textFieldState,
                    searchBarState = searchBarState,
                    colors = appBarWithSearchColors.searchBarColors.inputFieldColors,
                    onSearch = {
                        if (!it.isBlank()) {
                            webViewContext.navigateTo(it, this@NewActivity)
                        }
                        scope.launch {
                            searchBarState.animateToCollapsed()
                        }
                    },
                    placeholder = {
                        Text(modifier = Modifier.clearAndSetSemantics {}, text = "Search")
                    },
                    leadingIcon = { SearchBarLeadingIcon(webViewContext, searchBarState, scope) },
                    trailingIcon = { SearchBarTrailingIcon(webViewContext) },
                )
            }
            Scaffold(
                topBar = {
                    AppBarWithSearch(
                        state = searchBarState,
                        colors = appBarWithSearchColors,
                        inputField = inputField,
                        actions = {
                            AnimatedVisibility(
                                visible = searchBarState.targetValue == SearchBarValue.Collapsed,
                                enter = slideIn(
                                    animationSpec = motionScheme.fastSpatialSpec(),
                                    initialOffset = { IntOffset(it.width, 0) },
                                ),
                                exit = slideOut(
                                    animationSpec = tween(durationMillis = 150, delayMillis = 0),
                                    targetOffset = { IntOffset(it.width, 0) },
                                ),
                            ) {
                                var expanded by remember { mutableStateOf(false) }
                                TooltipBox("More") {
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(
                                            imageVector = Icons.Filled.MoreVert,
                                            contentDescription = it
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    SimpleMenuItem(
                                        Icons.Outlined.ContentCopy,
                                        stringResource(R.string.action_url)
                                    ) {
                                        webViewContext.webView?.apply {
                                            context.getSystemService<ClipboardManager>()
                                                ?.setPrimaryClip(
                                                    ClipData.newPlainText(title, url)
                                                )
                                        }
                                    }
                                    SimpleMenuItem(
                                        Icons.Outlined.Title,
                                        stringResource(R.string.toggle_dock)
                                    ) {
                                        toolbarExpended = !toolbarExpended
                                    }
                                    SimpleMenuItem(
                                        Icons.Filled.Refresh,
                                        stringResource(R.string.action_reload)
                                    ) {
                                        webViewContext.webView?.reload()
                                    }
                                    HorizontalDivider()
                                    SimpleMenuItem(
                                        Icons.Outlined.Schedule,
                                        stringResource(R.string.activity_timing)
                                    ) {
                                        this@NewActivity.startActivity<TimingActivity>()
                                    }
                                    SimpleMenuItem(
                                        Icons.Outlined.Settings,
                                        stringResource(R.string.activity_config)
                                    ) {
                                        this@NewActivity.startActivity<ConfigActivity>()
                                    }
                                }
                            }
                        },
                    )
                    ExpandedFullScreenContainedSearchBar(
                        state = searchBarState,
                        inputField = inputField,
                        colors = appBarWithSearchColors.searchBarColors,
                    ) {
                        LazyColumn(state = suggestionState) {
                            items(URL_SUGGESTIONS) {
                                ListItem(
                                    headlineContent = { Text(it.name) },
                                    supportingContent = { Text(it.url) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            textFieldState.setTextAndPlaceCursorAtEnd(it.url)
                                            scope.launch {
                                                searchBarState.animateToCollapsed()
                                                webViewContext.navigateTo(it.url, this@NewActivity)
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    HorizontalFloatingToolbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = -ScreenOffset)
                            .zIndex(1f),
                        expanded = toolbarExpended,
                        leadingContent = {
                            TooltipBox(stringResource(R.string.action_backward)) {
                                IconButton(onClick = { webViewContext.webView?.goBack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, it)
                                }
                            }
                        },
                        trailingContent = {
                            TooltipBox(stringResource(R.string.action_forward)) {
                                IconButton(onClick = { webViewContext.webView?.goForward() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, it)
                                }
                            }
                        },
                        content = {
                            val width by animateDpAsState(if (toolbarExpended) 96.dp else 64.dp)
                            TooltipBox(stringResource(R.string.action_export)) {
                                FilledIconButton(
                                    modifier = Modifier
                                        .width(width),
                                    onClick = click@{
                                        val parser = webViewContext.getParser() ?: return@click
                                        webViewContext.webView?.evaluateJavascript(parser.injectJavaScript()) eval@{ result ->
                                            if (result === null || result == QUOTED_DO_NOT_CONTINUE) {
                                                return@eval
                                            }
                                            lifecycleScope.launch(Dispatchers.Default) {
                                                SCHEDULE_HANDLER.exportSchedule(
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
                                ) {
                                    Icon(Icons.Filled.UploadFile, it)
                                }
                            }
                        },
                    )
                    WebView(
                        state = webViewContext,
                        modifier = Modifier
                            .fillMaxSize()
                            .floatingToolbarVerticalNestedScroll(
                                expanded = toolbarExpended,
                                onExpand = { toolbarExpended = true },
                                onCollapse = { toolbarExpended = false },
                            ),
                        onCreated = {
                            webViewContext.init(it)
                        }
                    )
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
private fun SearchBarTrailingIcon(webViewContext: WebViewContext) = TooltipBox("Refresh") {
    IconButton(onClick = { webViewContext.webView?.reload() }) {
        Icon(imageVector = Icons.Filled.Refresh, contentDescription = it)
    }
}

fun WebViewContext.navigateTo(dest: String, context: ComponentActivity) {
    when (dest) {
        "app://csv" -> {
            val parser = this.getParser() ?: return
            this.webView?.evaluateJavascript(parser.injectJavaScript()) eval@{ result ->
                if (result === null || result == QUOTED_DO_NOT_CONTINUE) {
                    return@eval
                }
                context.lifecycleScope.launch(Dispatchers.Default) {
                    SCHEDULE_HANDLER.dumpSchedule(
                        parser.parseSchedule(
                            object : ParserContext {
                                override fun dumpSource(message: String?) {}
                            },
                            result.unwrapAndUnescape()
                        ),
                        context
                    )
                }
            }
        }

        "app://dump" -> {
            dumpSource(context, null)
        }

        "app://console" -> {
            webView?.evaluateJavascript(eric.schedule_exporter.util.INJECT_CONSOLE) { result ->
                android.widget.Toast.makeText(
                    context,
                    result.unwrapAndUnescape(),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }

        else -> {
            val url = if (android.util.Patterns.WEB_URL.matcher(dest).matches()) {
                dest
            } else {
                "https://cn.bing.com/search?q=$dest"
            }
            this.webView?.loadUrl(url)
            this.destination = url
            return
        }
    }
}


fun WebViewContext.dumpSource(context: ComponentActivity, message: String?) {
    webView?.evaluateJavascript(DUMP_SOURCES) { result ->
        val uuid = result.substring(1, result.lastIndex)
        val dumps = reports[uuid] ?: return@evaluateJavascript
        context.lifecycleScope.launch(Dispatchers.IO) {
            val file = context.getDumpDir().resolveUnique(uuid) { "$it.zip" }
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
                    putExtra(Intent.EXTRA_STREAM, file.toUri(context))
                },
                null
            )
            withContext(Dispatchers.Main) {
                context.startActivity(intent)
            }
        }
    }
}

fun WebViewContext.getParser(): ScheduleParser? {
    return SCHEDULE_PARSER.resolveParser(webView?.url)
}
