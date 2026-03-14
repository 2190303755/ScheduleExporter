// Application级的ViewModel太难用了
package eric.schedule_exporter

import eric.schedule_exporter.handler.ScheduleHandler
import eric.schedule_exporter.handler.impl.MiAIHandler
import eric.schedule_exporter.parser.ParserType
import eric.schedule_exporter.url.UrlItem
import eric.schedule_exporter.url.impl.BUUItem
import eric.schedule_exporter.url.impl.SimpleItem
import eric.schedule_exporter.util.Period

var SCHEDULE_PARSER: ParserType = ParserType.UNSPECIFIED

var SCHEDULE_HANDLER: ScheduleHandler = MiAIHandler

val URL_SUGGESTIONS: List<UrlItem> = listOf(
    SimpleItem(
        "汕头大学",
        "https://sso.stu.edu.cn/login?service=http%3A%2F%2Fjw.stu.edu.cn"
    ),
    BUUItem,
    SimpleItem(
        "GitHub",
        "https://github.com/2190303755/ScheduleExporter"
    )
)

private val periods = mutableListOf<Period>()

fun getPeriods() = periods.toList()