package eric.schedule_exporter.parser

import androidx.annotation.StringRes
import eric.schedule_exporter.R
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

enum class ParserType(
    val parser: ScheduleParser?,
    @field:StringRes @param:StringRes val text: Int
) {
    UNSPECIFIED(null, R.string.university_unspecified),
    STU_PARSER(STUParser, R.string.university_stu),
    BUU_PARSER(BUUParser, R.string.university_buu);

    fun resolveParser(url: String?): ScheduleParser? {
        return this.parser ?: url?.inferParserByDomain()
    }
}