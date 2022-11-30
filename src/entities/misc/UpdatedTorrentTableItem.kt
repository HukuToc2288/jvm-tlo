package entities.misc

import entities.db.KeeperItem
import entities.db.TorrentItem
import entities.db.UpdatedTorrentItem
import java.util.*

class UpdatedTorrentTableItem(
    topicId: Int,
    name: String,
    date: Date,
    seeds: Int,
    keepers: MutableList<KeeperItem>,
    val oldHash: String,
    val newHash: String,
) : TorrentTableItem(topicId, name, date, seeds, keepers) {
    companion object {
        fun fromUpdatedTorrentItem(item: UpdatedTorrentItem): UpdatedTorrentTableItem {
            return UpdatedTorrentTableItem(
                item.torrentItem.topicId,
                item.torrentItem.name,
                item.torrentItem.date,
                item.torrentItem.seeds,
                if (item.torrentItem.keeper != null)
                    mutableListOf(item.torrentItem.keeper)
                else
                    mutableListOf(),
                item.oldHash,
                item.newHash
            )
        }
    }
}