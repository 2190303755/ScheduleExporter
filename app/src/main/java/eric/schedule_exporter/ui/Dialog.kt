package eric.schedule_exporter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDialogDefaults.MinHeightForTimePicker
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import eric.schedule_exporter.R
import eric.schedule_exporter.util.Moment

val DialogPadding = PaddingValues(all = 24.dp)
val IconPadding = PaddingValues(bottom = 16.dp)
val TitlePadding = PaddingValues(bottom = 16.dp)
val TextPadding = PaddingValues(bottom = 24.dp)

@Composable
fun ProvideContentColorTextStyle(
    contentColor: Color,
    textStyle: TextStyle,
    content: @Composable () -> Unit
) {
    val mergedStyle = LocalTextStyle.current.merge(textStyle)
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalTextStyle provides mergedStyle,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    neutralButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (BoxScope.() -> Unit)? = null,
    title: @Composable (BoxScope.() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
    content: @Composable BoxScope.() -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = properties
    ) {
        Surface(shape = shape, color = containerColor, tonalElevation = tonalElevation) {
            Column(modifier = Modifier.padding(DialogPadding)) {
                icon?.let {
                    CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                        Box(
                            modifier = Modifier
                                .padding(IconPadding)
                                .align(Alignment.CenterHorizontally),
                            content = icon
                        )
                    }
                }
                title?.let {
                    ProvideContentColorTextStyle(
                        contentColor = titleContentColor,
                        textStyle = MaterialTheme.typography.headlineSmall,
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(TitlePadding)
                                .align(
                                    if (icon == null) {
                                        Alignment.Start
                                    } else {
                                        Alignment.CenterHorizontally
                                    }
                                ),
                            content = title
                        )
                    }
                }
                ProvideContentColorTextStyle(
                    contentColor = textContentColor,
                    textStyle = MaterialTheme.typography.bodyMedium,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(weight = 1f, fill = false)
                            .padding(TextPadding)
                            .align(Alignment.Start),
                        content = content
                    )
                }
                Box(modifier = Modifier.align(Alignment.End)) {
                    ProvideContentColorTextStyle(
                        contentColor = MaterialTheme.colorScheme.primary,
                        textStyle = MaterialTheme.typography.labelLarge
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            neutralButton()
                            Spacer(modifier = Modifier.weight(1.0F))
                            dismissButton()
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initial: Moment,
    desiredMode: MutableState<TimePickerDisplayMode>,
    onConfirm: (Moment) -> Unit,
    onDismiss: () -> Unit,
    title: @Composable (Boolean) -> String
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true
    )
    val isPickerAvailable = LocalWindowInfo.current.containerDpSize.height > MinHeightForTimePicker
    val enablePicker by remember {
        derivedStateOf { isPickerAvailable && desiredMode.value == TimePickerDisplayMode.Picker }
    }
    TimePickerDialog(
        title = {
            /**
             * @see androidx.compose.material3.TimePickerDialogDefaults.Title
             */
            Text(
                modifier = Modifier.padding(bottom = 20.dp),
                style = MaterialTheme.typography.labelMedium,
                text = title(enablePicker),
            )
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(stringResource(R.string.action_confirm)) {
                onConfirm(Moment(timePickerState.hour, timePickerState.minute))
            }
        },
        dismissButton = {
            TextButton(stringResource(R.string.action_cancel), onClick = onDismiss)
        },
        modeToggleButton = {
            if (isPickerAvailable) {
                TimePickerDialogDefaults.DisplayModeToggle(
                    onDisplayModeChange = {
                        desiredMode.value =
                            if (desiredMode.value == TimePickerDisplayMode.Picker) {
                                TimePickerDisplayMode.Input
                            } else {
                                TimePickerDisplayMode.Picker
                            }
                    },
                    displayMode = desiredMode.value,
                )
            }
        },
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        if (enablePicker) {
            TimePicker(state = timePickerState)
        } else {
            TimeInput(state = timePickerState)
        }
    }
}