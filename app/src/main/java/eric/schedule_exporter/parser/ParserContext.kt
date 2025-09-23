package eric.schedule_exporter.parser

import androidx.annotation.MainThread

interface ParserContext {
    @MainThread
    fun dumpSource(message: String?)
}