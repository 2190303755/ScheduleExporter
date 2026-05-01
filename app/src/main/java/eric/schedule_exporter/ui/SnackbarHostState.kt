package eric.schedule_exporter.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult

suspend inline fun SnackbarHostState.showSnackbar(
    message: String,
    actionLabel: String,
    withDismissAction: Boolean = false,
    duration: SnackbarDuration = SnackbarDuration.Short,
    onConfirm: () -> Unit
) {
    if (SnackbarResult.ActionPerformed ==
        this.showSnackbar(message, actionLabel, withDismissAction, duration)
    ) {
        onConfirm()
    }
}