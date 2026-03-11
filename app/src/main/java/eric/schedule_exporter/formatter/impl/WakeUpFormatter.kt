package eric.schedule_exporter.formatter.impl

import androidx.collection.IntSet
import eric.schedule_exporter.formatter.ScheduleFormatter
import eric.schedule_exporter.util.Session
import eric.schedule_exporter.util.write
import org.apache.commons.text.StringEscapeUtils
import java.io.Writer

object WakeUpFormatter : ScheduleFormatter {
    override fun buildFileName(id: String) = "${id}.csv"

    override fun format(sessions: Map<Session, IntSet>, writer: Writer) {
        writer.write("课程名称,星期,开始节数,结束节数,老师,地点,周数")
        sessions.forEach { (session, weeks) ->
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
            writer.write(weeks.joinToString("、"))
        }
        writer.flush()
    }
}