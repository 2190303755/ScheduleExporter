package eric.schedule_exporter

import eric.schedule_exporter.util.Period

private val periods = mutableListOf<Period>()

fun getPeriods() = periods.toList()