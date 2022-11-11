package torrentclients

import entities.torrentclient.TorrentClientTorrent
import utils.TorrentClientException
import java.io.IOException
import kotlin.jvm.Throws

abstract class AbstractTorrentClient(
    val name: String,
    baseUrl: String,
    val login: String,
    val password: String) {

    val rutrackerCommentRegex = "^https?://rutracker\\..*/forum/viewtopic\\.php\\?t=".toRegex()

    @Throws(IOException::class, TorrentClientException::class)
    abstract fun auth()

    @Throws(IOException::class, TorrentClientException::class)
    abstract fun version(): String

    @Throws(IOException::class, TorrentClientException::class)
    abstract fun getTorrents(): List<TorrentClientTorrent>

    @Throws(IOException::class, TorrentClientException::class)
    abstract fun getTorrentTopicId(hash: String): Int
}