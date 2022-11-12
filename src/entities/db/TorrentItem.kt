package entities.db

import java.util.*

class TorrentItem(
    val topicId: Int,
    val name: String,
    val date: Date,
    val seeds: Int,
    keeper: KeeperItem?
) {
    // дропаем бота статистики здесь
    val keeper = if (keeper?.name == "StatsBot") null else keeper
}