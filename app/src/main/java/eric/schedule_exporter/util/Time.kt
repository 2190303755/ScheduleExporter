package eric.schedule_exporter.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.lang.Math.floorMod
import java.time.DayOfWeek
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

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
value class Moment internal constructor(val offset: Int) {
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
}

@Serializable
data class Period(val start: Moment, val duration: Int)

@OptIn(ExperimentalAtomicApi::class)
@Serializable(with = PeriodHolder.Companion::class)
data class PeriodHolder(
    @Transient
    val id: Int,
    val period: Period
) {
    constructor(period: Period) : this(COUNTER.fetchAndAdd(1), period)

    companion object : KSerializer<PeriodHolder> {
        override val descriptor: SerialDescriptor = Period.serializer().descriptor

        override fun serialize(encoder: Encoder, value: PeriodHolder) {
            encoder.encodeSerializableValue(Period.serializer(), value.period)
        }

        override fun deserialize(decoder: Decoder) = PeriodHolder(
            decoder.decodeSerializableValue(Period.serializer())
        )

        @JvmField
        internal val COUNTER: AtomicInt = AtomicInt(0)
    }

    inline fun copy(factory: (Period) -> Period) =
        PeriodHolder(this.id, factory(this.period))
}