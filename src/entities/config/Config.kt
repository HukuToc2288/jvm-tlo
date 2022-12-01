package entities.config

import torrentclients.AbstractTorrentClient
import torrentclients.TorrentClientFactory

class Config(
    var proxyConfig: ProxyConfig = ProxyConfig(),
    var trackerConfig: TrackerConfig = TrackerConfig(),
    var subsectionsConfig: SubsectionsConfig = SubsectionsConfig(),
    var torrentClients: MutableList<TorrentClientConfig> = ArrayList()
){

}