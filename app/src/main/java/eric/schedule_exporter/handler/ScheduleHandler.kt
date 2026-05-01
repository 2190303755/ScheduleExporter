package eric.schedule_exporter.handler

import androidx.compose.runtime.Composable
import eric.schedule_exporter.Exporter
import eric.schedule_exporter.SessionAdapter
import eric.schedule_exporter.data.HandlerSpec
import eric.schedule_exporter.ui.WebViewContext


interface ScheduleHandler<T> : SessionAdapter<T> {
    val type: HandlerType
    suspend fun loadSpec(config: HandlerSpec?): Boolean
    suspend fun saveSpec(): HandlerSpec?

    @Composable
    fun displayName(): String

    @Composable
    fun ConfigSection(onCancel: () -> Unit)

    @Composable
    fun Popup(exporter: Exporter, webViewContext: WebViewContext) {
    }

    suspend fun parseAndExport(exporter: Exporter, webViewContext: WebViewContext)
}