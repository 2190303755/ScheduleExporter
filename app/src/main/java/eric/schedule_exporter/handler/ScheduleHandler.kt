package eric.schedule_exporter.handler

import android.content.Context
import androidx.collection.IntSet
import androidx.lifecycle.MutableLiveData
import eric.schedule_exporter.util.Session
import eric.schedule_exporter.util.getScheduleDir
import eric.schedule_exporter.util.resolveUnique
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

suspend fun Map<Session, IntSet>.serializeToFile(context: Context, handler: ScheduleHandler): File {
    var file: File
    withContext(Dispatchers.IO) {
        file = context.getScheduleDir()
            .resolveUnique(null, handler::formatName)
        file.outputStream().use {
            handler.serialize(this@serializeToFile, it)
        }
    }
    return file
}

interface ScheduleHandler {
    fun formatName(id: String): String
    fun serialize(sessions: Map<Session, IntSet>, stream: OutputStream)
    suspend fun export(
        sessions: Map<Session, IntSet>,
        context: Context,
        indicator: MutableLiveData<Boolean>
    )
}