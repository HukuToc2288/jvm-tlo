package entities.torrentclient


class TorrentClientTorrent(
    val hash: String,
    val completed: Boolean,
    var topicId: Int
) {
    companion object {
        const val TOPIC_NEED_QUERY = -1       // topic must be queried separately (e.g. qbittorrent)
        const val TOPIC_THIRD_PARTY = -2      // torrent not from rutracker
    }
}