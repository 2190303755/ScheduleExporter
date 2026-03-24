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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eric.schedule_exporter.ScheduleExporterApplication.Companion.SCHEDULE_PERIODS
import eric.schedule_exporter.data.addPeriod
import eric.schedule_exporter.data.removePeriod
import eric.schedule_exporter.data.setAllDurations
import eric.schedule_exporter.data.updatePeriod
import eric.schedule_exporter.ui.Expander
import eric.schedule_exporter.ui.ExpanderIndicator
import eric.schedule_exporter.ui.Indicator
import eric.schedule_exporter.ui.InfoBar
import eric.schedule_exporter.ui.InfoBox
import eric.schedule_exporter.ui.TooltipBox
import eric.schedule_exporter.ui.applyInfoBarPadding
import eric.schedule_exporter.ui.applyInfoBoxPadding
import eric.schedule_exporter.ui.theme.setThemedContent
import eric.schedule_exporter.util.Moment
import eric.schedule_exporter.util.Period
import eric.schedule_exporter.util.PeriodHolder
import kotlinx.coroutines.launch

class TimingActivity : ComponentActivity() {
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
                        title = { Text(stringResource(R.string.activity_timing)) },
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
                val scope = rememberCoroutineScope()
                val lazyColumnState = rememberLazyListState()
                LazyColumn(
                    state = lazyColumnState,
                    contentPadding = innerPadding + PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(
                        SCHEDULE_PERIODS,
                        key = { _, holder -> holder.id },
                        itemContent = LazyItemScope::PeriodItem
                    )
                    item("") {
                        var showDialog by remember { mutableStateOf(false) }
                        InfoBox(Modifier.animateItem()) {
                            InfoBar(
                                title = "添加时间段",
                                modifier = Modifier
                                    .clickable {
                                        showDialog = true
                                    }
                                    .fillMaxWidth()
                                    .applyInfoBoxPadding(),
                                indicator = {
                                    Indicator(icon = Icons.Filled.Add)
                                }
                            )
                        }
                        if (showDialog) {
                            TimeDialog(
                                title = "设置开始时间",
                                initial = Moment(8, 0),
                                onConfirm = { moment ->
                                    scope.launch {
                                        this@TimingActivity.addPeriod(Period(moment, 45))
                                        lazyColumnState.animateScrollToItem(SCHEDULE_PERIODS.size)
                                    }
                                    showDialog = false
                                },
                                onDismiss = { showDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PeriodEditor(
    title: String,
    value: String,
    onCLick: () -> Unit
) {
    HorizontalDivider()
    InfoBar(
        title = title,
        modifier = Modifier
            .clickable(onClick = onCLick)
            .applyInfoBarPadding(),
        indicator = { Indicator(icon = Icons.Filled.Edit, description = "编辑") }
    ) {
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun LazyItemScope.PeriodItem(index: Int, holder: PeriodHolder) {
    val period = holder.period
    val end = period.start + period.duration
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showStartTimeDialog by remember { mutableStateOf(false) }
    var showEndTimeDialog by remember { mutableStateOf(false) }
    var showDurationDialog by remember { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    Expander(
        expanded = expanded,
        header = {
            InfoBar(
                title = "第 ${index + 1} 节",
                description = "${period.start} - $end",
                indicator = { ExpanderIndicator(expanded) },
                modifier = Modifier
                    .clickable {
                        expanded = !expanded
                    }
                    .fillMaxWidth()
                    .applyInfoBoxPadding()
            )
        },
        modifier = Modifier.animateItem()
    ) {
        Column {
            PeriodEditor("开始时间", period.start.toString()) {
                showStartTimeDialog = true
            }
            PeriodEditor("结束时间", end.toString()) {
                showEndTimeDialog = true
            }
            PeriodEditor("持续时间", "${period.duration} 分钟") {
                showDurationDialog = true
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
                                scope.launch {
                                    context.removePeriod(index)
                                }
                            }
                        ) {
                            Toast.makeText(context, "删除时间段需要长按", Toast.LENGTH_SHORT).show()
                        }
                        .applyInfoBarPadding(),
                    indicator = { Indicator(icon = Icons.Filled.Delete, description = "删除") }
                )
            }
        }
    }
    if (showStartTimeDialog) {
        TimeDialog(
            title = "设置开始时间",
            initial = period.start,
            onConfirm = { moment ->
                scope.launch {
                    context.updatePeriod(
                        index,
                        holder.copy {
                            it.copy(start = moment, duration = end - moment)
                        }
                    )
                }
                showStartTimeDialog = false
            },
            onDismiss = { showStartTimeDialog = false }
        )
    }

    if (showEndTimeDialog) {
        TimeDialog(
            title = "设置结束时间",
            initial = end,
            onConfirm = { moment ->
                scope.launch {
                    context.updatePeriod(
                        index,
                        holder.copy {
                            it.copy(duration = moment - it.start)
                        }
                    )
                }
                showEndTimeDialog = false
            },
            onDismiss = { showEndTimeDialog = false }
        )
    }

    if (showDurationDialog) {
        DurationDialog(
            initialDuration = period.duration,
            context = context,
            onConfirm = { duration ->
                scope.launch {
                    context.updatePeriod(index, holder.copy {
                        it.copy(duration = duration)
                    })
                }
                showDurationDialog = false
            },
            onDismiss = { showDurationDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(
    title: String,
    initial: Moment,
    onConfirm: (Moment) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TimePicker(
                state = timePickerState
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(Moment(timePickerState.hour, timePickerState.minute))
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun DurationDialog(
    initialDuration: Int,
    context: android.content.Context,
    onConfirm: (Int) -> Unit,
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
            TextButton(
                onClick = {
                    val d = duration.toIntOrNull() ?: 0
                    if (d > 0) {
                        if (setAllState) {
                            scope.launch {
                                context.setAllDurations(d)
                            }
                        } else {
                            onConfirm(d)
                        }
                    }
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
