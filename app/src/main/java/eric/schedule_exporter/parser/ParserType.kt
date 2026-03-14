package eric.schedule_exporter.parser

import eric.schedule_exporter.parser.impl.BUUParser
import eric.schedule_exporter.parser.impl.STUParser
import okhttp3.HttpUrl.Companion.toHttpUrl

fun String.inferParserByDomain(): ScheduleParser? {
    try {
        val host = this.toHttpUrl().host
        if (!host.endsWith(".edu.cn")) return null
        val parts = host.split(".")
        if (parts.size < 3) return null
        return when (parts[parts.size - 3]) {
            "stu" -> STUParser
            "buu" -> BUUParser
            else -> null
        }
    } catch (_: Exception) {
        return null
    }
}

enum class ParserType(val supplier: (String) -> ScheduleParser?) {
    UNSPECIFIED(String::inferParserByDomain),
    STU_PARSER({ STUParser }),
    BUU_PARSER({ BUUParser })
}