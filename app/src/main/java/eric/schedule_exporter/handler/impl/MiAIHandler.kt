package eric.schedule_exporter.handler.impl

import android.content.Context
import androidx.collection.IntSet
import androidx.lifecycle.MutableLiveData
import eric.schedule_exporter.handler.ScheduleHandler
import eric.schedule_exporter.util.JSON_CONFIG
import eric.schedule_exporter.util.Session
import eric.schedule_exporter.util.toMiAISession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.encodeToStream
import java.io.OutputStream

object MiAIHandler : ScheduleHandler {
    override fun formatName(id: String) = "${id}.json"

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(
        sessions: Map<Session, IntSet>,
        stream: OutputStream
    ) {
        JSON_CONFIG.encodeToStream(sessions.map {
            it.key.toMiAISession(it.value)
        }, stream)
    }

    override suspend fun export(
        sessions: Map<Session, IntSet>,
        context: Context,
        indicator: MutableLiveData<Boolean>
    ) {/*
        withContext(Dispatchers.IO) {
            val miai: MiAIContext
            val schedule = createMiAISchedule(miai, "")
            val detail = queryMiAISchedule(miai, schedule)
            // modify setting
            uploadMiAISessions(miai, schedule, sessions.map {
                it.key.toMiAISession(it.value)
            })
        }*/
        withContext(Dispatchers.Main) {
            indicator.value = false
        }
    }
}