package eric.schedule_exporter.util

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.view.ViewGroup.LayoutParams
import androidx.collection.MutableIntSet
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.ui.unit.Constraints
import org.apache.commons.text.StringEscapeUtils
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun Boolean.asFloat(): Float = if (this) 1.0F else 0.0F

inline fun <reified T> Iterator<T>.skip(): Boolean {
    if (this.hasNext()) {
        this.next()
        return true
    }
    return false
}

inline fun Int.spacedBy(space: () -> Int): Int = if (0 == this) 0 else this + space()

inline fun <reified T : Activity> Context.startActivity() {
    this.startActivity(Intent(this, T::class.java))
}

@OptIn(ExperimentalUuidApi::class)
fun randomUniqueString() = Uuid.random().toHexString().uppercase()

fun ClipData.collectText(): String? {
    val list = mutableListOf<CharSequence>()
    for (i in 0 until this.itemCount) {
        this.getItemAt(i).text?.let {
            list += it
        }
    }
    return if (list.isEmpty()) null else list.joinToString("\n")
}

fun String.unwrapAndUnescape(): String = StringEscapeUtils.unescapeEcmaScript(
    this.substring(1, this.lastIndex)
)

fun MutableIntSet.addRangeClosed(from: Int, to: Int) {
    (minOf(from, to)..maxOf(from, to)).forEach(this::add)
}

inline fun BoxWithConstraintsScope.resolveLayoutParams(
    isFixed: Constraints.() -> Boolean
): Int = if (this.constraints.isFixed()) {
    LayoutParams.MATCH_PARENT
} else {
    LayoutParams.WRAP_CONTENT
}