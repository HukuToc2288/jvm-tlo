package torrentclients

object TorrentClientFactory {
    fun createFromSettings(
        name: String,
        client: String,
        hostname: String,
        port: Int,
        login: String,
        password: String,
        ssl: Boolean
    ): AbstractTorrentClient? {
        val baseUrl = "http${if (ssl) "s" else ""}://$hostname:$port/"
        return when (client) {
            "qbittorrent" -> Qbittorrent(name, baseUrl, login, password)
            else -> null
        }
    }
}