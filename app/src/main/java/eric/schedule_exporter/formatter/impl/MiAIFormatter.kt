package eric.schedule_exporter.formatter.impl

import androidx.collection.IntSet
import eric.schedule_exporter.formatter.ScheduleFormatter
import eric.schedule_exporter.util.Session
import java.io.Writer

object MiAIFormatter : ScheduleFormatter {
    override fun buildFileName(id: String) = "${id}.json"

    override fun format(
        sessions: Map<Session, IntSet>,
        writer: Writer
    ) {
        TODO("Not yet implemented")
    }
}