package eric.schedule_exporter.url

import android.net.Network

interface UrlItem {
    val name: String
    val url: String
    suspend fun onNetworkAvailable(network: Network)
    override fun equals(other: Any?): Boolean
}