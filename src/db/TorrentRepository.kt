package db

import entities.TorrentTableItem
import utils.TorrentFilterCriteria
import java.sql.DriverManager
import java.util.*


object TorrentRepository {


    init {
        Class.forName("org.sqlite.JDBC")
    }

    val connection = DriverManager.getConnection("jdbc:sqlite:webtlo.db")

    fun getAllTorrents(): Iterator<TorrentTableItem> {
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery("SELECT na,rg,se FROM Topics")

        return object : Iterator<TorrentTableItem> {
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
                    Date(resultSet.getInt(2)*1000L),
                    resultSet.getInt(3)
                )
            }
        }
    }

    fun getFilteredTorrents(torrentFilter: TorrentFilterCriteria): Iterator<TorrentTableItem> {
        val statement = connection.createStatement()
        val query ="SELECT na,rg,se FROM Topics " +
                "WHERE st IN (${torrentFilter.statuses.joinToString(",")}) " +
                "AND pt IN (${torrentFilter.priorities.joinToString(",")}) " +
                "ORDER BY ${
                    when(torrentFilter.sortOrder){
                        TorrentFilterCriteria.SortOrder.NAME -> "na"
                        TorrentFilterCriteria.SortOrder.SIZE -> "si"
                        TorrentFilterCriteria.SortOrder.SEEDS -> "se"
                        TorrentFilterCriteria.SortOrder.DATE -> "rg"
                    }
                }${if (torrentFilter.sortAscending) "" else " DESC"}"
        println(query)
        val resultSet = statement.executeQuery(query)

        return object : Iterator<TorrentTableItem> {
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
                    Date(resultSet.getInt(2)*1000L),
                    resultSet.getInt(3)
                )
            }
        }
    }
}