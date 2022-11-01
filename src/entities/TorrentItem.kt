package entities

import java.util.*

class TorrentItem(
    val topicId: Int,
    val name: String,
    val date: Date,
    val seeds: Int,
    val keeper: KeeperItem?
)