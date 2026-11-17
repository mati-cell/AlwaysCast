package dev.matiribco.alwayscast

object ProxyUrl {
    private const val MIN_PORT = 1
    private const val MAX_PORT = 65535
    private const val LOOPBACK_IPV4 = "127.0.0.1"
    private const val LOOPBACK_NAME = "localhost"
    private const val LOOPBACK_IPV6 = "::1"
    private const val SLASH = "/"

    fun format(
        host: String,
        port: Int,
        path: String,
    ): String {
        require(port in MIN_PORT..MAX_PORT) { "Invalid port: $port" }
        val cleanPath = if (path.startsWith(SLASH)) path else "$SLASH$path"
        return "http://$host:$port$cleanPath"
    }

    fun isLoopback(host: String): Boolean = host == LOOPBACK_IPV4 || host == LOOPBACK_NAME || host == LOOPBACK_IPV6
}
