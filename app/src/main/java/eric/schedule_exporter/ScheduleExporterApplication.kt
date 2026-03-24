package eric.schedule_exporter

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import eric.schedule_exporter.data.handlerConfig
import eric.schedule_exporter.data.parserConfig
import eric.schedule_exporter.data.periodConfig
import eric.schedule_exporter.handler.ScheduleHandler
import eric.schedule_exporter.handler.impl.MiAIHandler
import eric.schedule_exporter.parser.ParserType
import eric.schedule_exporter.url.UrlItem
import eric.schedule_exporter.url.impl.BUUItem
import eric.schedule_exporter.url.impl.SimpleItem
import eric.schedule_exporter.util.PeriodHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScheduleExporterApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        this.applicationScope.launch(Dispatchers.IO) {
            parserConfig.data.collectLatest {
                withContext(Dispatchers.Main) {
                    SCHEDULE_PARSER = it.parser
                }
            }
        }
        this.applicationScope.launch(Dispatchers.IO) {
            handlerConfig.data.collectLatest {
                val type = it.type
                val handler = type.handler
                if (handler.loadSpec(it.specs[type])) {
                    withContext(Dispatchers.Main) {
                        SCHEDULE_HANDLER = handler
                    }
                }
            }
        }
        this.applicationScope.launch(Dispatchers.IO) {
            periodConfig.data.collectLatest {
                if (it !== SCHEDULE_PERIODS) {
                    withContext(Dispatchers.Main) {
                        SCHEDULE_PERIODS.clear()
                        SCHEDULE_PERIODS.addAll(it)
                    }
                }
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        this.applicationScope.cancel()
    }

    companion object {
        lateinit var INSTANCE: ScheduleExporterApplication
            private set

        @JvmField
        var SCHEDULE_PARSER: ParserType = ParserType.UNSPECIFIED

        @JvmField
        var SCHEDULE_HANDLER: ScheduleHandler<*> = MiAIHandler

        @JvmField
        val SCHEDULE_PERIODS: SnapshotStateList<PeriodHolder> = mutableStateListOf()

        @JvmField
        val URL_SUGGESTIONS: SnapshotStateList<UrlItem> = mutableStateListOf(
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
}
