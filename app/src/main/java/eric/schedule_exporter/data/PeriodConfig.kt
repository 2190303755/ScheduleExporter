package eric.schedule_exporter.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import eric.schedule_exporter.ScheduleExporterApplication.Companion.SCHEDULE_PERIODS
import eric.schedule_exporter.util.Period
import eric.schedule_exporter.util.PeriodHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream

val Context.periodConfig: DataStore<List<PeriodHolder>> by dataStore(
    fileName = "PeriodConfig.json",
    serializer = PeriodConfigSerializer
)

suspend fun Context.savePeriods() {
    withContext(Dispatchers.IO) {
        periodConfig.updateData { SCHEDULE_PERIODS }
    }
}


suspend fun Context.addPeriod(period: Period) {
    withContext(Dispatchers.Main) {
        SCHEDULE_PERIODS.add(PeriodHolder(period))
    }
    savePeriods()
}

suspend fun Context.removePeriod(index: Int) {
    withContext(Dispatchers.Main) {
        if (index in 0 until SCHEDULE_PERIODS.size) {
            SCHEDULE_PERIODS.removeAt(index)
            savePeriods()
        }
    }
}

suspend fun Context.updatePeriod(index: Int, period: PeriodHolder) {
    withContext(Dispatchers.Main) {
        if (index in 0 until SCHEDULE_PERIODS.size) {
            SCHEDULE_PERIODS[index] = period
            savePeriods()
        }
    }
}

suspend fun Context.setAllDurations(duration: Int) {
    withContext(Dispatchers.Main) {
        for (i in SCHEDULE_PERIODS.indices) {
            SCHEDULE_PERIODS[i] = SCHEDULE_PERIODS[i].copy {
                it.copy(duration = duration)
            }
        }
    }
    savePeriods()
}

object PeriodConfigSerializer : Serializer<List<PeriodHolder>> {
    override val defaultValue: List<PeriodHolder>
        get() = mutableListOf()

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun readFrom(input: InputStream): List<PeriodHolder> = try {
        Json.decodeFromStream<List<PeriodHolder>>(input)
    } catch (serialization: SerializationException) {
        throw CorruptionException("Unable to read PeriodConfig", serialization)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun writeTo(t: List<PeriodHolder>, output: OutputStream) {
        Json.encodeToStream<List<PeriodHolder>>(t, output)
    }
}