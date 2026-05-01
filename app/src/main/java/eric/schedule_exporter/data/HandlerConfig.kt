package eric.schedule_exporter.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import eric.schedule_exporter.handler.HandlerType
import eric.schedule_exporter.util.MiAIContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

val DataStore<HandlerConfig>.effectiveHandler: Flow<HandlerType>
    get() = this.data.map {
        val type = it.type
        if (type.handler.loadSpec(it.specs[type])) type else HandlerType.WAKE_UP_HANDLER
    }

suspend fun Context.setScheduleHandler(type: HandlerType) {
    this.handlerConfig.updateData {
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