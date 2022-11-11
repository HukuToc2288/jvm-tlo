package db

import entities.db.*
import entities.keeper.ForumTorrentTopicsData
import entities.torrentclient.TorrentClientTorrent
import utils.TorrentFilterCriteria
import java.lang.StringBuilder
import java.sql.DriverManager
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.min


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
            .execute("CREATE TEMPORARY TABLE ForumsNew AS SELECT id,na,qt,si FROM Forums LIMIT 0")
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
        connection.createStatement().execute("DROP TABLE temp.ForumsNew")
        connection.createStatement()
            .execute("INSERT INTO UpdateTime(id,ud) VALUES (8888,${(System.currentTimeMillis()).toInt()})")
    }

    fun appendFullUpdateTopics(topics: Set<FullUpdateTopic>) {
        connection.createStatement().execute(
            "CREATE TEMPORARY TABLE FullUpdateTopics AS " +
                    "SELECT id,ss,na,hs,se,si,st,rg,qt,ds,pt FROM Topics LIMIT 0"
        )
        if (topics.isEmpty())
            return
        connection.autoCommit = false
        val insertTempStatement =
            connection.prepareStatement(
                "INSERT INTO temp.FullUpdateTopics (id,ss,na,hs,se,si,st,rg,qt,ds,pt)" +
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
            "CREATE TEMPORARY TABLE SeedsUpdateTopics AS " +
                    "SELECT id,se,qt,ds FROM Topics LIMIT 0"
        )
        if (topics.isEmpty())
            return
        connection.autoCommit = false
        val insertTempStatement =
            connection.prepareStatement(
                "INSERT INTO temp.SeedsUpdateTopics (id,se,qt,ds)" +
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
                        " SELECT id,se,qt,ds FROM temp.SeedsUpdateTopics"
            )

        connection.createStatement()
            .execute(
                "INSERT INTO Topics (id,ss,na,hs,se,si,st,rg,qt,ds,pt)" +
                        " SELECT id,ss,na,hs,se,si,st,rg,qt,ds,pt FROM temp.FullUpdateTopics"
            )

        if (deleteOther) {
            connection.createStatement().execute(
                "DELETE FROM Topics WHERE Topics.ss = $forumId AND id IN (" +
                        " SELECT Topics.id FROM Topics" +
                        " LEFT JOIN temp.SeedsUpdateTopics ON Topics.id = temp.SeedsUpdateTopics.id" +
                        " LEFT JOIN temp.FullUpdateTopics ON Topics.id = temp.FullUpdateTopics.id" +
                        " WHERE temp.SeedsUpdateTopics.id IS NULL AND temp.FullUpdateTopics.id IS NULL)"
            )
        }

        connection.createStatement().execute("DROP TABLE temp.SeedsUpdateTopics")
        connection.createStatement().execute("DROP TABLE temp.FullUpdateTopics")
    }

    fun getTopicHashesByIds(ids: List<Int>): Map<Int, String> {
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

    // достаём хэши, но только в нужных подразделах
    fun getTopicHashesByIdsInSubsections(ids: Set<Int>, subsections: String): Map<Int, String> {
        val resultSet = connection.createStatement().executeQuery(
            "SELECT id,hs FROM Topics" +
                    " WHERE ss IN ($subsections)" +
                    " AND id IN (${ids.joinToString(",")})"
        )
        val existingIds = HashMap<Int, String>()
        while (resultSet.next()) {
            existingIds[resultSet.getInt("id")] = resultSet.getString("hs")
        }
        return existingIds
    }

    fun getTopicIdsByHashes(
        torrents: List<TorrentClientTorrent>,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE
    ): Map<Int, String> {
        connection.autoCommit = true
        // 40-bytes hash + quotes + comma
        val hashesStringBuilder = StringBuilder((limit) * 43)
        val endIndex = min(offset + limit, torrents.size - 1)
        for (i in offset..endIndex) {
            hashesStringBuilder.append(torrents[i].hash)
            if (i != endIndex)
                hashesStringBuilder.append("','")
        }
        val query = "SELECT id,hs FROM Topics" +
                " WHERE hs COLLATE NOCASE IN ('$hashesStringBuilder')"
        println(query)
        val resultSet = connection.createStatement().executeQuery(
            query
        )
        val existingHashes = HashMap<Int, String>()
        while (resultSet.next()) {
            existingHashes[resultSet.getInt("id")] = resultSet.getString("hs")
        }
        return existingHashes
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

    fun getTorrentsFromClient(clientId: Int): Map<String, Boolean> {
        val torrentsResult = connection.createStatement().executeQuery("SELECT hs,dl FROM Clients WHERE cl=$clientId")
        val torrentsMap = HashMap<String, Boolean>()
        while (torrentsResult.next()) {
            torrentsMap[torrentsResult.getString(1)] = torrentsResult.getInt(2) == 1
        }
        return torrentsMap
    }

    fun createTempClients() {
        connection.createStatement().execute(
            "CREATE TEMPORARY TABLE ClientsNew AS" +
                    " SELECT hs,dl FROM Clients LIMIT 0"
        )
        connection.autoCommit = false
    }

    fun appendTorrentsFromClient(torrents: List<TorrentClientTorrent>, start: Int = 0, end: Int = torrents.size) {
        if (torrents.isEmpty())
            return
        connection.autoCommit = false
        val insertTempStatement =
            connection.prepareStatement(
                "INSERT INTO temp.ClientsNew (hs,dl)" +
                        " VALUES (?,?)"
            )
        for (i in start until end) {
            val torrent = torrents[i]
            insertTempStatement.setString(1, torrent.hash)
            insertTempStatement.setInt(2, if (torrent.completed) 1 else 0)
            insertTempStatement.addBatch()
        }
        insertTempStatement.executeBatch()
    }

    fun commitTorrentsFromClient(clientId: Int) {
        connection.autoCommit = true
        connection.createStatement()
            .execute("INSERT INTO Clients (hs,cl,dl) SELECT hs,$clientId as cl,dl FROM temp.ClientsNew")

        connection.createStatement().execute(
            "DELETE FROM Clients WHERE Clients.cl == $clientId AND hs IN (" +
                    " SELECT Clients.hs FROM Clients" +
                    " LEFT JOIN temp.ClientsNew ON Clients.hs = temp.ClientsNew.hs" +
                    " WHERE temp.ClientsNew.hs IS NULL)"
        )
        connection.createStatement().execute("DROP TABLE temp.ClientsNew")
    }


    fun createTempUntrackedTopics() {
        connection.createStatement().execute(
            "CREATE TEMPORARY TABLE TopicsUntrackedNew AS" +
                    " SELECT id,ss,na,hs,se,si,st,rg FROM TopicsUntracked LIMIT 0"
        )
        connection.autoCommit = false
    }

    fun appendUntrackedTopics(untrackedTopicsData: HashMap<Int, ForumTorrentTopicsData.TopicData?>) {
        if (untrackedTopicsData.isEmpty())
            return
        connection.autoCommit = false
        val statement = connection.prepareStatement(
            "INSERT INTO temp.TopicsUntrackedNew (id,ss,na,hs,se,si,st,rg)" +
                    " VALUES (?,?,?,?,?,?,?,?)"
        )
        for (untrackedTopicEntry in untrackedTopicsData) {
            untrackedTopicEntry.value?.let { untrackedTopic ->
                statement.setInt(1, untrackedTopicEntry.key)
                statement.setInt(2, untrackedTopic.forumId)
                statement.setString(3, untrackedTopic.topicTitle)
                statement.setString(4, untrackedTopic.infoHash)
                statement.setInt(5, untrackedTopic.seeders)
                statement.setLong(6, untrackedTopic.size)
                statement.setInt(7, untrackedTopic.torStatus)
                statement.setInt(8, untrackedTopic.regTime)
                statement.addBatch()
            }
        }
        statement.executeBatch()
    }

    fun commitUntrackedTopics() {
        connection.autoCommit = true
        connection.createStatement()
            .execute("INSERT INTO TopicsUntracked (id,ss,na,hs,se,si,st,rg) SELECT id,ss,na,hs,se,si,st,rg FROM temp.TopicsUntrackedNew")

        connection.createStatement().execute(
            "DELETE FROM TopicsUntracked WHERE TopicsUntracked.id IN (" +
                    " SELECT TopicsUntracked.id FROM TopicsUntracked" +
                    " LEFT JOIN temp.TopicsUntrackedNew ON TopicsUntracked.id = temp.TopicsUntrackedNew.id" +
                    " WHERE temp.TopicsUntrackedNew.id IS NULL)"
        )
        connection.createStatement().execute("DROP TABLE temp.TopicsUntrackedNew")
    }

    fun updateClientsUpdated(updatedTopicsHashes: Map<String, String>, clientId: Int) {
        // FIXME: 11.11.2022 если в одном клиенте обновились больше нескольких тыс. раздач, могут быть проблемы
        if (updatedTopicsHashes.isEmpty())
            return
        connection.createStatement().execute(
            "CREATE TEMPORARY TABLE ClientsUpdatedNew AS" +
                    " SELECT oh,nh FROM ClientsUpdated" +
                    " LIMIT 0"
        )
        connection.autoCommit = false
        val statement =
            connection.prepareStatement(
                "INSERT INTO temp.ClientsUpdatedNew (oh,nh)" +
                        " VALUES (?,?)"
            )
        for (topic in updatedTopicsHashes) {
            statement.setString(1, topic.key)
            statement.setString(2, topic.value)
            statement.addBatch()
        }
        statement.executeBatch()
        connection.autoCommit = true
        connection.createStatement()
            .execute("INSERT INTO ClientsUpdated (cl,oh,nh) SELECT $clientId as cl,oh,nh FROM temp.ClientsUpdatedNew")

        connection.createStatement().execute(
            "DELETE FROM ClientsUpdated WHERE ClientsUpdated.cl == $clientId AND ClientsUpdated.nh IN" +
                    " (SELECT ClientsUpdated.nh FROM ClientsUpdated" +
                    " LEFT JOIN temp.ClientsUpdatedNew" +
                    " ON (ClientsUpdated.nh = temp.ClientsUpdatedNew.nh OR ClientsUpdated.oh = temp.ClientsUpdatedNew.oh)" +
                    " WHERE (temp.ClientsUpdatedNew.nh IS NULL OR temp.ClientsUpdatedNew.oh IS NULL))"
        )
        connection.createStatement().execute("DROP TABLE temp.ClientsUpdatedNew")
    }
}