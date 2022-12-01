package entities.config

import torrentclients.AbstractTorrentClient

class Config(
    var proxyConfig: ProxyConfig = ProxyConfig(),
    var trackerConfig: TrackerConfig = TrackerConfig(),
    var subsectionsConfig: SubsectionsConfig = SubsectionsConfig(),
    var torrentClients: MutableMap<Int,AbstractTorrentClient> = HashMap()
){

}