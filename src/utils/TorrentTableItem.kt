package utils

import entities.KeeperItem
import entities.TorrentItem
import java.util.*

class TorrentTableItem(
    val topicId: Int,
    val name: String,
    val date: Date,
    val seeds: Int,
    val keepers: MutableList<KeeperItem>
){
    companion object {
        fun fromTorrentItem(item: TorrentItem): TorrentTableItem{
            return TorrentTableItem(
                item.topicId,
                item.name,
                item.date,
                item.seeds,
                mutableListOf(item.keeper)
            )
        }
    }
}