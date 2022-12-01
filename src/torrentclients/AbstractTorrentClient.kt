package torrentclients

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonCreator
import entities.torrentclient.TorrentClientTorrent
import utils.TorrentClientException
import java.io.IOException
import kotlin.jvm.Throws

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
abstract class AbstractTorrentClient(
    val name: String,
    val type: TorrentClientTypes,
    val baseUrl: String,
    val login: String?,
    val password: String?
) {

    protected val rutrackerCommentRegex = "^https?://rutracker\\..*/forum/viewtopic\\.php\\?t=".toRegex()

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
            baseUrl: String,
            login: String? = null,
            password: String? = null
        ): AbstractTorrentClient {
            return when (type) {
                TorrentClientTypes.QBITTORRENT -> Qbittorrent(name, baseUrl, login, password)
            }
        }
    }
}