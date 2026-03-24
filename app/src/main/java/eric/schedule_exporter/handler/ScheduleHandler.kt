package eric.schedule_exporter.handler

import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import androidx.collection.IntSet
import androidx.compose.runtime.Composable
import eric.schedule_exporter.data.HandlerSpec
import eric.schedule_exporter.util.Session
import eric.schedule_exporter.util.getScheduleDir
import eric.schedule_exporter.util.resolveUnique
import eric.schedule_exporter.util.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

suspend fun <T> ScheduleHandler<T>.exportSchedule(
    sessions: Map<Session, IntSet>,
    context: Context
) {
    this.export(this.convert(sessions), context)
}

suspend fun <T> ScheduleHandler<T>.dumpSchedule(
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

interface ScheduleHandler<T> {
    val type: HandlerType
    fun formatName(id: String): String
    fun convert(sessions: Map<Session, IntSet>): T
    fun serialize(sessions: T, stream: OutputStream)
    suspend fun export(data: T, context: Context)
    suspend fun loadSpec(config: HandlerSpec?): Boolean
    suspend fun saveSpec(): HandlerSpec?

    @Composable
    fun ConfigSection(onCancel: () -> Unit)

    @Composable
    fun displayName(): String
}