package eric.schedule_exporter.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import eric.schedule_exporter.parser.Session
import org.apache.commons.text.StringEscapeUtils
import java.io.File
import java.io.Writer
import java.time.DayOfWeek

inline fun <reified T : Writer> T.write(char: Char) {
    this.write(char.code)
}

inline fun runSilently(action: () -> Unit) = try {
    action()
} catch (_: Exception) {
}

inline fun <reified T : Context> File.toUri(context: T): Uri =
    FileProvider.getUriForFile(context, "eric.schedule_exporter.file_provider", this)

inline fun <reified T : Context> T.getScheduleDir() =
    File(this.externalCacheDir ?: this.cacheDir, "schedules")

inline fun <reified T : Context> T.getDumpDir() =
    File(this.externalCacheDir ?: this.cacheDir, "dumps")

fun Iterable<Session>.encodeAsCSV(writer: Writer) {
    writer.write("课程名称,星期,开始节数,结束节数,老师,地点,周数")
    for (session in this) {
        writer.write(System.lineSeparator())
        writer.write(StringEscapeUtils.escapeCsv(session.subject))
        writer.write(charArrayOf(',', '0' + session.dayOfWeek.value, ','))
        writer.write(session.start.toString())
        writer.write(',')
        writer.write(session.end.toString())
        writer.write(',')
        writer.write(StringEscapeUtils.escapeCsv(session.teacher))
        writer.write(',')
        writer.write(StringEscapeUtils.escapeCsv(session.location))
        writer.write(',')
        writer.write(session.weeks.joinToString("、"))
    }
    writer.flush()
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