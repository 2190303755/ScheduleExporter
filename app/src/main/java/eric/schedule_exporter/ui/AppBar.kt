package eric.schedule_exporter.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.AppBarWithSearchColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import eric.schedule_exporter.util.asFloat
import kotlin.math.absoluteValue

val UnspecifiedTextFieldColors: TextFieldColors = TextFieldColors(
    focusedTextColor = Color.Unspecified,
    unfocusedTextColor = Color.Unspecified,
    disabledTextColor = Color.Unspecified,
    errorTextColor = Color.Unspecified,
    focusedContainerColor = Color.Unspecified,
    unfocusedContainerColor = Color.Unspecified,
    disabledContainerColor = Color.Unspecified,
    errorContainerColor = Color.Unspecified,
    cursorColor = Color.Unspecified,
    errorCursorColor = Color.Unspecified,
    textSelectionColors = TextSelectionColors(Color.Unspecified, Color.Unspecified),
    focusedIndicatorColor = Color.Unspecified,
    unfocusedIndicatorColor = Color.Unspecified,
    disabledIndicatorColor = Color.Unspecified,
    errorIndicatorColor = Color.Unspecified,
    focusedLeadingIconColor = Color.Unspecified,
    unfocusedLeadingIconColor = Color.Unspecified,
    disabledLeadingIconColor = Color.Unspecified,
    errorLeadingIconColor = Color.Unspecified,
    focusedTrailingIconColor = Color.Unspecified,
    unfocusedTrailingIconColor = Color.Unspecified,
    disabledTrailingIconColor = Color.Unspecified,
    errorTrailingIconColor = Color.Unspecified,
    focusedLabelColor = Color.Unspecified,
    unfocusedLabelColor = Color.Unspecified,
    disabledLabelColor = Color.Unspecified,
    errorLabelColor = Color.Unspecified,
    focusedPlaceholderColor = Color.Unspecified,
    unfocusedPlaceholderColor = Color.Unspecified,
    disabledPlaceholderColor = Color.Unspecified,
    errorPlaceholderColor = Color.Unspecified,
    focusedSupportingTextColor = Color.Unspecified,
    unfocusedSupportingTextColor = Color.Unspecified,
    disabledSupportingTextColor = Color.Unspecified,
    errorSupportingTextColor = Color.Unspecified,
    focusedPrefixColor = Color.Unspecified,
    unfocusedPrefixColor = Color.Unspecified,
    disabledPrefixColor = Color.Unspecified,
    errorPrefixColor = Color.Unspecified,
    focusedSuffixColor = Color.Unspecified,
    unfocusedSuffixColor = Color.Unspecified,
    disabledSuffixColor = Color.Unspecified,
    errorSuffixColor = Color.Unspecified,
)
val LocalTextFieldColors = compositionLocalOf(structuralEqualityPolicy()) {
    UnspecifiedTextFieldColors
}

val SearchBarAsTopBarPadding = 8.dp
val AppBarWithSearchHorizontalPadding = 4.dp
val AppBarWithSearchVerticalPadding = 4.dp
val SearchBarMinWidth = 360.dp
val SearchBarMaxWidth = 720.dp

