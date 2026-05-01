package eric.schedule_exporter

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.lifecycleScope
import eric.schedule_exporter.model.TimingViewModel
import eric.schedule_exporter.ui.Expander
import eric.schedule_exporter.ui.ExpanderIndicator
import eric.schedule_exporter.ui.IconButton
import eric.schedule_exporter.ui.Indicator
import eric.schedule_exporter.ui.InfoBar
import eric.schedule_exporter.ui.TextButton
import eric.schedule_exporter.ui.TimePickerDialog
import eric.schedule_exporter.ui.TooltipBox
import eric.schedule_exporter.ui.applyInfoBarPadding
import eric.schedule_exporter.ui.applyInfoBoxPadding
import eric.schedule_exporter.ui.showSnackbar
import eric.schedule_exporter.ui.theme.setThemedContent
import eric.schedule_exporter.util.Moment
import eric.schedule_exporter.util.PeriodRecord
import eric.schedule_exporter.util.end
import kotlinx.coroutines.launch

sealed interface Edit {
    val index: Int

    object Append : Edit {
        override val index: Int
            get() = -1
    }

    @JvmInline
    value class Start(override val index: Int) : Edit

    @JvmInline
    value class End(override val index: Int) : Edit

    @JvmInline
    value class Duration(override val index: Int) : Edit
}

fun Edit.encode(): Long = (this.index.toLong() shl 32) or when (this) {
    Edit.Append -> 1L
    is Edit.Start -> 2L
    is Edit.End -> 3L
    is Edit.Duration -> 4L
}

val EditSaver: Saver<MutableState<Edit?>, Long> = Saver(
    save = { state -> state.value?.encode() ?: 0L },
    restore = { value ->
        mutableStateOf(
            when (value and 0xFFFFFFFF) {
                1L -> Edit.Append
                2L -> Edit.Start((value shr 32).toInt())
                3L -> Edit.End((value shr 32).toInt())
                4L -> Edit.Duration((value shr 32).toInt())
                else -> null
            }
        )
    }
)

class TimingActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        this.setThemedContent {
            var edit by rememberSaveable(saver = EditSaver) { mutableStateOf(null) }
            val desiredPickerMode = remember { mutableStateOf(TimePickerDisplayMode.Picker) }
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            val snackbarHostState = remember { SnackbarHostState() }
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    TopAppBar(
                        scrollBehavior = scrollBehavior,
                        title = { Text(stringResource(R.string.activity_timing)) },
                        navigationIcon = {
                            TooltipBox("返回", TooltipAnchorPosition.Below) { tooltip ->
                                IconButton(Icons.AutoMirrored.Filled.ArrowBack, tooltip) {
                                    this.finish()
                                }
                            }
                        }
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { edit = Edit.Append },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "添加时间段")
                    }
                }
            ) { innerPadding ->
                val viewModel = remember { TimingViewModel(this) }
                val periods by viewModel.periods.collectAsState(emptyList())
                val lazyColumnState = rememberLazyListState()
                LazyColumn(
                    state = lazyColumnState,
                    contentPadding = innerPadding + PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        periods,
                        key = { _, period -> period.id }
                    ) { index, period ->
                        val end = period.end
                        var expanded by rememberSaveable { mutableStateOf(false) }
                        Expander(
                            expanded = expanded,
                            header = {
                                InfoBar(
                                    title = "第 ${index + 1} 节",
                                    description = "${period.start} - $end",
                                    indicator = { ExpanderIndicator(expanded) },
                                    modifier = Modifier
                                        .clickable { expanded = !expanded }
                                        .fillMaxWidth()
                                        .applyInfoBoxPadding()
                                )
                            },
                            modifier = Modifier.animateItem()
                        ) {
                            Column {
                                PeriodEditor("开始时间", period.start.toString()) {
                                    edit = Edit.Start(index)
                                }
                                PeriodEditor("结束时间", end.toString()) {
                                    edit = Edit.End(index)
                                }
                                PeriodEditor("持续时间", "${period.duration} 分钟") {
                                    edit = Edit.Duration(index)
                                }
                                HorizontalDivider()
                                CompositionLocalProvider(
                                    LocalContentColor provides MaterialTheme.colorScheme.error
                                ) {
                                    InfoBar(
                                        title = "删除时间段",
                                        description = "长按以删除时间段",
                                        modifier = Modifier
                                            .combinedClickable(
                                                onLongClick = {
                                                    lifecycleScope.launch {
                                                        viewModel.removePeriod(index)
                                                        snackbarHostState.currentSnackbarData?.dismiss()
                                                        snackbarHostState.showSnackbar(
                                                            message = "已删除时间段",
                                                            actionLabel = "撤销",
                                                            duration = SnackbarDuration.Long
                                                        ) {
                                                            viewModel.insertPeriod(index, period)
                                                        }
                                                    }
                                                }
                                            ) {
                                                Toast.makeText(
                                                    this@TimingActivity,
                                                    "删除时间段需要长按",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            .applyInfoBarPadding(),
                                        indicator = {
                                            Indicator(
                                                icon = Icons.Filled.Delete,
                                                description = "删除"
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                when (val action = edit) {
                    Edit.Append -> {
                        TimePickerDialog(
                            initial = Moment(8, 0),
                            desiredMode = desiredPickerMode,
                            onConfirm = { moment ->
                                lifecycleScope.launch {
                                    viewModel.appendPeriod(PeriodRecord(moment, 45))
                                }
                                edit = null
                            },
                            onDismiss = { edit = null }
                        ) {
                            "设置开始时间"
                        }
                    }

                    is Edit.Start -> {
                        val period = periods.getOrNull(action.index)
                        if (period !== null) {
                            TimePickerDialog(
                                initial = period.start,
                                desiredMode = desiredPickerMode,
                                onConfirm = { moment ->
                                    lifecycleScope.launch {
                                        viewModel.updatePeriod(
                                            action.index,
                                            period.copy(
                                                start = moment,
                                                duration = period.end - moment
                                            )
                                        )
                                    }
                                    edit = null
                                },
                                onDismiss = { edit = null }
                            ) {
                                "设置开始时间"
                            }
                        }
                    }

                    is Edit.End -> {
                        val period = periods.getOrNull(action.index)
                        if (period !== null) {
                            TimePickerDialog(
                                initial = period.end,
                                desiredMode = desiredPickerMode,
                                onConfirm = { moment ->
                                    lifecycleScope.launch {
                                        viewModel.updatePeriod(
                                            action.index,
                                            period.copy(duration = moment - period.start)
                                        )
                                    }
                                    edit = null
                                },
                                onDismiss = { edit = null }
                            ) {
                                "设置结束时间"
                            }
                        }
                    }

                    is Edit.Duration -> {
                        val period = periods.getOrNull(action.index)
                        if (period !== null) {
                            DurationDialog(
                                initialDuration = period.duration,
                                onConfirm = { unify, duration ->
                                    lifecycleScope.launch {
                                        if (unify) {
                                            viewModel.unifyDuration(duration)
                                        } else {
                                            viewModel.updatePeriod(
                                                action.index,
                                                period.copy(duration = duration)
                                            )
                                        }
                                        edit = null
                                    }
                                },
                                onDismiss = { edit = null }
                            )
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}

@Composable
fun PeriodEditor(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    HorizontalDivider()
    InfoBar(
        title = title,
        modifier = Modifier
            .clickable(onClick = onClick)
            .applyInfoBarPadding(),
        indicator = { Indicator(icon = Icons.Filled.Edit, description = "编辑") }
    ) {
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun DurationDialog(
    initialDuration: Int,
    onConfirm: (Boolean, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var duration by remember { mutableStateOf(initialDuration.toString()) }
    val (setAllState, onSetAllStateChange) = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置持续时间") },
        text = {
            Column {
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("分钟") },
                    modifier = Modifier.fillMaxWidth()
                )
                val interactionSource = remember { MutableInteractionSource() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .toggleable(
                            value = setAllState,
                            onValueChange = onSetAllStateChange,
                            role = Role.Checkbox,
                            indication = null,
                            interactionSource = interactionSource,
                        ),
                ) {
                    Checkbox(
                        checked = setAllState,
                        interactionSource = interactionSource,
                        onCheckedChange = onSetAllStateChange,
                    )
                    Text(
                        text = "统一设置所有时间段的持续时间",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(stringResource(R.string.action_confirm)) {
                val duration = duration.toIntOrNull() ?: 0
                if (duration > 0) {
                    onConfirm(setAllState, duration)
                }
            }
        },
        dismissButton = {
            TextButton(stringResource(R.string.action_cancel), onClick = onDismiss)
        },
        properties = DialogProperties(dismissOnClickOutside = false)
    )
}
