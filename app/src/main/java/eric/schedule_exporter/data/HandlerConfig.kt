package eric.schedule_exporter.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import eric.schedule_exporter.ScheduleExporterApplication.Companion.SCHEDULE_HANDLER
import eric.schedule_exporter.handler.HandlerType
import eric.schedule_exporter.handler.ScheduleHandler
import eric.schedule_exporter.util.MiAIContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream
import java.util.EnumMap


val Context.handlerConfig: DataStore<HandlerConfig> by dataStore(
    fileName = "HandlerConfig.json",
    serializer = HandlerConfig
)

suspend fun Context.setScheduleHandler(handler: ScheduleHandler<*>) {
    withContext(Dispatchers.Main) {
        SCHEDULE_HANDLER = handler
    }
    withContext(NonCancellable + Dispatchers.IO) {
        handlerConfig.updateData {
            val type = SCHEDULE_HANDLER.type
            val spec = type.handler.saveSpec()
            if (spec === null) {
                it.copy(type = type)
            } else {
                val specs = EnumMap<HandlerType, HandlerSpec>(HandlerType::class.java)
                specs.putAll(it.specs)
                specs[type] = spec
                HandlerConfig(type, specs)
            }
        }
    }
}

@Serializable
data class HandlerConfig(
    val type: HandlerType = HandlerType.WAKE_UP_HANDLER,
    val specs: Map<HandlerType, HandlerSpec> = EnumMap(HandlerType::class.java)
) {
    companion object SerializerImpl : Serializer<HandlerConfig> {
        override val defaultValue: HandlerConfig = HandlerConfig()

        @OptIn(ExperimentalSerializationApi::class)
        override suspend fun readFrom(input: InputStream): HandlerConfig = try {
            Json.decodeFromStream<HandlerConfig>(input)
        } catch (serialization: SerializationException) {
            throw CorruptionException("Unable to read HandlerConfig", serialization)
        }

        @OptIn(ExperimentalSerializationApi::class)
        override suspend fun writeTo(t: HandlerConfig, output: OutputStream) {
            Json.encodeToStream<HandlerConfig>(t, output)
        }
    }
}

@Serializable
sealed interface HandlerSpec

@Serializable
@SerialName("miai")
@JvmInline
value class MiAISpec(val miai: MiAIContext) : HandlerSpec