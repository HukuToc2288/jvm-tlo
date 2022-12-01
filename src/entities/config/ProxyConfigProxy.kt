package entities.config

class ProxyConfigProxy(
    var type: ProxyType,
    var hostname: String,
    var port: Int,
    var login: String? = null,
    var password: String? = null
) {
    enum class ProxyType {
        HTTP, HTTPS, SOCKS4, SOCKS5
    }
}