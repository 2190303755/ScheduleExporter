package eric.schedule_exporter.parser.impl

import android.util.Log
import androidx.collection.IntSet
import androidx.collection.MutableIntSet
import eric.schedule_exporter.parser.ParserContext
import eric.schedule_exporter.parser.ParserState
import eric.schedule_exporter.parser.ScheduleParser
import eric.schedule_exporter.parser.requireValue
import eric.schedule_exporter.parser.resolveDayOfWeek
import eric.schedule_exporter.util.QUOTED_DO_NOT_CONTINUE
import eric.schedule_exporter.util.Session
import eric.schedule_exporter.util.addRangeClosed
import eric.schedule_exporter.util.skip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.Arrays

private val REGEX_PERIODS = Regex("\\d+")
private val REGEX_WEEKS = Regex("(\\d+)(?:-(\\d+))?")

private fun MutableMap<Session, MutableIntSet>.addCourses(info: Element): Boolean {
    var error = false
    val iterator = info.children().iterator()
    var state: ParserState
    while (iterator.hasNext()) {
        state = ParserState.LOCATING_SUBJECT
        try {
            val element = iterator.next()
            if ("p" != element.tag().name) continue
            val subject = element.text()
            if (!iterator.hasNext()) break
            val generic = iterator.next()
            if (!iterator.hasNext()) break
            val details = iterator.next()
            state = ParserState.PARSING_PERIODS
            val periods =
                REGEX_PERIODS.findAll(generic.lastChild()?.firstChild().requireValue {
                    NoSuchElementException("Failed to locate periods")
                }).map { it.value.toInt() }.toMutableList().toIntArray()
            if (periods.isEmpty()) throw IllegalStateException("Failed to parse periods")
            val teacher = generic.run {
                state = ParserState.PARSING_TEACHER
                val string = this.firstChild()?.firstChild().requireValue {
                    NoSuchElementException("Failed to find teacher")
                }
                if (string.startsWith("教师：")) string.substring(3) else string
            }
            val location = details.run {
                state = ParserState.PARSING_LOCATION
                this.firstChild()?.lastChild().requireValue {
                    NoSuchElementException("Failed to find location")
                }
            }
            val weeks = MutableIntSet()
            val day = details.run {
                state = ParserState.PARSING_TIME
                val time = this.lastChild()?.lastChild().requireValue {
                    NoSuchElementException("Failed to find time")
                }
                state = ParserState.PARSING_WEEKS
                for (match in REGEX_WEEKS.findAll(time)) {
                    val groups = match.groupValues
                    val start = (groups.getOrNull(1) ?: continue).toInt()
                    val optional = groups.getOrNull(2)
                    val end = if (optional === null || optional.isEmpty()) {
                        start
                    } else {
                        optional.toInt()
                    }
                    weeks.addRangeClosed(start, end)
                }
                state = ParserState.PARSING_DAY
                time.resolveDayOfWeek()
                    ?: throw NoSuchElementException("Failed to resolve day of week")
            }
            Arrays.sort(periods)
            var start = periods[0]
            var current = start
            for (index in 1 until periods.size) {
                val value = periods[index]
                if (value < current + 2) {
                    current = value
                } else {
                    this.getOrPut(
                        Session(subject, teacher, location, start, current, day),
                        ::MutableIntSet
                    ) += weeks
                    start = value
                    current = start
                }
            }
            this.getOrPut(
                Session(subject, teacher, location, start, current, day),
                ::MutableIntSet
            ) += weeks
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
if (iframe === null) return ${QUOTED_DO_NOT_CONTINUE};
const schedule = iframe.contentDocument.getElementById('timetable');
return schedule ? schedule.outerHTML : ${QUOTED_DO_NOT_CONTINUE};
})();"""

    override suspend fun parseSchedule(
        context: ParserContext,
        message: String
    ): Map<Session, IntSet> {
        var error = false
        val sessions = hashMapOf<Session, MutableIntSet>()
        val row = Jsoup.parseBodyFragment(message).getElementsByTag("tr").iterator()
        if (row.skip()) { // skip header
            while (row.hasNext()) {
                val column = row.next().getElementsByTag("td").iterator()
                if (!column.hasNext()) continue
                val period = column.next()
                if ((period.firstChild() ?: continue).nodeValue().contains("备注")) continue
                while (column.hasNext()) {
                    if (sessions.addCourses(column.next().lastElementChild() ?: continue)) {
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
        return sessions
    }
}