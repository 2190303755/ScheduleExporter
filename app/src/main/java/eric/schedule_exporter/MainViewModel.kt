package eric.schedule_exporter

import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import eric.schedule_exporter.formatter.ScheduleFormatter
import eric.schedule_exporter.formatter.impl.WakeUpFormatter
import eric.schedule_exporter.parser.ScheduleParser
import eric.schedule_exporter.parser.impl.BUUParser
import eric.schedule_exporter.url.UrlItem
import eric.schedule_exporter.url.impl.BUUItem
import eric.schedule_exporter.url.impl.SimpleItem
import java.util.UUID

class MainViewModel : ViewModel() {
    val defaultParser: ScheduleParser get() = BUUParser
    val defaultFormatter: ScheduleFormatter get() = WakeUpFormatter
    val url: MutableLiveData<String?> = MutableLiveData()
    val title: MutableLiveData<String?> = MutableLiveData()
    val progress: MutableLiveData<Int?> = MutableLiveData()
    val loading: MutableLiveData<Boolean> = MutableLiveData()
    val reports = mutableMapOf<String, MutableList<Dump>>()
    val urls: List<UrlItem> = listOf(
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

    val webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            this@MainViewModel.progress.value = newProgress
        }

        override fun onReceivedTitle(view: WebView?, title: String) {
            this@MainViewModel.title.value = title
        }
    }

    @Suppress("unused")
    @JavascriptInterface
    fun uuid(): String {
        return UUID.randomUUID().toString()
    }

    @Suppress("unused")
    @JavascriptInterface
    fun dump(uuid: String, url: String, html: String?) {
        this.reports.getOrPut(uuid, ::mutableListOf) += Dump(
            url.toUri().lastPathSegment ?: UUID.randomUUID().toString(),
            html ?: "! NO ACCESS !"
        )
    }

    data class Dump(val name: String, val content: String)
}