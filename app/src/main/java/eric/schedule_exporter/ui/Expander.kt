package eric.schedule_exporter.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eric.schedule_exporter.R

val fadeInAndExpandVertically = fadeIn() + expandVertically()
val fadeOutAndShrinkVertically = fadeOut() + shrinkVertically()

@Composable
fun ExpanderIndicator(expanded: Boolean) {
    Indicator(
        icon = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
        description = stringResource(if (expanded) R.string.expander_collapse else R.string.expander_expand)
    )
}

@Composable
fun Expander(
    expanded: Boolean,
    header: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = fadeInAndExpandVertically,
    exitTransition: ExitTransition = fadeOutAndShrinkVertically,
    content: @Composable () -> Unit,
) {
    InfoBox(modifier) {
        header()
        AnimatedVisibility(
            visible = expanded,
            enter = enterTransition,
            exit = exitTransition
        ) {
            content()
        }
    }
}