package eric.schedule_exporter.parser

interface ScheduleParser {
    fun injectJavaScript(): String

    suspend fun parseSchedule(message: String): Iterable<Session>
}