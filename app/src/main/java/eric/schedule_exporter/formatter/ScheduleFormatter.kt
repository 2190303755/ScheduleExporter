package eric.schedule_exporter.formatter

import androidx.collection.IntSet
import eric.schedule_exporter.util.Session
import java.io.Writer

interface ScheduleFormatter {
    fun buildFileName(id: String): String
    fun format(sessions: Map<Session, IntSet>, writer: Writer)
}