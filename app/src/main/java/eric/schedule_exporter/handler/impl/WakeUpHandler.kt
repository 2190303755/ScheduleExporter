package eric.schedule_exporter.handler.impl

import android.content.Context
import android.content.Intent
import androidx.collection.IntSet
import androidx.lifecycle.MutableLiveData
import eric.schedule_exporter.handler.ScheduleHandler
import eric.schedule_exporter.handler.serializeToFile
import eric.schedule_exporter.util.MIME_TYPE_CSV
import eric.schedule_exporter.util.Session
import eric.schedule_exporter.util.toUri
import eric.schedule_exporter.util.write
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.text.StringEscapeUtils
import java.io.OutputStream

object WakeUpHandler : ScheduleHandler {
    override fun formatName(id: String) = "${id}.csv"

    override fun serialize(sessions: Map<Session, IntSet>, stream: OutputStream) {
        val writer = stream.bufferedWriter()
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

    override suspend fun export(
        sessions: Map<Session, IntSet>,
        context: Context,
        indicator: MutableLiveData<Boolean>
    ) {
        val intent = Intent(Intent.ACTION_VIEW)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .setDataAndType(
                sessions.serializeToFile(context, this).toUri(context),
                MIME_TYPE_CSV
            )
        withContext(Dispatchers.Main) {
            indicator.value = false
            context.startActivity(intent)
        }
    }
}