package torrentclients

import entities.config.TorrentClientConfig

object TorrentClientFactory {
    fun buildFromConfig(config: TorrentClientConfig): AbstractTorrentClient? {
        with(config) {
            return when (type) {
                "qbittorrent" -> Qbittorrent(name, url, login, password)
                else -> null
            }
        }
    }
}