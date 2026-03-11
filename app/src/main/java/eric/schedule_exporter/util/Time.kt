package eric.schedule_exporter.util

import java.lang.Math.floorMod
import java.time.DayOfWeek

const val MINUTES_PER_HOUR = 60
const val HOURS_PER_DAY = 24
const val MINUTES_PER_DAY = MINUTES_PER_HOUR * HOURS_PER_DAY

fun Int.toMoment() = Moment(floorMod(this, MINUTES_PER_DAY))

fun String.toDayOfWeek() = when (this) {
    "一" -> DayOfWeek.MONDAY
    "二" -> DayOfWeek.TUESDAY
    "三" -> DayOfWeek.WEDNESDAY
    "四" -> DayOfWeek.THURSDAY
    "五" -> DayOfWeek.FRIDAY
    "六" -> DayOfWeek.SATURDAY
    else -> DayOfWeek.SUNDAY
}

@JvmInline
value class Moment internal constructor(val offset: Int) {
    val hour: Int get() = this.offset / MINUTES_PER_HOUR
    val minute: Int get() = this.offset % MINUTES_PER_HOUR
    operator fun plus(duration: Int) = (duration + this.offset).toMoment()
    override fun toString() = "${hour}:${minute}"
}

data class Period(val start: Moment, val duration: Int)