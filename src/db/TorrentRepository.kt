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
                    Date(resultSet.getInt(2) * 1000L),
                    resultSet.getInt(3)
                )
            }
        }
    }

    fun getFilteredTorrents(filter: TorrentFilterCriteria): Iterator<TorrentTableItem> {
        val query = "SELECT na,rg,se FROM Topics " +
                "WHERE st IN (${filter.statuses.joinToString(",")}) " +
                "AND pt IN (${filter.priorities.joinToString(",")}) " +
                "AND se >= ${filter.minAverageSeeds} AND  se <= ${filter.maxAverageSeeds} " +
                "AND rg <= ${(filter.registerDate.time / 1000).toInt()} " +
                "AND na LIKE ? ESCAPE '\\'" +
                "ORDER BY ${
                    when (filter.sortOrder) {
                        TorrentFilterCriteria.SortOrder.NAME -> "na"
                        TorrentFilterCriteria.SortOrder.SIZE -> "si"
                        TorrentFilterCriteria.SortOrder.SEEDS -> "se"
                        TorrentFilterCriteria.SortOrder.DATE -> "rg"
                    }
                }${if (filter.sortAscending) "" else " DESC"}"
        val statement = connection.prepareStatement(query)
        println(statement)
        // TODO: 01.11.2022 уточнить правильно ли я всё заэкранировал
        statement.setString(1, "%${filter.titleSearchText
            .replace("\\","\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")}%")
        val resultSet = statement.executeQuery()

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
                    Date(resultSet.getInt(2) * 1000L),
                    resultSet.getInt(3)
                )
            }
        }
    }
}