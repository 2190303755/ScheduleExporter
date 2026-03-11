package eric.schedule_exporter.parser.impl

import androidx.collection.IntIntPair
import androidx.collection.IntSet
import androidx.collection.MutableIntSet
import eric.schedule_exporter.parser.ParserContext
import eric.schedule_exporter.parser.ScheduleParser
import eric.schedule_exporter.parser.resolveDayOfWeek
import eric.schedule_exporter.util.QUOTED_DO_NOT_CONTINUE
import eric.schedule_exporter.util.Session
import eric.schedule_exporter.util.addRangeClosed
import eric.schedule_exporter.util.skip
import org.jsoup.Jsoup
import java.time.DayOfWeek

private val REGEX_PERIODS = Regex("第(\\d+)[-,]?(\\d*)节|(\\d+)节/周")
private val REGEX_WEEKS = Regex("(\\d+)[-,]?(\\d*)周")
private val REGEX_SKIP_WEEK = Regex("\\|[单双]周")
private fun String.resolveWeeks(): IntIntPair {
    val groups = REGEX_WEEKS.find(this)?.groupValues
        ?: throw Exception()
    val left = groups.getOrNull(1)?.toInt()
        ?: throw Exception()
    val right = groups.getOrNull(2)
    return IntIntPair(left, if (right.isNullOrEmpty()) left else right.toInt())
}

private fun String.resolvePeriods(current: Int): IntIntPair {
    val groups = REGEX_PERIODS.find(this)?.groupValues
        ?: return IntIntPair(current, current)
    val left = groups.getOrNull(1)
    if (left.isNullOrEmpty()) {
        return IntIntPair(current, current - 1 + (groups.getOrNull(3)?.toInt() ?: 1))
    }
    val start = left.toInt()
    val right = groups.getOrNull(2)
    return IntIntPair(start, if (right.isNullOrEmpty()) start else right.toInt())
}

object BUUParser : ScheduleParser {
    override fun injectJavaScript() = """(() => {
const iframe = document.getElementById('iframeautoheight');
if (iframe === null) return ${QUOTED_DO_NOT_CONTINUE};
const schedules = iframe.contentDocument.getElementsByClassName('schedule');
return schedules.length ? schedules[0].outerHTML : ${QUOTED_DO_NOT_CONTINUE};
})();"""

    override suspend fun parseSchedule(
        context: ParserContext,
        message: String
    ): Map<Session, IntSet> {
        val sessions = hashMapOf<Session, MutableIntSet>()
        val row = Jsoup.parseBodyFragment(message).getElementsByTag("tr").iterator()
        if (row.skip() && row.skip()) { // skip header and blank
            val bottom = IntArray(7)
            var period = 0
            while (row.hasNext()) {
                var day = 0
                val column = row.next().getElementsByTag("td").iterator()
                when (++period) {
                    1, 6, 10 -> column.skip()
                }
                if (column.skip()) {
                    while (column.hasNext()) {
                        while (period <= bottom[day]) {
                            if (++day >= bottom.size) throw Exception()
                        }
                        val cell = column.next()
                        val end = period - 1 + (cell.attr("rowspan").toIntOrNull() ?: 1)
                        bottom[day] = end
                        val info = cell.textNodes()
                        var index = 0
                        while (index + 3 < info.size) {
                            val subject = info[index]
                            if (subject.isBlank) {
                                ++index
                                continue
                            }
                            val time = info[index + 1].text()
                            val weeks = time.resolveWeeks()
                            val periods = time.resolvePeriods(period)
                            val repeat = sessions.getOrPut(
                                Session(
                                    subject.text(),
                                    info[index + 2].text(),
                                    info[index + 3].text(),
                                    periods.first,
                                    periods.second,
                                    time.resolveDayOfWeek() ?: DayOfWeek.of(day + 1)
                                ),
                                ::MutableIntSet
                            )
                            if (time.matches(REGEX_SKIP_WEEK)) {
                                val left = weeks.first
                                val right = weeks.second
                                for (week in IntProgression.fromClosedRange(
                                    minOf(left, right),
                                    maxOf(left, right),
                                    2
                                )) {
                                    repeat += week
                                }
                            } else {
                                repeat.addRangeClosed(weeks.first, weeks.second)
                            }
                            index += 4
                        }
                    }
                }
            }
        }
        return sessions
    }
}