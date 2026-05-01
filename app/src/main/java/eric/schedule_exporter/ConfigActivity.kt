package eric.schedule_exporter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import eric.schedule_exporter.data.ParserConfig
import eric.schedule_exporter.data.parserConfig
import eric.schedule_exporter.handler.HandlerType
import eric.schedule_exporter.model.ConfigViewModel
import eric.schedule_exporter.parser.ParserType
import eric.schedule_exporter.ui.DropdownMenuChip
import eric.schedule_exporter.ui.IconButton
import eric.schedule_exporter.ui.InfoBar
import eric.schedule_exporter.ui.InfoBox
import eric.schedule_exporter.ui.TooltipBox
import eric.schedule_exporter.ui.applyInfoBoxPadding
import eric.schedule_exporter.ui.fadeInAndExpandVertically
import eric.schedule_exporter.ui.fadeOutAndShrinkVertically
import eric.schedule_exporter.ui.theme.setThemedContent
import kotlinx.coroutines.launch

class ConfigActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        this.setThemedContent {
            val viewModel = remember { ConfigViewModel(this) }
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    TopAppBar(
                        scrollBehavior = scrollBehavior,
                        title = { Text(stringResource(R.string.activity_config)) },
                        navigationIcon = {
                            TooltipBox("返回", TooltipAnchorPosition.Below) { tooltip ->
                                IconButton(Icons.AutoMirrored.Filled.ArrowBack, tooltip) {
                                    this.finish()
                                }
                            }
                        }
                    )
                }
            ) { innerPadding ->
                LazyColumn(
                    contentPadding = innerPadding + PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        val current = viewModel.parserConfig.collectAsState(ParserConfig())
                        InfoBox {
                            InfoBar(
                                title = "课程表解析器",
                                modifier = Modifier
                                    .applyInfoBoxPadding()
                                    .fillMaxWidth()
                            ) {
                                DropdownMenuChip(
                                    options = ParserType.entries,
                                    selected = current.value.parser,
                                    onSelect = { parser ->
                                        lifecycleScope.launch {
                                            this@ConfigActivity.parserConfig.updateData {
                                                ParserConfig(parser)
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    stringResource(it.text)
                                }
                            }
                        }
                    }
                    item {
                        val effective = viewModel.effectiveHandler.collectAsState(
                            HandlerType.WAKE_UP_HANDLER
                        )
                        val selected = rememberSaveable {
                            mutableStateOf<HandlerType?>(null)
                        }
                        LaunchedEffect(effective.value) {
                            selected.value = effective.value
                        }
                        InfoBox {
                            InfoBar(
                                title = "课程表处理器",
                                modifier = Modifier
                                    .applyInfoBoxPadding()
                                    .fillMaxWidth()
                            ) {
                                DropdownMenuChip(
                                    options = HandlerType.entries,
                                    selected = selected.value ?: HandlerType.WAKE_UP_HANDLER,
                                    onSelect = selected.component2(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    it.handler.displayName()
                                }
                            }
                            AnimatedContent(
                                targetState = selected.value,
                                transitionSpec = {
                                    fadeInAndExpandVertically togetherWith fadeOutAndShrinkVertically
                                },
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    it?.handler?.ConfigSection {
                                        selected.value = effective.value
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}