package torrentclients

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIncludeProperties
import entities.torrentclient.TorrentClientTorrent
import utils.TorrentClientException
import java.io.IOException
import kotlin.jvm.Throws


@JsonIncludeProperties("name","type","hostname","port","ssl","login","password")
abstract class AbstractTorrentClient(
    val name: String,
    val type: TorrentClientTypes,
    val hostname: String,
    val port: Int,
    val ssl: Boolean,
    val login: String? = null,
    val password: String? = null
) {

    protected val rutrackerCommentRegex = "^https?://rutracker\\..*/forum/viewtopic\\.php\\?t=".toRegex()

    protected val baseUrl
        get() = "http${if (ssl) "s" else ""}://$hostname:$port/"

    @Throws(IOException::class, TorrentClientException::class)
    abstract fun auth()

    @Throws(IOException::class, TorrentClientException::class)
    abstract fun version(): String

    @Throws(IOException::class, TorrentClientException::class)
    abstract fun getTorrents(): List<TorrentClientTorrent>

    @Throws(IOException::class, TorrentClientException::class)
    abstract fun getTorrentTopicId(hash: String): Int

    companion object {

        @JvmStatic
        @JsonCreator
        fun fromJson(
            name: String,
            type: TorrentClientTypes,
            hostname: String,
            port: Int,
            ssl: Boolean,
            login: String? = null,
            password: String? = null
        ): AbstractTorrentClient {
            return when (type) {
                TorrentClientTypes.QBITTORRENT -> Qbittorrent(name, hostname, port, ssl, login, password)
            }
        }
    }
}