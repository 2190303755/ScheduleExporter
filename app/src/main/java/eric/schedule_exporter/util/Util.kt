package eric.schedule_exporter.util

import androidx.collection.MutableIntSet
import org.apache.commons.text.StringEscapeUtils

inline fun runSilently(action: () -> Unit) = try {
    action()
} catch (_: Exception) {
}

inline fun <reified T> Iterator<T>.skip(): Boolean {
    if (this.hasNext()) {
        this.next()
        return true
    }
    return false
}

fun String.unwrapAndUnescape(): String = StringEscapeUtils.unescapeEcmaScript(
    this.substring(1, this.lastIndex)
)

fun MutableIntSet.addRangeClosed(from: Int, to: Int) {
    for (value in minOf(from, to)..maxOf(from, to)) {
        this += value
    }
}