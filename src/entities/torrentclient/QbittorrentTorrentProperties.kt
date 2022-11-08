package entities.torrentclient

import com.fasterxml.jackson.annotation.JsonProperty

class QbittorrentTorrentProperties(
    @JsonProperty("comment") val comment: String
)