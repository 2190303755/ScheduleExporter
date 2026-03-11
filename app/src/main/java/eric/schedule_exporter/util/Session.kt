package eric.schedule_exporter.util

import java.time.DayOfWeek

data class Session(
    val subject: String,
    val teacher: String,
    val location: String,
    val start: Int,
    val end: Int,
    val dayOfWeek: DayOfWeek
)