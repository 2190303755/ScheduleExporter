package eric.schedule_exporter.parser.impl

import androidx.collection.IntSet
import androidx.collection.MutableIntSet
import eric.schedule_exporter.parser.ScheduleParser
import eric.schedule_exporter.parser.Session
import eric.schedule_exporter.util.DO_NOT_CONTINUE
import eric.schedule_exporter.util.toDayOfWeek
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.time.DayOfWeek

val REGEX_SESSIONS = Regex("(\\d+).*?\\D(\\d+)?")
val REGEX_WEEK_OF_DAY = Regex("星期([一二三四五六日])")
val REGEX_WEEKS = Regex("(\\d+)(?:-(\\d+))?")

private val Element.teacher: String?
    get() {
        val teacher = this.firstChild()?.firstChild()?.nodeValue() ?: return null
        if (teacher.startsWith("教师：")) return teacher.substring(3)
        return teacher
    }

private val Element.sessions: IntRange?
    get() {
        val results = REGEX_SESSIONS.find(
            this.lastChild()?.firstChild()?.nodeValue() ?: return null
        )?.groupValues ?: return null
        val start = results.getOrNull(1)?.toIntOrNull() ?: return null
        return IntRange(start, results.getOrNull(2)?.toIntOrNull() ?: start)
    }

private val Element.location: String?
    get() = this.firstChild()?.lastChild()?.nodeValue()

private val Element.time: Pair<DayOfWeek, IntSet>?
    get() {
        val time = this.lastChild()?.lastChild()?.nodeValue() ?: return null
        val day = REGEX_WEEK_OF_DAY.find(time)?.groupValues?.getOrNull(1) ?: return null
        val weeks = MutableIntSet()
        for (match in REGEX_WEEKS.findAll(time)) {
            val groups = match.groupValues
            val start = groups.getOrNull(1)?.toIntOrNull() ?: continue
            val end = groups.getOrNull(2)?.toIntOrNull() ?: start
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
        return Pair(day.toDayOfWeek(), weeks)
    }

fun MutableList<Session>.addSTUCourses(info: Elements) {
    val iterator = info.iterator()
    while (iterator.hasNext()) {
        val element = iterator.next()
        if ("p" != element.tag().name) continue
        val subject = element.text()
        if (!iterator.hasNext()) break
        val generic = iterator.next()
        if (!iterator.hasNext()) break
        val details = iterator.next()
        val sessions = generic.sessions ?: continue
        val time = details.time ?: continue
        this += Session(
            subject,
            generic.teacher ?: continue,
            details.location ?: continue,
            sessions.start,
            sessions.endInclusive,
            time.first,
            time.second
        )
    }
}

object STUParser : ScheduleParser {
    override fun injectJavaScript() = """(() => {
const iframe = document.getElementById('Frame0');
if (iframe === null) return '${DO_NOT_CONTINUE}';
const schedule = iframe.contentDocument.getElementById('timetable');
if (schedule === null) return '${DO_NOT_CONTINUE}';
return schedule.outerHTML;
})();"""

    override suspend fun parseSchedule(message: String): List<Session> {
        val sessions = mutableListOf<Session>()
        val rowIterator = Jsoup.parseBodyFragment(message).getElementsByTag("tr").iterator()
        if (rowIterator.hasNext()) {
            rowIterator.next() // head
            while (rowIterator.hasNext()) {
                val columnIterator = rowIterator.next().getElementsByTag("td").iterator()
                if (!columnIterator.hasNext()) continue
                val period = columnIterator.next()
                if ((period.firstChild() ?: continue).nodeValue().contains("备注")) continue
                while (columnIterator.hasNext()) {
                    sessions.addSTUCourses(
                        columnIterator.next().lastElementChild()?.children() ?: continue
                    )
                }
            }
        }
        return sessions
    }
}