@OptIn(ExperimentalMaterial3Api::class)
fun AppBarWithSearchColors.appBarContainerColor(colorTransitionFraction: Float): Color {
    if (scrolledAppBarContainerColor == Color.Unspecified) {
        return appBarContainerColor
    }
    return lerp(
        appBarContainerColor,
        scrolledAppBarContainerColor,
        FastOutLinearInEasing.transform(colorTransitionFraction),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
fun AppBarWithSearchColors.searchBarContainerColor(colorTransitionFraction: Float): Color {
    if (scrolledSearchBarContainerColor == Color.Unspecified) {
        return searchBarColors.containerColor
    }
    return lerp(
        searchBarColors.containerColor,
        scrolledSearchBarContainerColor,
        FastOutLinearInEasing.transform(colorTransitionFraction),
    )
}

@Composable
fun rememberPinnedSearchBarScrollBehavior(
    initialScrollOffsetLimit: Float = -Float.MAX_VALUE,
    initialContentOffset: Float = 0.0F,
    isScrollingContentAtStart: () -> Boolean = { true },
    canScroll: () -> Boolean = { true }
): PinnedSearchBarScrollBehavior = rememberSaveable(
    canScroll,
    saver = listSaver(
        save = { listOf(it.scrollOffsetLimit, it.contentOffset) },
        restore = {
            PinnedSearchBarScrollBehavior(
                initialScrollOffsetLimit = it[0],
                initialContentOffset = it[1],
                canScroll = canScroll,
                isScrollingContentAtStart = isScrollingContentAtStart
            )
        }
    ),
) {
    PinnedSearchBarScrollBehavior(
        initialScrollOffsetLimit = initialScrollOffsetLimit,
        initialContentOffset = initialContentOffset,
        canScroll = canScroll,
        isScrollingContentAtStart = isScrollingContentAtStart
    )
}

/**
 * @see androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior
 */
@OptIn(ExperimentalMaterial3Api::class)
class PinnedSearchBarScrollBehavior(
    initialScrollOffsetLimit: Float,
    initialContentOffset: Float,
    val canScroll: () -> Boolean = { true },
    val isScrollingContentAtStart: () -> Boolean = { true },
) : SearchBarScrollBehavior {
    override var scrollOffset: Float
        get() = 0.0F
        set(_) {}
    override var scrollOffsetLimit by mutableFloatStateOf(initialScrollOffsetLimit)
    private var _contentOffset by mutableFloatStateOf(initialContentOffset)
    override var contentOffset: Float
        @FrequentlyChangingValue get() = _contentOffset
        set(offset) {
            _contentOffset = offset
        }

    val overlappedFraction: Float
        get() = if (!isScrollingContentAtStart() && contentOffset == 0.0F) {
            1.0F
        } else if (scrollOffsetLimit != 0.0F) {
            1.0F - ((scrollOffsetLimit + contentOffset.absoluteValue).coerceIn(
                minimumValue = scrollOffsetLimit,
                maximumValue = 0.0F,
            ) / scrollOffsetLimit)
        } else {
            0.0F
        }

    override fun Modifier.searchBarScrollBehavior(): Modifier = this

    override val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            if (canScroll()) {
                contentOffset += consumed.y
            }
            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (available.y > 0f) {
                contentOffset = 0f
            }
            return super.onPostFling(consumed, available)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBarWithSearch(
    state: SearchBarState,
    inputField: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: PinnedSearchBarScrollBehavior = rememberPinnedSearchBarScrollBehavior(),
    navigationIcon: @Composable (() -> Unit)? = null,
    shape: Shape = SearchBarDefaults.inputFieldShape,
    colors: AppBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(),
    contentPadding: PaddingValues = SearchBarDefaults.AppBarContentPadding,
    windowInsets: WindowInsets = SearchBarDefaults.windowInsets,
    actions: @Composable (RowScope.() -> Unit)? = null,
) {
    val (targetAppBarContainerColor, targetSearchBarContainerColor) = remember(
        colors,
        scrollBehavior
    ) {
        derivedStateOf {
            val colorTransitionFraction = (scrollBehavior.overlappedFraction > 0.01F).asFloat()
            colors.appBarContainerColor(
                colorTransitionFraction
            ) to colors.searchBarContainerColor(
                colorTransitionFraction
            )
        }
    }.value

    val appBarContainerColor by animateColorAsState(
        targetAppBarContainerColor,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )

    val searchBarColors by remember(targetSearchBarContainerColor) {
        derivedStateOf {
            colors.searchBarColors.copy(containerColor = targetSearchBarContainerColor)
        }
    }
    Box(
        modifier
            .fillMaxWidth()
            .drawBehind {
                val color = appBarContainerColor
                if (color != Color.Unspecified) {
                    drawRect(color = color)
                }
            }
            .semantics { isTraversalGroup = true }
            .windowInsetsPadding(windowInsets)
            .onSizeChanged { size ->
                val offset = size.height.toFloat() - scrollBehavior.scrollOffset
                scrollBehavior.scrollOffsetLimit = -offset
            }
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationIcon?.let {
                Box(Modifier.padding(start = AppBarWithSearchHorizontalPadding)) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.appBarNavigationIconColor,
                        content = it,
                    )
                }
            }
            Box(Modifier.weight(1f)) {
                SearchBar(
                    state = state,
                    inputField = inputField,
                    modifier =
                        Modifier
                            .padding(
                                horizontal = SearchBarAsTopBarPadding,
                                vertical = AppBarWithSearchVerticalPadding,
                            )
                            .widthIn(min = SearchBarMinWidth, max = SearchBarMaxWidth)
                            .align(Alignment.Center),
                    shape = shape,
                    colors = searchBarColors
                )
            }
            actions?.let {
                CompositionLocalProvider(
                    LocalContentColor provides colors.appBarActionIconColor,
                ) {
                    Row(
                        modifier = Modifier.padding(end = AppBarWithSearchHorizontalPadding),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        content = it
                    )
                }
            }
        }
    }
}