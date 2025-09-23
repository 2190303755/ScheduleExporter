package eric.schedule_exporter.parser.impl

import android.util.Log
import androidx.collection.MutableIntSet
import eric.schedule_exporter.parser.ParserContext
import eric.schedule_exporter.parser.ScheduleParser
import eric.schedule_exporter.parser.Session
import eric.schedule_exporter.util.DO_NOT_CONTINUE
import eric.schedule_exporter.util.toDayOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import java.util.Arrays

val REGEX_SESSIONS = Regex("\\d+")
val REGEX_WEEK_OF_DAY = Regex("星期([一二三四五六日])")
val REGEX_WEEKS = Regex("(\\d+)(?:-(\\d+))?")

private inline fun Node?.requireValue(
    throwable: () -> Throwable
): String = (this ?: throw throwable()).nodeValue()

fun MutableList<Session>.addSTUCourses(info: Element): Boolean {
    var error = false
    val iterator = info.children().iterator()
    var state: STUParserState
    while (iterator.hasNext()) {
        state = STUParserState.FINDING_SUBJECT
        try {
            val element = iterator.next()
            if ("p" != element.tag().name) continue
            val subject = element.text()
            if (!iterator.hasNext()) break
            val generic = iterator.next()
            if (!iterator.hasNext()) break
            val details = iterator.next()
            state = STUParserState.PARSING_SESSIONS
            val sessions = REGEX_SESSIONS.findAll(generic.lastChild()?.firstChild().requireValue {
                NoSuchElementException("Failed to find sessions")
            }).map { it.value.toInt() }.toMutableList().toIntArray()
            if (sessions.isEmpty()) throw IllegalStateException("Failed to parse sessions")
            val teacher = generic.run {
                state = STUParserState.PARSING_TEACHER
                val string = this.firstChild()?.firstChild().requireValue {
                    NoSuchElementException("Failed to find teacher")
                }
                if (string.startsWith("教师：")) string.substring(3) else string
            }
            val location = details.run {
                state = STUParserState.PARSING_LOCATION
                this.firstChild()?.lastChild().requireValue {
                    NoSuchElementException("Failed to find location")
                }
            }
            val weeks = MutableIntSet()
            val day = details.run {
                state = STUParserState.PARSING_TIME
                val time = this.lastChild()?.lastChild().requireValue {
                    NoSuchElementException("Failed to find time")
                }
                state = STUParserState.PARSING_WEEKS
                for (match in REGEX_WEEKS.findAll(time)) {
                    val groups = match.groupValues
                    val start = (groups.getOrNull(1) ?: continue).toInt()
                    val optional = groups.getOrNull(2)
                    val end = if (optional === null || optional.isEmpty())
                        start
                    else optional.toInt()
                    if (start < end) {
                        for (week in start..end) {
                            weeks += week
                        }
                    } else {
                        for (week in end..start) {
                            weeks += week
                        }
                    }
                }
                state = STUParserState.PARSING_DAY
                (REGEX_WEEK_OF_DAY.find(time)?.groupValues?.getOrNull(1)
                    ?: throw NoSuchElementException("Failed to find day of week"))
                    .toDayOfWeek()
            }
            Arrays.sort(sessions)
            var start = sessions[0]
            var current = start
            sessions.iterator()
            for (index in 1 until sessions.size) {
                val value = sessions[index]
                if (value < current + 2) {
                    current = value
                } else {
                    this += Session(subject, teacher, location, start, current, day, weeks)
                    start = value
                    current = start
                }
            }
            this += Session(subject, teacher, location, start, current, day, weeks)
        } catch (e: Exception) {
            error = true
            Log.w("STUParser", "Error when parsing course at $state", e)
        }
    }
    return error
}

object STUParser : ScheduleParser {
    override fun injectJavaScript() = """(() => {
const iframe = document.getElementById('Frame0');
if (iframe === null) return '${DO_NOT_CONTINUE}';
const schedule = iframe.contentDocument.getElementById('timetable');
if (schedule === null) return '${DO_NOT_CONTINUE}';
return schedule.outerHTML;
})();"""

    override suspend fun parseSchedule(context: ParserContext, message: String): Iterable<Session> {
        var error = false
        val parsed = mutableListOf<Session>()
        val row = Jsoup.parseBodyFragment(message).getElementsByTag("tr").iterator()
        if (row.hasNext()) {
            row.next() // head
            while (row.hasNext()) {
                val column = row.next().getElementsByTag("td").iterator()
                if (!column.hasNext()) continue
                val period = column.next()
                if ((period.firstChild() ?: continue).nodeValue().contains("备注")) continue
                while (column.hasNext()) {
                    if (parsed.addSTUCourses(column.next().lastElementChild() ?: continue)) {
                        error = true
                    }
                }
            }
        }
        if (error) {
            withContext(Dispatchers.Main) {
                context.dumpSource(message)
            }
        }
        val lookup = hashMapOf<String, MutableList<Session>>()
        val iterator = parsed.iterator()
        outer@ while (iterator.hasNext()) {
            val session = iterator.next()
            val typed = lookup.getOrPut(session.subject) { mutableListOf() }
            for (other in typed) {
                if (session.canMergeWith(other)) {
                    other.weeks += session.weeks
                    continue@outer
                }
            }
            typed += session
        }
        return lookup.values.flatten()
    }
}

enum class STUParserState {
    FINDING_SUBJECT,
    PARSING_TEACHER,
    PARSING_LOCATION,
    PARSING_TIME,
    PARSING_DAY,
    PARSING_WEEKS,
    PARSING_SESSIONS,
}