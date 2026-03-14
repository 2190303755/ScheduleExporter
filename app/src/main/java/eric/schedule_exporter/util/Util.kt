package eric.schedule_exporter.util

import android.view.View
import androidx.collection.MutableIntSet
import com.google.android.material.behavior.HideViewOnScrollBehavior
import org.apache.commons.text.StringEscapeUtils
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

inline fun <reified T> Iterator<T>.skip(): Boolean {
    if (this.hasNext()) {
        this.next()
        return true
    }
    return false
}

inline fun <reified T : View> T.withHideBehavior(action: HideViewOnScrollBehavior<T>.() -> Unit) {
    val behavior = try {
        HideViewOnScrollBehavior.from(this)
    } catch (_: Exception) {
        return
    }
    behavior.action()
}

@OptIn(ExperimentalUuidApi::class)
fun randomUniqueString() = Uuid.random().toHexString().uppercase()

fun String.unwrapAndUnescape(): String = StringEscapeUtils.unescapeEcmaScript(
    this.substring(1, this.lastIndex)
)

fun MutableIntSet.addRangeClosed(from: Int, to: Int) {
    for (value in minOf(from, to)..maxOf(from, to)) {
        this += value
    }
}