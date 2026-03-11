package eric.schedule_exporter.parser

import androidx.collection.IntSet
import eric.schedule_exporter.util.Session
import eric.schedule_exporter.util.toDayOfWeek
import org.jsoup.nodes.Node
import java.time.DayOfWeek

inline fun Node?.requireValue(
    throwable: () -> Throwable
): String = (this ?: throw throwable()).nodeValue()

val REGEX_WEEK_OF_DAY = Regex("(?:星期|周)([一二三四五六日])")

fun String.resolveDayOfWeek(): DayOfWeek? = REGEX_WEEK_OF_DAY.find(this)
    ?.groupValues
    ?.getOrNull(1)
    ?.toDayOfWeek()

enum class ParserState {
    LOCATION_SESSION,
    LOCATING_SUBJECT,
    PARSING_TEACHER,
    PARSING_LOCATION,
    PARSING_TIME,
    PARSING_DAY,
    PARSING_WEEKS,
    PARSING_PERIODS,
}

interface ScheduleParser {
    fun injectJavaScript(): String

    suspend fun parseSchedule(context: ParserContext, message: String): Map<Session, IntSet>
}