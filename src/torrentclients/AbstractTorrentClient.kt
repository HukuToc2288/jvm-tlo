package torrentclients

import utils.TorrentClientException
import java.io.IOException
import kotlin.jvm.Throws

abstract class AbstractTorrentClient() {
    @Throws(IOException::class, TorrentClientException::class)
    abstract fun auth()


    @Throws(IOException::class, TorrentClientException::class)
    abstract fun version(): String
}