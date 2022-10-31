package entities

import java.util.*

data class TorrentTableItem(
    val name: String,
    val date: Date,
    val seeds: Int,
)