package eric.schedule_exporter.url

import android.net.Network
import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineScope

interface UrlItem {
    val name: String
    val url: String
    fun onNetworkAvailable(network: Network, observer: Observer)
    override fun equals(other: Any?): Boolean

    interface Observer {
        val coroutineScope: CoroutineScope

        @MainThread
        fun onItemChange(item: UrlItem)
    }
}