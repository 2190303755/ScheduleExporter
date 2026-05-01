package eric.schedule_exporter.model

import android.content.Context
import eric.schedule_exporter.data.ParserConfig
import eric.schedule_exporter.data.effectiveHandler
import eric.schedule_exporter.data.handlerConfig
import eric.schedule_exporter.data.parserConfig
import eric.schedule_exporter.handler.HandlerType
import kotlinx.coroutines.flow.Flow

class ConfigViewModel(val context: Context) {
    val effectiveHandler: Flow<HandlerType> = context.handlerConfig.effectiveHandler
    val parserConfig: Flow<ParserConfig> = context.parserConfig.data
}