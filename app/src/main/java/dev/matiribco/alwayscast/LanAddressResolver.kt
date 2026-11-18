package dev.matiribco.alwayscast

import java.net.Inet4Address
import java.net.NetworkInterface

data class NetworkInterfaceInfo(
    val name: String,
    val isUp: Boolean,
    val isLoopback: Boolean,
    val addresses: List<Inet4Address>,
)

object LanAddressResolver {
    const val LOOPBACK = "127.0.0.1"
    private const val WIFI_DIRECT_DEFAULT_IP = "192.168.49.1"

    private val EXCLUDED_INTERFACE_PREFIXES =
        listOf("p2p", "dummy", "rmnet", "ccmni", "tun", "tap", "sit")

    fun findLanIp(): String =
        runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return@runCatching null
            val infos =
                interfaces.asSequence().map { iface ->
                    NetworkInterfaceInfo(
                        name = iface.name,
                        isUp = iface.isUp,
                        isLoopback = iface.isLoopback,
                        addresses =
                            iface.inetAddresses
                                .asSequence()
                                .filterIsInstance<Inet4Address>()
                                .toList(),
                    )
                }
            selectBestLanIp(infos)
        }.getOrNull() ?: LOOPBACK

    internal fun selectBestLanIp(interfaces: Sequence<NetworkInterfaceInfo>): String {
        val validInterfaces = interfaces.filter { it.isUp && !it.isLoopback }.toList()

        fun validAddresses(info: NetworkInterfaceInfo): List<Inet4Address> =
            info.addresses.filter { addr ->
                !addr.isLoopbackAddress &&
                    !addr.isLinkLocalAddress &&
                    addr.hostAddress != WIFI_DIRECT_DEFAULT_IP &&
                    addr.isSiteLocalAddress
            }

        val wifiIp =
            validInterfaces
                .filter { info ->
                    val lower = info.name.lowercase()
                    (lower.startsWith("wlan") || lower.startsWith("wl")) && !lower.contains("p2p")
                }.flatMap { validAddresses(it).asSequence() }
                .firstOrNull()
                ?.hostAddress

        val ethernetIp =
            validInterfaces
                .filter { info ->
                    val lower = info.name.lowercase()
                    lower.startsWith("eth") || lower.startsWith("en")
                }.flatMap { validAddresses(it).asSequence() }
                .firstOrNull()
                ?.hostAddress

        val otherIp =
            validInterfaces
                .filter { info ->
                    val lower = info.name.lowercase()
                    EXCLUDED_INTERFACE_PREFIXES.none { lower.contains(it) }
                }.flatMap { validAddresses(it).asSequence() }
                .firstOrNull()
                ?.hostAddress

        val fallbackIp =
            validInterfaces
                .flatMap { validAddresses(it).asSequence() }
                .firstOrNull()
                ?.hostAddress

        return wifiIp ?: ethernetIp ?: otherIp ?: fallbackIp ?: LOOPBACK
    }
}
