package eric.schedule_exporter.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import eric.schedule_exporter.util.Moment
import eric.schedule_exporter.util.UniquePeriod
import eric.schedule_exporter.util.UniquePeriodBuilderScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream

val Context.periodConfig: DataStore<List<UniquePeriod>> by dataStore(
    fileName = "PeriodConfig.json",
    serializer = PeriodConfigSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler {
        PeriodConfigSerializer.defaultValue
    }
)

object PeriodConfigSerializer : Serializer<List<UniquePeriod>> {
    override val defaultValue: List<UniquePeriod> = with(UniquePeriodBuilderScope()) {
        mutableListOf(
            Moment(8, 0) lastFor 45,
            Moment(8, 50) lastFor 45,
            Moment(9, 55) lastFor 45,
            Moment(10, 45) lastFor 45,
            Moment(11, 35) lastFor 45,
            Moment(13, 0) lastFor 45,
            Moment(13, 50) lastFor 45,
            Moment(14, 50) lastFor 45,
            Moment(15, 40) lastFor 45,
            Moment(16, 40) lastFor 45,
            Moment(17, 30) lastFor 45,
            Moment(18, 20) lastFor 45,
            Moment(19, 10) lastFor 45
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun readFrom(input: InputStream): List<UniquePeriod> = try {
        Json.decodeFromStream<List<UniquePeriod>>(input)
    } catch (serialization: SerializationException) {
        throw CorruptionException("Unable to read PeriodConfig", serialization)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun writeTo(t: List<UniquePeriod>, output: OutputStream) {
        Json.encodeToStream<List<UniquePeriod>>(t, output)
    }
}