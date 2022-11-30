package entities.db

class UpdatedTorrentItem(
    val oldHash: String,
    val newHash: String,
    val torrentItem: TorrentItem
)