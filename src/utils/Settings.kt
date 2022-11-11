package utils

import org.ini4j.IniPreferences
import org.ini4j.spi.EscapeTool
import torrentclients.AbstractTorrentClient
import torrentclients.TorrentClientFactory
import java.io.File
import java.io.FileInputStream
import java.util.prefs.Preferences

object Settings : IniPreferences(FileInputStream("files/config.ini")) {

    // TODO: 03.11.2022 создавать файл если его нет

    fun getTorrentClients(): Map<Int,AbstractTorrentClient> {
        val torrentClientsCount = Settings.node("other")["qt", "0"].unquote().toInt()
        val torrentClients = HashMap<Int,AbstractTorrentClient>()
        for (i in 1..torrentClientsCount) {
            val clientNode = Settings.node("torrent-client-$i")
            clientNode["id", null].unquote().toIntOrNull()?.let {
                val client = TorrentClientFactory.createFromSettings(
                    clientNode.get("comment", "").unquote(),
                    clientNode.get("client", "").unquote(),
                    clientNode.get("hostname", "").unquote(),
                    clientNode.get("port", "0").unquote().toInt(),
                    clientNode.get("login", "").unquote(),
                    clientNode.get("password", "").unquote(),
                    clientNode.get("ssl", "").unquote() == "1",
                )
                if (client == null) {
                    // TODO: 11.11.2022 писать это в журнал
                } else {
                    torrentClients[it] = client
                }
            }
        }
        return torrentClients
    }
}

public fun String.unquote(): String {
    // все настройки в файле в кавычках, УЖАС!
    return EscapeTool.getInstance().unquote(this)
}


public fun String.quote(): String {
    // все настройки в файле в кавычках, УЖАС!
    if (this.isEmpty())
        return ""
    return EscapeTool.getInstance().quote(this)
}

fun Boolean.toZeroOne(): String {
    return if (this) "\"1\"" else "\"0\""
}