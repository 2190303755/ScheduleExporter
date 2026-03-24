package eric.schedule_exporter.ui

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SimpleMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(text) },
        onClick = onClick,
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null)
        }
    )
}

@Stable
val ItemContentPadding: PaddingValues =
    PaddingValues(horizontal = 12.dp, vertical = 0.dp) // 16 - 4

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> DropdownMenuChip(
    selected: MutableState<T>,
    options: List<T>,
    modifier: Modifier = Modifier,
    namer: @Composable (T) -> String,
) {
    val (expanded, onExpandedChange) = remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        val minSize = LocalMinimumInteractiveComponentSize.current
        AssistChip(
            onClick = {},
            label = {
                Text(text = namer(selected.value))
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = modifier
                .defaultMinSize(minHeight = minSize, minWidth = minSize)
                .menuAnchor(
                    ExposedDropdownMenuAnchorType.PrimaryNotEditable
                )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            containerColor = MenuDefaults.groupStandardContainerColor,
            shape = MenuDefaults.standaloneGroupShape,
        ) {
            val size = options.size
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(index, size),
                    text = {
                        Text(
                            text = namer(option),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.basicMarquee()
                        )
                    },
                    selected = option == selected.value,
                    onClick = {
                        selected.value = option
                        onExpandedChange(false)
                    },
                    selectedLeadingIcon = {
                        Icon(
                            Icons.Filled.Check,
                            modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                            contentDescription = null,
                        )
                    },
                    contentPadding = ItemContentPadding,
                )
            }
        }
    }
}