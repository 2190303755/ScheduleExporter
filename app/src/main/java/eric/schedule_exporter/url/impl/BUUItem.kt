package eric.schedule_exporter.url.impl

import android.net.Network
import eric.schedule_exporter.url.UrlItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

object BUUItem : UrlItem {
    var isInCampus: Boolean = false
    override val name: String
        get() = "北京联合大学"
    override val url: String
        get() = if (this.isInCampus) {
            "https://jwxt.buu.edu.cn"
        } else {
            "https://wvpn.buu.edu.cn"
        }

    override fun onNetworkAvailable(network: Network, observer: UrlItem.Observer) {
        observer.coroutineScope.launch(Dispatchers.IO) {
            val wasInCampus = isInCampus
            try {
                InetAddress.getByName("go.buu.edu.cn")
                isInCampus = true
            } catch (_: Exception) {
                isInCampus = false
            }
            if (isInCampus != wasInCampus) {
                withContext(Dispatchers.Main) {
                    observer.onItemChange(this@BUUItem)
                }
            }
        }
    }

    override fun equals(other: Any?) = other === this
}