package db

import entities.db.*
import utils.TorrentFilterCriteria
import java.sql.DriverManager
import java.util.*
import kotlin.collections.HashMap


object TorrentRepository {

    // FIXME: 04.11.2022 в финальной версии это число должно быть >= 3600
    private val updateTimeout = 1

    init {
        Class.forName("org.sqlite.JDBC")
    }

    val connection = DriverManager.getConnection("jdbc:sqlite:files/webtlo.db")

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
                    // сложная механика, поэтому фильтруем уже на фронте
//                    when {
//                        filter.noKeepers -> {
//                            // проверятся только наличие в отчёте, как в Web-TLO
//                            " AND Keepers.nick IS NULL"
//                        }
//                        // обе галочки выбраны = Есть хранитель из Web-TLO
//                        filter.hasDownloaded && filter.noDownloaded -> {
//                            " AND Keepers.nick IS NOT NULL"
//                        }
//                        filter.hasDownloaded -> {
//                            // есть в отчёте и скачал
//                            // или уже раздаёт, но в отчёте отображается как не скачал
//                            " AND Keepers.nick IS NOT NULL AND (Keepers.complete = 1" +
//                                    " OR KeepersSeeders.nick IS NOT NULL)"
//                        }
//                        filter.noDownloaded -> {
//                            // есть в отчёте но не скачал по отчёту и не раздаёт фактически
//                            " AND (Keepers.nick IS NOT NULL AND Keepers.complete != 1 AND KeepersSeeders.nick IS NULL)"
//                        }
//                        else -> ""
//                    } +
//                    when {
//                        filter.hasSeeders -> {
//                            " AND KeepersSeeders.nick IS NOT NULL"
//                        }
//                        filter.noSeeders -> {
//                            " AND KeepersSeeders.nick IS NULL"
//                        }
//                        else -> ""
//                    } +
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

    fun shouldUpdateForums(): Boolean {
        return shouldUpdate(8888)
    }

    fun shouldUpdateHighPriority(): Boolean {
        return shouldUpdate(9999)
    }

    fun shouldUpdate(section: Int): Boolean {
        with(connection.createStatement().executeQuery("SELECT ud FROM UpdateTime WHERE id = $section LIMIT 1")) {
            val s = !next() || (getInt(1) + updateTimeout) < System.currentTimeMillis()
            close()
            return s
        }
    }

    fun updateForums(forumList: Map<Int, ForumItem>) {
        // TODO: 03.11.2022 process errors
        connection.createStatement()
            .execute("CREATE TEMPORARY TABLE ForumsNew AS SELECT id,na,qt,si FROM Forums WHERE 0 = 1")
        connection.autoCommit = false
        val insertTempStatement =
            connection.prepareStatement("INSERT INTO temp.ForumsNew (id,na,qt,si) VALUES (?,?,?,?)")
        for (forumItem in forumList) {
            insertTempStatement.setInt(1, forumItem.key)
            with(forumItem.value) {
                insertTempStatement.setString(2, title)
                insertTempStatement.setLong(3, count)
                insertTempStatement.setLong(4, size)
            }
            insertTempStatement.addBatch()
        }
        insertTempStatement.executeBatch()

        connection.createStatement()
            .execute("INSERT INTO Forums (id,na,qt,si) SELECT id,na,qt,si FROM temp.ForumsNew")

        connection.createStatement().execute(
            "DELETE FROM Forums WHERE id IN (" +
                    " SELECT Forums.id FROM Forums" +
                    " LEFT JOIN temp.ForumsNew ON Forums.id = temp.ForumsNew.id" +
                    " WHERE temp.ForumsNew.id IS NULL)"
        )
        connection.createStatement().execute("DROP TABLE ForumsNew")
        connection.createStatement()
            .execute("INSERT INTO UpdateTime(id,ud) VALUES (8888,${(System.currentTimeMillis()).toInt()})")
    }

