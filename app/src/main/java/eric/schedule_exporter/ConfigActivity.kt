package eric.schedule_exporter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eric.schedule_exporter.ScheduleExporterApplication.Companion.SCHEDULE_HANDLER
import eric.schedule_exporter.ScheduleExporterApplication.Companion.SCHEDULE_PARSER
import eric.schedule_exporter.data.setScheduleParser
import eric.schedule_exporter.handler.HandlerType
import eric.schedule_exporter.parser.ParserType
import eric.schedule_exporter.ui.DropdownMenuChip
import eric.schedule_exporter.ui.InfoBar
import eric.schedule_exporter.ui.InfoBox
import eric.schedule_exporter.ui.TooltipBox
import eric.schedule_exporter.ui.applyInfoBoxPadding
import eric.schedule_exporter.ui.fadeInAndExpandVertically
import eric.schedule_exporter.ui.fadeOutAndShrinkVertically
import eric.schedule_exporter.ui.theme.setThemedContent

class ConfigActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        this.setThemedContent {
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    TopAppBar(
                        scrollBehavior = scrollBehavior,
                        title = { Text(stringResource(R.string.activity_config)) },
                        navigationIcon = {
                            TooltipBox("返回", TooltipAnchorPosition.Below) {
                                IconButton(onClick = { this.finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, it)
                                }
                            }
                        }
                    )
                }
            ) { innerPadding ->
                LazyColumn(
                    contentPadding = innerPadding + PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        ParserTypeSection()
                    }
                    item {
                        HandlerTypeSection()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ParserTypeSection() {
    InfoBox {
        val context = LocalContext.current
        val selected = remember { mutableStateOf(SCHEDULE_PARSER) }
        LaunchedEffect(selected.value) {
            context.setScheduleParser(selected.value)
        }
        InfoBar(
            title = "课程表解析器",
            modifier = Modifier
                .applyInfoBoxPadding()
                .fillMaxWidth()
        ) {
            DropdownMenuChip(
                selected = selected,
                options = ParserType.entries,
                modifier = Modifier.fillMaxWidth()
            ) {
                stringResource(it.text)
            }
        }
    }
}

@Composable
fun HandlerTypeSection() {
    val selected = rememberSaveable { mutableStateOf(SCHEDULE_HANDLER.type) }
    InfoBox(Modifier.fillMaxWidth()) {
        InfoBar(
            title = "课程表处理器",
            modifier = Modifier
                .applyInfoBoxPadding()
                .fillMaxWidth()
        ) {
            DropdownMenuChip(
                selected = selected,
                options = HandlerType.entries,
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
                it.handler.ConfigSection {
                    selected.value = SCHEDULE_HANDLER.type
                }
            }
        }
    }
}
