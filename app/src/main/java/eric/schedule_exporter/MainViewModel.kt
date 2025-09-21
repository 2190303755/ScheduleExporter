package eric.schedule_exporter

import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import eric.schedule_exporter.parser.ScheduleParser
import eric.schedule_exporter.parser.impl.STUParser

class MainViewModel : ViewModel() {
    val parser: ScheduleParser get() = STUParser
    val url: MutableLiveData<String?> = MutableLiveData()
    val title: MutableLiveData<String?> = MutableLiveData()
    val progress: MutableLiveData<Int?> = MutableLiveData()
    val loading: MutableLiveData<Boolean> = MutableLiveData()

    val webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            this@MainViewModel.progress.value = newProgress
        }

        override fun onReceivedTitle(view: WebView?, title: String) {
            this@MainViewModel.title.value = title
        }
    }
}