package eric.schedule_exporter

import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import eric.schedule_exporter.parser.ScheduleParser
import eric.schedule_exporter.parser.impl.STUParser
import java.util.UUID

class MainViewModel : ViewModel() {
    val parser: ScheduleParser get() = STUParser
    val url: MutableLiveData<String?> = MutableLiveData()
    val title: MutableLiveData<String?> = MutableLiveData()
    val progress: MutableLiveData<Int?> = MutableLiveData()
    val loading: MutableLiveData<Boolean> = MutableLiveData()
    val reports = mutableMapOf<String, MutableList<Dump>>()

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
        val segment = url.toUri().lastPathSegment
        this.reports.getOrPut(uuid) {
            mutableListOf()
        } += Dump(
            if (segment === null || segment.toByteArray(Charsets.UTF_8).size > 0xFFFF)
                UUID.randomUUID().toString()
            else segment,
            html ?: "! NO ACCESS !"
        )
    }

    data class Dump(val name: String, val content: String)
}