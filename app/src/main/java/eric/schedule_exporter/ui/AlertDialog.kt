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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties


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