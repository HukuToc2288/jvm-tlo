package db

import entities.TorrentTableItem
import java.sql.DriverManager
import java.sql.SQLException


object TorrentRepository {


    init {
        Class.forName("org.sqlite.JDBC")
    }

    val connection = DriverManager.getConnection("jdbc:sqlite:webtlo.db")

    fun getAllTorrents(): Iterator<TorrentTableItem>{
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery("SELECT na,rg,se FROM Topics LIMIT 3")
        val torrentList = ArrayList<TorrentTableItem>()

        return object : Iterator<TorrentTableItem>{
            var cachedHasNext = false
            override fun hasNext(): Boolean {
                if (!cachedHasNext)
                    cachedHasNext = resultSet.next()
                return cachedHasNext
            }

            override fun next(): TorrentTableItem {
                if (!cachedHasNext)
                    resultSet.next()
                cachedHasNext = false
                return TorrentTableItem(
                    resultSet.getString(1),
                    resultSet.getDate(2),
                    resultSet.getInt(3)
                )
            }
        }
    }
}