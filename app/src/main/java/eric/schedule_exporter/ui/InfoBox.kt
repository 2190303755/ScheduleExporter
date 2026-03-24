package eric.schedule_exporter.ui

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eric.schedule_exporter.util.spacedBy
import kotlin.math.max

@Composable
fun Descriptor(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    enableMarquee: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            modifier = if (enableMarquee) Modifier.basicMarquee() else Modifier
        )
    }
}

@Composable
fun Indicator(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Icon(
        imageVector = icon,
        contentDescription = description,
        modifier = modifier.size(20.dp)
    )
}

@Composable
fun InfoBar(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    enableMarquee: Boolean = false,
    spacing: Dp = 8.dp,
    icon: @Composable () -> Unit = EmptySpacer,
    indicator: @Composable () -> Unit = EmptySpacer,
    footer: @Composable () -> Unit = EmptySpacer
) {
    Layout(
        content = {
            icon()
            indicator()
            footer()
            if (description === null) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
            } else {
                Descriptor(title, description, enableMarquee = enableMarquee)
            }
        },
        modifier = modifier,
        measurePolicy = InfoRowMeasurePolicy(spacing)
    )
}

@Composable
fun InfoBox(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(),
        colors = CardDefaults.elevatedCardColors(),
        content = content
    )
}

fun Modifier.applyInfoBarPadding(
    horizontal: Dp = 16.dp,
    vertical: Dp = 12.dp,
) = this.padding(horizontal, vertical)

fun Modifier.applyInfoBoxPadding(
    padding: Dp = 16.dp
): Modifier = this.padding(padding)

@JvmInline
internal value class InfoRowMeasurePolicy(val spacing: Dp) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        require(measurables.size == 4) {
            "InfoBar expects exactly 4 children: icon, indicator, footer, descriptor. Got ${measurables.size}"
        }
        val padding = this@InfoRowMeasurePolicy.spacing.roundToPx()
        val maxWidth = constraints.maxWidth
        val maxConstraints = constraints.copy(minWidth = 0)
        val trisector = maxConstraints.copy(maxWidth = maxWidth / 3)
        val icon = measurables[0].measure(trisector)
        val indicator = measurables[1].measure(trisector)
        val iconLogicalWidth = icon.width.spacedBy { padding }
        val indicatorLogicalWidth = indicator.width.spacedBy { padding }
        val consumedWidth = iconLogicalWidth + indicatorLogicalWidth
        val inline = (icon.width + indicator.width) * 5 < maxWidth
        val footer = measurables[2].measure(
            if (inline) {
                maxConstraints.copy(maxWidth = (maxWidth - consumedWidth) / 2)
            } else {
                maxConstraints
            }
        )
        val desiredWidth = consumedWidth + footer.width.spacedBy { padding }
        if (!inline && maxWidth != Constraints.Infinity && (maxWidth - desiredWidth) * 3 < desiredWidth) {
            val description = measurables[3].measure(
                maxConstraints.copy(maxWidth = maxWidth - consumedWidth)
            )
            val infoHeight = max(
                max(icon.height, indicator.height),
                description.height
            )
            return layout(maxWidth, infoHeight + footer.height + padding) {
                icon.placeRelative(
                    x = 0,
                    y = (infoHeight - icon.height) / 2
                )
                description.placeRelative(
                    x = iconLogicalWidth,
                    y = (infoHeight - description.height) / 2
                )
                footer.placeRelative(
                    x = 0,
                    y = infoHeight + padding
                )
                indicator.placeRelative(
                    x = maxWidth - indicator.width,
                    y = (infoHeight - indicator.height) / 2
                )
            }
        }
        val description = measurables[3].measure(
            maxConstraints.copy(maxWidth = maxWidth - desiredWidth)
        )
        val maxHeight = max(
            max(icon.height, indicator.height),
            max(description.height, footer.height)
        )
        return layout(maxWidth, maxHeight) {
            icon.placeRelative(
                x = 0,
                y = (maxHeight - icon.height) / 2
            )
            description.placeRelative(
                x = iconLogicalWidth,
                y = (maxHeight - description.height) / 2
            )
            footer.placeRelative(
                x = maxWidth - footer.width - indicatorLogicalWidth,
                y = (maxHeight - footer.height) / 2
            )
            indicator.placeRelative(
                x = maxWidth - indicator.width,
                y = (maxHeight - indicator.height) / 2
            )
        }
    }
}