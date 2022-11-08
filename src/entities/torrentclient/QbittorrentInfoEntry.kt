package entities.torrentclient

import com.fasterxml.jackson.annotation.JsonProperty

class QbittorrentInfoEntry(
    @JsonProperty("hash") val hash: String,
    @JsonProperty("state") val state: String,
    @JsonProperty("amount_left") val amountLeft: Long,
)