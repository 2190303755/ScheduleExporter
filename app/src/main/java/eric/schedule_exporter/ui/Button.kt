package eric.schedule_exporter.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun TextButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(text = text)
    }
}

@Composable
fun IconButton(
    icon: ImageVector,
    tooltip: String? = null,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(imageVector = icon, contentDescription = tooltip)
    }
}