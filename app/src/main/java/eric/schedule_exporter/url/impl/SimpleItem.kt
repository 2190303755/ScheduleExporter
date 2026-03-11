package eric.schedule_exporter.url.impl

import android.net.Network
import eric.schedule_exporter.url.UrlItem

data class SimpleItem(
    override val name: String,
    override val url: String
) : UrlItem {
    override fun onNetworkAvailable(network: Network, observer: UrlItem.Observer) {}
}