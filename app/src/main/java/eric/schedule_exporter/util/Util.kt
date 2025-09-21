package eric.schedule_exporter.util

import android.content.Context
import eric.schedule_exporter.parser.Session
import org.apache.commons.text.StringEscapeUtils
import java.io.File
import java.io.Writer
import java.time.DayOfWeek

fun String.escapeCSV(): String = if (this.any(",\"\r\n"::contains))
    "\"" + this.replace("\"", "\"\"") + "\""
else this

inline fun <reified T : Writer> T.write(char: Char) {
    this.write(char.code)
}

inline fun runSilently(action: () -> Unit) = try {
    action()
} catch (_: Exception) {
}

inline fun <reified T : Context> T.getScheduleDir() =
    File(this.cacheDir, "schedules")

fun Iterable<Session>.encodeAsCSV(writer: Writer) {
    writer.write("课程名称,星期,开始节数,结束节数,老师,地点,周数")
    for (session in this) {
        writer.write(System.lineSeparator())
        writer.write(session.subject.escapeCSV())
        writer.write(charArrayOf(',', '0' + session.dayOfWeek.value, ','))
        writer.write(session.start.toString())
        writer.write(',')
        writer.write(session.end.toString())
        writer.write(',')
        writer.write(session.teacher.escapeCSV())
        writer.write(',')
        writer.write(session.location.escapeCSV())
        writer.write(',')
        writer.write(session.weeks.joinToString("、"))
    }
    writer.flush()
}

fun Int.toDayOfWeek(): DayOfWeek = try {
    DayOfWeek.of(this)
} catch (_: Exception) {
    DayOfWeek.SUNDAY
}

fun String.toDayOfWeek() = when (this) {
    "一" -> DayOfWeek.MONDAY
    "二" -> DayOfWeek.TUESDAY
    "三" -> DayOfWeek.WEDNESDAY
    "四" -> DayOfWeek.THURSDAY
    "五" -> DayOfWeek.FRIDAY
    "六" -> DayOfWeek.SATURDAY
    else -> DayOfWeek.SUNDAY
}

fun String.unescapeScriptResult(): String = StringEscapeUtils.unescapeEcmaScript(
    this.substring(1, this.lastIndex)
)