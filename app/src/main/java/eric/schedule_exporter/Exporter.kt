package eric.schedule_exporter

import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import androidx.collection.IntSet
import eric.schedule_exporter.model.ExporterViewModel
import eric.schedule_exporter.ui.WebViewContext
import eric.schedule_exporter.util.Session
import eric.schedule_exporter.util.getScheduleDir
import eric.schedule_exporter.util.resolveUnique
import eric.schedule_exporter.util.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

interface SessionAdapter<T> {
    fun formatName(id: String): String
    fun convert(sessions: Map<Session, IntSet>): T
    fun serialize(sessions: T, stream: OutputStream)
}

interface ExportMethod<T> : SessionAdapter<T> {
    suspend fun export(data: T, context: Context)
}

interface Exporter {
    val viewModel: ExporterViewModel
    suspend fun parseAndExport(context: WebViewContext, method: ExportMethod<*>)
}

suspend fun <T> ExportMethod<T>.exportSchedule(
    sessions: Map<Session, IntSet>,
    context: Context
) {
    this.export(this.convert(sessions), context)
}

suspend fun <T> SessionAdapter<T>.dumpSchedule(
    sessions: Map<Session, IntSet>,
    context: Context
) {
    val file = context.getScheduleDir().resolveUnique(null, this::formatName)
    val data = this.convert(sessions)
    file.outputStream().use {
        this.serialize(data, it)
    }
    val intent = Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
            type = ClipDescription.MIMETYPE_TEXT_PLAIN
            putExtra(Intent.EXTRA_STREAM, file.toUri(context))
        },
        null
    )
    withContext(Dispatchers.Main) {
        context.startActivity(intent)
    }
}