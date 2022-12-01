package entities.config

class ProxyConfig(
    var proxyForum: Boolean = false,
    var proxyApi: Boolean = false,
    var proxies: List<ProxyConfigProxy> = emptyList()
)