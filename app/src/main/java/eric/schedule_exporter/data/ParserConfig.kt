package eric.schedule_exporter.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import eric.schedule_exporter.parser.ParserType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream

val Context.parserConfig: DataStore<ParserConfig> by dataStore(
    fileName = "ParserConfig.json",
    serializer = ParserConfig
)

@JvmInline
@Serializable
value class ParserConfig(val parser: ParserType = ParserType.UNSPECIFIED) {
    companion object SerializerImpl : Serializer<ParserConfig> {
        override val defaultValue: ParserConfig = ParserConfig()

        @OptIn(ExperimentalSerializationApi::class)
        override suspend fun readFrom(input: InputStream): ParserConfig = try {
            Json.decodeFromStream<ParserConfig>(input)
        } catch (serialization: SerializationException) {
            throw CorruptionException("Unable to read ParserConfig", serialization)
        }

        @OptIn(ExperimentalSerializationApi::class)
        override suspend fun writeTo(t: ParserConfig, output: OutputStream) {
            Json.encodeToStream<ParserConfig>(t, output)
        }
    }
}