    fun appendFullUpdateTopics(topics: Set<FullUpdateTopic>) {
        connection.createStatement().execute(
            "CREATE TEMPORARY TABLE TopicsNew AS " +
                    "SELECT id,ss,na,hs,se,si,st,rg,qt,ds,pt FROM Topics WHERE 0 = 1"
        )
        if (topics.isEmpty())
            return
        connection.autoCommit = false
        val insertTempStatement =
            connection.prepareStatement(
                "INSERT INTO temp.TopicsNew (id,ss,na,hs,se,si,st,rg,qt,ds,pt)" +
                        " VALUES (?,?,?,?,?,?,?,?,?,?,?)"
            )
        for (topic in topics) {
            insertTempStatement.setInt(1, topic.id)
            insertTempStatement.setInt(2, topic.forumId)
            insertTempStatement.setString(3, topic.title)
            insertTempStatement.setString(4, topic.hash)
            insertTempStatement.setInt(5, topic.seedsCount)
            insertTempStatement.setLong(6, topic.size)
            insertTempStatement.setInt(7, topic.status)
            insertTempStatement.setInt(8, topic.date)
            insertTempStatement.setInt(9, 1)
            insertTempStatement.setInt(10, 0)
            insertTempStatement.setInt(11, topic.priority)
            insertTempStatement.addBatch()
        }
        insertTempStatement.executeBatch()
    }

    fun appendSeedsUpdatedTopics(topics: Set<SeedsUpdateTopic>) {
        connection.createStatement().execute(
            "CREATE TEMPORARY TABLE TopicsUpdated AS " +
                    "SELECT id,se,qt,ds FROM Topics WHERE 0 = 1"
        )
        if (topics.isEmpty())
            return
        connection.autoCommit = false
        val insertTempStatement =
            connection.prepareStatement(
                "INSERT INTO temp.TopicsUpdated (id,se,qt,ds)" +
                        " VALUES (?,?,?,?)"
            )
        for (topic in topics) {
            insertTempStatement.setInt(1, topic.id)
            insertTempStatement.setInt(2, topic.seedsCount)
            insertTempStatement.setInt(3, 1)
            insertTempStatement.setInt(4, 0)
            insertTempStatement.addBatch()
        }
        insertTempStatement.executeBatch()
    }

    fun commitTopics(forumId: Int, deleteOther: Boolean) {
        connection.autoCommit = true

        // делаем всё как с обновлением дерева форумов
        connection.createStatement()
            .execute(
                "INSERT INTO Topics (id,se,qt,ds)" +
                        " SELECT id,se,qt,ds FROM temp.TopicsUpdated"
            )

        connection.createStatement()
            .execute(
                "INSERT INTO Topics (id,ss,na,hs,se,si,st,rg,qt,ds,pt)" +
                        " SELECT id,ss,na,hs,se,si,st,rg,qt,ds,pt FROM temp.TopicsNew"
            )

        if (deleteOther) {
            connection.createStatement().execute(
                "DELETE FROM Topics WHERE Topics.ss = $forumId AND id IN (" +
                        " SELECT Topics.id FROM Topics" +
                        " LEFT JOIN temp.TopicsUpdated ON Topics.id = temp.TopicsUpdated.id" +
                        " LEFT JOIN temp.TopicsNew ON Topics.id = temp.TopicsNew.id" +
                        " WHERE temp.TopicsUpdated.id IS NULL AND temp.TopicsNew.id IS NULL)"
            )
        }

        connection.createStatement().execute("DROP TABLE temp.TopicsUpdated")
        connection.createStatement().execute("DROP TABLE temp.TopicsNew")
    }

    fun getTopicsByIds(ids: List<Int>): Map<Int, String> {
        val resultSet = connection.createStatement().executeQuery(
            "SELECT id,hs FROM Topics" +
                    " WHERE id IN (${ids.joinToString(",")})"
        )
        val existingIds = HashMap<Int, String>()
        while (resultSet.next()) {
            existingIds[resultSet.getInt("id")] = resultSet.getString("hs")
        }
        return existingIds
    }

    fun getTopicsRegistrationDate(forumId: Int): Map<Int, Int> {
        val datesResult = connection.createStatement().executeQuery("SELECT id,rg FROM Topics WHERE ss = $forumId")
        val datesMap = HashMap<Int, Int>()
        while (datesResult.next()) {
            datesMap[datesResult.getInt(1)] = datesResult.getInt(2)
        }
        return datesMap
    }

    fun deleteUnregisteredTopics(forumId: Int, topics: Map<Int, List<Any>>) {
        connection.createStatement().executeQuery(
            "DELETE FROM Topics" +
                    " WHERE ss = forumId AND id NOT IN (${topics.keys.joinToString(",")})"
        )
    }
}