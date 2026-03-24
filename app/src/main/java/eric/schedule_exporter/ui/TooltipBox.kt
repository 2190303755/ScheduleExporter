package eric.schedule_exporter.ui

import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable

@Composable
fun TooltipBox(
    tooltip: String,
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Above,
    content: @Composable (String) -> Unit
) = TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = positioning),
    tooltip = { PlainTooltip { Text(tooltip) } },
    state = rememberTooltipState(),
) {
    content(tooltip)
}