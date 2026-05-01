package eric.schedule_exporter.model

import android.content.Context
import eric.schedule_exporter.data.periodConfig
import eric.schedule_exporter.util.Period
import eric.schedule_exporter.util.UniquePeriod
import kotlinx.coroutines.flow.Flow
import java.util.UUID

fun Period.makeUnique(occupied: List<UniquePeriod>): UniquePeriod {
    var uuid: UUID
    do {
        uuid = UUID.randomUUID()
    } while (occupied.any { it.id == uuid })
    return UniquePeriod(this.start, this.duration, uuid)
}

class TimingViewModel(val context: Context) {
    val periods: Flow<List<UniquePeriod>> = context.periodConfig.data

    suspend fun removePeriod(index: Int) {
        this.context.periodConfig.updateData {
            it.filterIndexed { i, _ -> i != index }
        }
    }

    suspend fun appendPeriod(period: Period) {
        this.context.periodConfig.updateData {
            it + period.makeUnique(it)
        }
    }

    suspend fun insertPeriod(index: Int, period: Period) {
        this.context.periodConfig.updateData {
            if (index in 0..it.size) {
                val list = it.toMutableList()
                list.add(index, period.makeUnique(it))
                list
            } else it
        }
    }

    suspend fun updatePeriod(index: Int, period: Period) {
        this.context.periodConfig.updateData {
            if (index in it.indices) {
                val list = it.toMutableList()
                list[index] = list[index].copy(start = period.start, duration = period.duration)
                list
            } else it
        }
    }

    suspend fun unifyDuration(duration: Int) {
        this.context.periodConfig.updateData {
            it.map { period ->
                period.copy(start = period.start, duration = duration)
            }
        }
    }
}
