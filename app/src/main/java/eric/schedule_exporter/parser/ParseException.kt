package eric.schedule_exporter.parser

class ParseException(
    val state: String,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
}