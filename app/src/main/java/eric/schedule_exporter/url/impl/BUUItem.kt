package eric.schedule_exporter.url.impl

import android.net.Network
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eric.schedule_exporter.url.UrlItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

object BUUItem : UrlItem {
    const val DIRECT = "https://jwxt.buu.edu.cn"
    const val WEB_VPN = "https://wvpn.buu.edu.cn"
    override val name: String
        get() = "北京联合大学"
    override var url: String by mutableStateOf(WEB_VPN)
        internal set

    override suspend fun onNetworkAvailable(network: Network) {
        withContext(Dispatchers.IO) {
            val url = try {
                InetAddress.getByName("go.buu.edu.cn")
                DIRECT
            } catch (_: Exception) {
                WEB_VPN
            }
            withContext(Dispatchers.Main) {
                this@BUUItem.url = url
            }
        }
    }

    override fun equals(other: Any?) = other === this
}