package eric.schedule_exporter.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import eric.schedule_exporter.ScheduleExporterApplication.Companion.SCHEDULE_PERIODS
import eric.schedule_exporter.util.Moment
import eric.schedule_exporter.util.Period
import eric.schedule_exporter.util.PeriodRecord
import eric.schedule_exporter.util.UniquePeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream

val Context.periodConfig: DataStore<List<Period>> by dataStore(
    fileName = "PeriodConfig.json",
    serializer = PeriodConfigSerializer
)

suspend fun Context.savePeriods() {
    withContext(Dispatchers.IO) {
        periodConfig.updateData { SCHEDULE_PERIODS }
    }
}

suspend fun Context.addPeriod(period: PeriodRecord) {
    withContext(Dispatchers.Main) {
        SCHEDULE_PERIODS.add(period.makeUnique())
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

suspend fun Context.updatePeriod(index: Int, period: UniquePeriod) {
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
            SCHEDULE_PERIODS[i] = SCHEDULE_PERIODS[i].edit {
                it.copy(duration = duration)
            }
        }
    }
    savePeriods()
}

object PeriodConfigSerializer : Serializer<List<Period>> {
    override val defaultValue: List<Period>
        get() = mutableListOf(
            PeriodRecord(Moment(8, 0), 45),
            PeriodRecord(Moment(8, 50), 45),
            PeriodRecord(Moment(9, 55), 45),
            PeriodRecord(Moment(10, 45), 45),
            PeriodRecord(Moment(11, 35), 45),
            PeriodRecord(Moment(13, 0), 45),
            PeriodRecord(Moment(13, 50), 45),
            PeriodRecord(Moment(14, 50), 45),
            PeriodRecord(Moment(15, 40), 45),
            PeriodRecord(Moment(16, 40), 45),
            PeriodRecord(Moment(17, 30), 45),
            PeriodRecord(Moment(18, 20), 45),
            PeriodRecord(Moment(19, 10), 45)
        )

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun readFrom(input: InputStream): List<Period> = try {
        Json.decodeFromStream<List<Period>>(input)
    } catch (serialization: SerializationException) {
        throw CorruptionException("Unable to read PeriodConfig", serialization)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun writeTo(t: List<Period>, output: OutputStream) {
        Json.encodeToStream<List<Period>>(t, output)
    }
}