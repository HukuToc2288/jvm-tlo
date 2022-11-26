package entities.db

class UpdatedTorrentItem(
    val clientId: Int,
    val oldHash: String,
    val newHash: String,
    val torrentItem: TorrentItem
)