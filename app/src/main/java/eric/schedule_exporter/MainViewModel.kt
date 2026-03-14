package eric.schedule_exporter

import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import eric.schedule_exporter.util.randomUniqueString

class MainViewModel : ViewModel() {
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
        return randomUniqueString()
    }

    @Suppress("unused")
    @JavascriptInterface
    fun dump(uuid: String, url: String, html: String?) {
        this.reports.getOrPut(uuid, ::mutableListOf) += Dump(
            url.toUri().lastPathSegment ?: randomUniqueString(),
            html ?: "! NO ACCESS !"
        )
    }

    data class Dump(val name: String, val content: String)
}