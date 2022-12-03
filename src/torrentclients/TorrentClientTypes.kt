package torrentclients

enum class TorrentClientTypes(val displayName: String) {
    QBITTORRENT("qBittorrent");

    override fun toString(): String {
        return displayName
    }
}