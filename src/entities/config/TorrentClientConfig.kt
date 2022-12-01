package entities.config

class TorrentClientConfig(
    var id: Int,
    var name: String,
    var type: String,
    var url: String,
    var login: String = "",
    var password: String = ""
)