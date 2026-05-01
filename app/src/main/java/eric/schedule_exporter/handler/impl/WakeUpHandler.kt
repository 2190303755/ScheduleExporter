package eric.schedule_exporter.handler.impl

import android.content.Context
import android.content.Intent
import androidx.collection.IntSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eric.schedule_exporter.ExportMethod
import eric.schedule_exporter.Exporter
import eric.schedule_exporter.R
import eric.schedule_exporter.data.HandlerSpec
import eric.schedule_exporter.data.setScheduleHandler
import eric.schedule_exporter.handler.HandlerType
import eric.schedule_exporter.handler.ScheduleHandler
import eric.schedule_exporter.ui.WebViewContext
import eric.schedule_exporter.util.MIME_TYPE_CSV
import eric.schedule_exporter.util.Session
import eric.schedule_exporter.util.getScheduleDir
import eric.schedule_exporter.util.resolveUnique
import eric.schedule_exporter.util.toUri
import eric.schedule_exporter.util.write
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.text.StringEscapeUtils
import java.io.OutputStream

object WakeUpHandler : ScheduleHandler<Map<Session, IntSet>>, ExportMethod<Map<Session, IntSet>> {
    override val type: HandlerType
        get() = HandlerType.WAKE_UP_HANDLER

    override fun formatName(id: String) = "${id}.csv"
    override fun convert(sessions: Map<Session, IntSet>) = sessions
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

    override suspend fun export(data: Map<Session, IntSet>, context: Context) {
        val file = context.getScheduleDir().resolveUnique(null, this::formatName)
        file.outputStream().use {
            this.serialize(data, it)
        }
        val intent = Intent(Intent.ACTION_VIEW)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .setDataAndType(
                file.toUri(context),
                MIME_TYPE_CSV
            )
        withContext(Dispatchers.Main) {
            context.startActivity(intent)
        }
    }

    override suspend fun loadSpec(config: HandlerSpec?): Boolean = true

    override suspend fun saveSpec(): HandlerSpec? = null

    @Composable
    override fun displayName() = stringResource(R.string.app_wakeup)

    @Composable
    override fun ConfigSection(onCancel: () -> Unit) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.setScheduleHandler(HandlerType.WAKE_UP_HANDLER)
        }
    }

    override suspend fun parseAndExport(exporter: Exporter, webViewContext: WebViewContext) {
        exporter.parseAndExport(webViewContext, this)
    }
}