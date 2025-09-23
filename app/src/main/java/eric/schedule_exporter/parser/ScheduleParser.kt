package eric.schedule_exporter.parser

interface ScheduleParser {
    fun injectJavaScript(): String

    suspend fun parseSchedule(context: ParserContext, message: String): Iterable<Session>
}