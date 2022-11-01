package db

import entities.KeeperItem
import entities.TorrentItem
import utils.TorrentFilterCriteria
import java.sql.DriverManager
import java.util.*


object TorrentRepository {


    init {
        Class.forName("org.sqlite.JDBC")
    }

    val connection = DriverManager.getConnection("jdbc:sqlite:webtlo.db")

//    fun getAllTorrents(): Iterator<TorrentTableItem> {
//        val statement = connection.createStatement()
//        val resultSet = statement.executeQuery("SELECT na,rg,se FROM Topics")
//
//        return object : Iterator<TorrentTableItem> {
//            var cachedHasNext = false
//            override fun hasNext(): Boolean {
//                if (!cachedHasNext)
//                    cachedHasNext = resultSet.next()
//                return cachedHasNext
//            }
//
//            override fun next(): TorrentTableItem {
//                if (!cachedHasNext)
//                    resultSet.next()
//                cachedHasNext = false
//                return TorrentTableItem(
//                    resultSet.getString(1),
//                    Date(resultSet.getInt(2) * 1000L),
//                    resultSet.getInt(3)
//                )
//            }
//        }
//    }

    fun getFilteredTorrents(filter: TorrentFilterCriteria): Iterator<TorrentItem> {
        // FIXME: 01.11.2022 некоторые комбинации галочек приводят к невероятным результатам
        val query =
            "SELECT Topics.id,Topics.na,Topics.rg,Topics.se,Keepers.nick,Keepers.complete,KeepersSeeders.nick FROM Topics" +
                    " LEFT OUTER JOIN Keepers ON Topics.id = Keepers.id" +
                    " LEFT OUTER JOIN KeepersSeeders ON Topics.id = KeepersSeeders.topic_id" +
                    " AND (KeepersSeeders.nick IS NULL OR Keepers.nick IS NULL OR Keepers.nick == KeepersSeeders.nick)" +
                    " WHERE st IN (${filter.statuses.joinToString(",")})" +
                    " AND pt IN (${filter.priorities.joinToString(",")})" +
                    " AND se >= ${filter.minAverageSeeds} AND  se <= ${filter.maxAverageSeeds}" +
                    " AND rg <= ${(filter.registerDate.time / 1000).toInt()}" +
                    " AND na LIKE ? ESCAPE '\\'" +
                    when {
                        // проверятся только наличие в отчёте, как в Web-TLO
                        filter.noKeepers -> {
                            " AND Keepers.nick IS NULL"
                        }
                        else -> ""
                    } +
                    when {
                        filter.hasDownloaded -> {
                            // есть в отчёте и скачал
                            " AND Keepers.nick IS NOT NULL AND Keepers.complete = 1"
                        }
                        filter.noDownloaded -> {
                            // есть в отчёте но не скачал
                            " AND Keepers.nick IS NOT NULL AND Keepers.complete = 0"
                        }
                        else -> ""
                    } +
                    when {
                        filter.hasSeeders -> {
                            " AND KeepersSeeders.nick IS NOT NULL"
                        }
                        filter.noSeeders -> {
                            " AND KeepersSeeders.nick IS NULL"
                        }
                        else -> ""
                    } +
                    " ORDER BY Topics." +
                    "${
                        when (filter.sortOrder) {
                            TorrentFilterCriteria.SortOrder.NAME -> "na"
                            TorrentFilterCriteria.SortOrder.SIZE -> "si"
                            TorrentFilterCriteria.SortOrder.SEEDS -> "se"
                            TorrentFilterCriteria.SortOrder.DATE -> "rg"
                        }
                    }${if (filter.sortAscending) "" else " DESC"}, Topics.id ASC"
        val statement = connection.prepareStatement(query)
        // TODO: 01.11.2022 уточнить правильно ли я всё заэкранировал
        statement.setString(
            1, "%${
                filter.titleSearchText
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_")
            }%"
        )
        println(statement)
        val resultSet = statement.executeQuery()

        return object : Iterator<TorrentItem> {
            var cachedHasNext = false
            override fun hasNext(): Boolean {
                if (!cachedHasNext)
                    cachedHasNext = resultSet.next()
                return cachedHasNext
            }

            override fun next(): TorrentItem {
                if (!cachedHasNext)
                    resultSet.next()
                cachedHasNext = false
                val keeper = resultSet.getString(5)
                val complete = resultSet.getInt(6)
                val seeder = resultSet.getString(7)
                val keeperItem: KeeperItem? = if (keeper != null) {
                    // есть официальный хранитель
                    KeeperItem(
                        keeper,
                        when {
                            seeder == keeper -> {
                                // официальный хранитель раздаёт
                                KeeperItem.Status.FULL
                            }
                            complete == 1 -> {
                                // официальный хранитель скачал и не раздаёт (вот редиска)
                                KeeperItem.Status.KEEPING
                            }
                            else -> {
                                // официальный хранитель ещё не скачал
                                KeeperItem.Status.DOWNLOADING
                            }
                        }
                    )
                } else if (seeder != null) {
                    // хранитель раздаёт неофициально
                    KeeperItem(seeder, KeeperItem.Status.SEEDING)
                } else {
                    null
                }
                return TorrentItem(
                    resultSet.getInt(1),
                    resultSet.getString(2),
                    Date(resultSet.getInt(3) * 1000L),
                    resultSet.getInt(4),
                    keeperItem
                )
            }
        }
    }
}