package eric.schedule_exporter.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.lang.Math.floorMod
import java.time.DayOfWeek
import java.util.UUID

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

fun StringBuilder.appendTimePart(part: Int): StringBuilder = if (part < 10) {
    this.append('0').append(part)
} else {
    this.append(part)
}

@Serializable
@JvmInline
value class Moment internal constructor(val offset: Int) : Comparable<Moment> {
    constructor(hour: Int, minute: Int) : this(
        floorMod(hour * 60 + minute, MINUTES_PER_DAY)
    )

    val hour: Int get() = this.offset / MINUTES_PER_HOUR
    val minute: Int get() = this.offset % MINUTES_PER_HOUR
    operator fun plus(duration: Int): Moment = (this.offset + duration).toMoment()
    operator fun minus(duration: Int): Moment = (this.offset - duration).toMoment()
    operator fun minus(moment: Moment): Int = floorMod(this.offset - moment.offset, MINUTES_PER_DAY)
    override fun toString() = StringBuilder()
        .appendTimePart(this.hour)
        .append(':')
        .appendTimePart(this.minute)
        .toString()

    override fun compareTo(other: Moment): Int = this.offset.compareTo(other.offset)

    companion object : KSerializer<Moment> {
        override val descriptor = String.serializer().descriptor

        override fun serialize(encoder: Encoder, value: Moment) {
            encoder.encodeString(value.toString())
        }

        override fun deserialize(decoder: Decoder): Moment {
            val parts = decoder.decodeString().split(":")
            require(parts.size == 2) { "Invalid time format" }
            val hour = parts[0].toInt()
            require(hour in 0 until 24) { "Invalid hour" }
            val minute = parts[1].toInt()
            require(minute in 0 until 60) { "Invalid minute" }
            return Moment(hour, minute)
        }
    }
}

sealed interface Period {
    val start: Moment
    val duration: Int
}

val Period.end: Moment
    get() = this.start + this.duration

data class PeriodRecord(
    override val start: Moment,
    override val duration: Int
) : Period

@Serializable
data class UniquePeriod(
    override val start: Moment,
    override val duration: Int,
    @Serializable(with = UUIDSerializer::class)
    val id: UUID
) : Period

@JvmInline
value class UniquePeriodBuilderScope(val occupied: Set<UUID> = hashSetOf()) {
    infix fun Moment.lastFor(duration: Int): UniquePeriod {
        var uuid: UUID
        do {
            uuid = UUID.randomUUID()
        } while (uuid in occupied)
        return UniquePeriod(this, duration, uuid)
    }
}