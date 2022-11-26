package entities.misc

import entities.db.KeeperItem
import entities.db.TorrentItem
import java.util.*

class TorrentTableItem(
    val topicId: Int,
    val name: String,
    val date: Date,
    val seeds: Int,
    val keepers: MutableList<KeeperItem>
) {
    companion object {
        fun fromTorrentItem(item: TorrentItem): TorrentTableItem {
            return TorrentTableItem(
                item.topicId,
                item.name,
                item.date,
                item.seeds,
                if (item.keeper != null)
                    mutableListOf(item.keeper)
                else
                    mutableListOf()
            )
        }
    }
}