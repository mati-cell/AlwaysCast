package dev.matiribco.alwayscast

import java.net.Inet4Address
import java.net.NetworkInterface

object LanAddressResolver {
    private const val LOOPBACK = "127.0.0.1"

    fun findLanIp(): String =
        runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return@runCatching null
            interfaces
                .asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.asSequence() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress }
                ?.hostAddress
        }.getOrNull() ?: LOOPBACK
}
