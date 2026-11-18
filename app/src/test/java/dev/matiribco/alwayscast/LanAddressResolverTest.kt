package dev.matiribco.alwayscast

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.InetAddress

class LanAddressResolverTest {
    @Test
    fun selectBestLanIp_sinInterfacesValidas_retornaLoopback() {
        // Arrange
        val interfaces = emptySequence<NetworkInterfaceInfo>()

        // Act
        val result = LanAddressResolver.selectBestLanIp(interfaces)

        // Assert
        assertEquals(LanAddressResolver.LOOPBACK, result)
    }

    @Test
    fun selectBestLanIp_soloLoopbackOInactivas_retornaLoopback() {
        // Arrange
        val ip = InetAddress.getByName("127.0.0.1") as java.net.Inet4Address
        val interfaces =
            sequenceOf(
                NetworkInterfaceInfo(
                    name = "lo",
                    isUp = true,
                    isLoopback = true,
                    addresses = listOf(ip),
                ),
                NetworkInterfaceInfo(
                    name = "wlan0",
                    isUp = false,
                    isLoopback = false,
                    addresses = listOf(InetAddress.getByName("192.168.1.100") as java.net.Inet4Address),
                ),
            )

        // Act
        val result = LanAddressResolver.selectBestLanIp(interfaces)

        // Assert
        assertEquals(LanAddressResolver.LOOPBACK, result)
    }

    @Test
    fun selectBestLanIp_conP2pYConWlan_priorizaWlanYSaltaP2p() {
        // Arrange: p2p0 viene primero con la IP típica de Android Wi-Fi Direct (192.168.49.1)
        val p2pIp = InetAddress.getByName("192.168.49.1") as java.net.Inet4Address
        val wlanIp = InetAddress.getByName("192.168.1.55") as java.net.Inet4Address

        val interfaces =
            sequenceOf(
                NetworkInterfaceInfo(
                    name = "p2p0",
                    isUp = true,
                    isLoopback = false,
                    addresses = listOf(p2pIp),
                ),
                NetworkInterfaceInfo(
                    name = "wlan0",
                    isUp = true,
                    isLoopback = false,
                    addresses = listOf(wlanIp),
                ),
            )

        // Act
        val result = LanAddressResolver.selectBestLanIp(interfaces)

        // Assert
        assertEquals("192.168.1.55", result)
    }

    @Test
    fun selectBestLanIp_conRmnetYConWlan_priorizaWlanSobreDatosMoviles() {
        // Arrange: rmnet0 (datos móviles) y wlan0 (Wi-Fi)
        val rmnetIp = InetAddress.getByName("10.15.20.25") as java.net.Inet4Address
        val wlanIp = InetAddress.getByName("192.168.0.88") as java.net.Inet4Address

        val interfaces =
            sequenceOf(
                NetworkInterfaceInfo(
                    name = "rmnet_data0",
                    isUp = true,
                    isLoopback = false,
                    addresses = listOf(rmnetIp),
                ),
                NetworkInterfaceInfo(
                    name = "wlan0",
                    isUp = true,
                    isLoopback = false,
                    addresses = listOf(wlanIp),
                ),
            )

        // Act
        val result = LanAddressResolver.selectBestLanIp(interfaces)

        // Assert
        assertEquals("192.168.0.88", result)
    }

    @Test
    fun selectBestLanIp_conEthernet_retornaEthernet() {
        // Arrange
        val ethIp = InetAddress.getByName("192.168.1.200") as java.net.Inet4Address
        val interfaces =
            sequenceOf(
                NetworkInterfaceInfo(
                    name = "eth0",
                    isUp = true,
                    isLoopback = false,
                    addresses = listOf(ethIp),
                ),
            )

        // Act
        val result = LanAddressResolver.selectBestLanIp(interfaces)

        // Assert
        assertEquals("192.168.1.200", result)
    }

    @Test
    fun selectBestLanIp_conDireccionLinkLocal_esIgnorada() {
        // Arrange: 169.254.x.x link-local autoconfigurada
        val linkLocalIp = InetAddress.getByName("169.254.12.34") as java.net.Inet4Address
        val interfaces =
            sequenceOf(
                NetworkInterfaceInfo(
                    name = "wlan0",
                    isUp = true,
                    isLoopback = false,
                    addresses = listOf(linkLocalIp),
                ),
            )

        // Act
        val result = LanAddressResolver.selectBestLanIp(interfaces)

        // Assert: al no tener IP privada válida, cae al fallback LOOPBACK
        assertEquals(LanAddressResolver.LOOPBACK, result)
    }
}
