package eric.schedule_exporter.model

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import eric.schedule_exporter.data.ParserConfig
import eric.schedule_exporter.data.effectiveHandler
import eric.schedule_exporter.data.handlerConfig
import eric.schedule_exporter.data.parserConfig
import eric.schedule_exporter.handler.HandlerType
import eric.schedule_exporter.url.UrlItem
import eric.schedule_exporter.url.impl.BUUItem
import eric.schedule_exporter.url.impl.SimpleItem
import kotlinx.coroutines.flow.Flow

class ExporterViewModel(app: Application) : AndroidViewModel(app) {
    var currentPopup: HandlerType? by mutableStateOf(null)
    val effectiveHandler: Flow<HandlerType> = app.handlerConfig.effectiveHandler
    val parserConfig: Flow<ParserConfig> = app.parserConfig.data
    val urlSuggestions: SnapshotStateList<UrlItem> = mutableStateListOf(
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
}