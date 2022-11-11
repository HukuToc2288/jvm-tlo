package gui.operations

import api.keeperRetrofit
import db.TorrentRepository
import entities.db.FullUpdateTopic
import entities.db.SeedsUpdateTopic
import entities.torrentclient.TorrentClientTorrent
import utils.Settings
import utils.pluralForum
import utils.unquote
import java.awt.Frame
import kotlin.math.min

class UpdateTopicsDialog(frame: Frame?) : OperationDialog(frame, "Обновление списка раздач") {


    override fun doTask() {
        val updateTorrentsThread = Thread {
            updateSubsections()
            updateTopicsFromClient()
            onTaskSuccess()
        }
        updateTorrentsThread.start()
    }

    fun updateSubsections() {
        // FIXME: 06.11.2022 сувать всё в один try-catch блок это плохо
        try {
            val subsections =
                Settings.node("sections")["subsections", ""].unquote().split(',')
            setFullProgress(0, subsections.size)
            setFullText("Обновляются темы в ${subsections.size.pluralForum("подраздела", "подразделов")}")
            setCurrentText("Получение данных с сервера...")
            if (cancelTaskIfRequested {})
                return
            val keepersMap = keeperRetrofit.keepersUserData().execute().body()!!

            val torrentStatus = setOf(0, 2, 3, 8, 10)
            if (cancelTaskIfRequested {})
                return
            val limit = keeperRetrofit.getLimit().execute().body()!!.limit
            for (i in subsections.indices) {
                val subsection = subsections[i].toInt()
                if (!TorrentRepository.shouldUpdate(subsection)) {
                    println("Notice: Не требуется обновление для подраздела № $subsection")
                    continue
                }
                setCurrentText("Обновление подраздела $subsection")
                try {
                    // запрашиваем инфу о раздачах в подразделе с сервера
                    if (cancelTaskIfRequested {})
                        return
                    val forumTorrents =
                        keeperRetrofit.getForumTorrents(subsection).execute().body()!!.result
                    setCurrentProgress(0, forumTorrents.size)
                    // даты регистрации с клиента
                    val existingTopicsDates =
                        TorrentRepository.getTopicsRegistrationDate(subsection)
                    val fullUpdateTopics = HashSet<FullUpdateTopic>()
                    val seedsUpdateTopics = HashSet<SeedsUpdateTopic>()
                    val topicsToQuery = HashMap<Int, List<Any>>()
                    for (forumTorrent in forumTorrents) {
                        if (forumTorrent.value[0] !in torrentStatus)
                            continue
                        val inDb = forumTorrent.key in existingTopicsDates.keys
                        val needToQuery =
                            !inDb || forumTorrent.value[2] != existingTopicsDates[forumTorrent.key]
                        if (needToQuery) {
                            // нужно запросить инфу о раздаче
                            // добавляем тему в очередь
                            topicsToQuery.put(forumTorrent.key, forumTorrent.value)
                            if (topicsToQuery.size == limit) {
                                if (cancelTaskIfRequested {
                                        // добавим то, что уже обновили
                                        TorrentRepository.appendSeedsUpdatedTopics(seedsUpdateTopics)
                                        TorrentRepository.appendFullUpdateTopics(fullUpdateTopics)
                                        TorrentRepository.commitTopics(subsection, false)
                                    })
                                    return
                                requestTorrentTopicsData(topicsToQuery, fullUpdateTopics)
                                incrementCurrentProgress(limit)
                                topicsToQuery.clear()
                            }
                        } else {
                            // просто обновим сиды
                            seedsUpdateTopics.add(
                                SeedsUpdateTopic(
                                    forumTorrent.key,
                                    forumTorrent.value[1] as Int,
                                    1,
                                    0
                                )
                            )
                            incrementCurrentProgress()
                        }
                    }
                    if (topicsToQuery.isNotEmpty()) {
                        if (cancelTaskIfRequested {
                                // добавим то, что уже обновили
                                TorrentRepository.appendSeedsUpdatedTopics(seedsUpdateTopics)
                                TorrentRepository.appendFullUpdateTopics(fullUpdateTopics)
                                TorrentRepository.commitTopics(subsection, false)
                            })
                            return
                        requestTorrentTopicsData(topicsToQuery, fullUpdateTopics)
                        incrementCurrentProgress(topicsToQuery.size)
                        topicsToQuery.clear()
                    }
                    TorrentRepository.appendSeedsUpdatedTopics(seedsUpdateTopics)
                    TorrentRepository.appendFullUpdateTopics(fullUpdateTopics)
                    TorrentRepository.commitTopics(subsection, true)
                    println("Обновление подраздела $subsection успешно завершено")
                    incrementFullProgress()
                } catch (e: Exception) {
                    System.err.println("Error: Не получены данные о подразделе № $subsection")
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onTaskFailed()
        }
    }

    fun requestTorrentTopicsData(
        topicsToQuery: Map<Int, List<Any>>,
        fullUpdateTopics: MutableSet<FullUpdateTopic>
    ) {
        val torrentTopicsData =
            keeperRetrofit.getTorrentTopicsData(topicsToQuery.keys.joinToString(","))
                .execute().body()!!.result
        for (topicDataItem in torrentTopicsData) {
            val topicData = topicDataItem.value ?: continue
            // добавляем новую раздачу/раздачу для полного обновления
            fullUpdateTopics.add(
                FullUpdateTopic(
                    topicDataItem.key,
                    topicData.forumId,
                    topicData.topicTitle,
                    topicData.infoHash,
                    topicData.seeders,
                    topicData.size,
                    topicData.torStatus,
                    topicData.regTime,
                    1,
                    0,
                    topicsToQuery[topicDataItem.key]!![4] as Int
                )
            )
        }
    }

    fun updateTopicsFromClient() {
        // TODO: 11.11.2022 всё вроде работает, но очень странно, что вот так наскоком всё получилось
        //  проверить, что точно всё правильно, и никакие пограничные случаи не пропущены
        //  полоса прогресса не доходит до конца, с большой вероятностью теряется часть раздач

        val torrentClients = Settings.getTorrentClients()
        TorrentRepository.createTempUntrackedTopics()
        setFullText("Получение раздач из ${torrentClients.size.pluralForum("торрент-клиента", "торрент-клиентов")}")
        setFullProgress(0, torrentClients.size)
        for (torrentClientItem in torrentClients) {
            val torrentClient = torrentClientItem.value
            setCurrentText("Получение раздач из клиента ${torrentClientItem.value.name}")
            setCurrentProgress(-1)
            if (cancelTaskIfRequested {
                    TorrentRepository.commitUntrackedTopics(false)
                }) return
            torrentClient.auth()
            val torrents = torrentClient.getTorrents()
            if (cancelTaskIfRequested {
                    TorrentRepository.commitUntrackedTopics(false)
                }) return
            setCurrentText(
                "Обработка ${torrents.size.pluralForum("раздачи", "раздач")}" +
                        " из клиента ${torrentClient.name}"
            )
            setCurrentProgress(0, torrents.size)
            TorrentRepository.createTempClients()
            val rutrackerTopicsFromClient = ArrayList<TorrentClientTorrent>()
            val noDbTopics = HashMap<Int, String>()
            val untrackedTopicsIds = ArrayList<Int>()
            val updatedTopicsHashes = HashMap<String, String>()    // старый и новый хэши
            val keepingSubsectionsString = Settings.node("sections")["subsections", ""].unquote()
            // берём пачками, т.к. от нескольких десятков тысяч торрентов может кончиться память
            val packSize = 1000
            var topicInfoRequestLimit = -1
            for (i in torrents.indices step packSize) {
                val topicsFromDb = TorrentRepository.getTopicIdsByHashes(torrents, i, packSize)
                val currentTorrentsCount = min(torrents.size - i, packSize)
                var countDiff = currentTorrentsCount - topicsFromDb.size
                for (j in i until i + currentTorrentsCount) {
                    // если нашли все хэши, которых нет в БД, можно добавить остатки в список не проверяя
                    // TODO: 11.11.2022 ломается обработка "хвоста"
//                if (countDiff == 0) {
//                    println(i + currentTorrentsCount - j)
//                    TorrentRepository.appendTorrentsFromClient(torrents, j, i + currentTorrentsCount)
//                    break
//                }
                    val currentTorrent = torrents[j]
                    // проверяем хэши
                    if (currentTorrent.hash !in topicsFromDb.values) {
                        countDiff--
                        // хэша нет в таблице, попробуем найти по теме
                        if (currentTorrent.topicId == TorrentClientTorrent.TOPIC_NEED_QUERY) {
                            // запрашиваем тему если её нет
                            currentTorrent.topicId = torrentClient.getTorrentTopicId(currentTorrent.hash)
                        }
                        if (currentTorrent.topicId != TorrentClientTorrent.TOPIC_THIRD_PARTY) {
                            // комментарий содержит тему, но хэша нет в базе
                            noDbTopics[currentTorrent.topicId] = currentTorrent.hash

                            // TODO: 09.11.2022 обработка случая, когда раздача закрыта
                        } else {
                            // тема не с рутрекера
                            // TODO: 10.11.2022 и что с ней делать?
                            incrementCurrentProgress()
                        }
                    } else {
                        // хэш есть, всё окей
                        rutrackerTopicsFromClient.add(currentTorrent)
                        incrementCurrentProgress()
                    }
                }
                if (noDbTopics.size == packSize || i + currentTorrentsCount == torrents.size && noDbTopics.isNotEmpty()) {
                    // хэши раздач, которые находятся в хранимых подразделах
                    val keepingUpdatedTopics = TorrentRepository.getTopicHashesByIdsInSubsections(
                        noDbTopics.keys,
                        keepingSubsectionsString
                    )
                    var noDbTopicsProcessed = 0
                    for (noDbTopicEntry in noDbTopics) {
                        if (noDbTopicEntry.key in keepingUpdatedTopics.keys) {
                            // айдишник есть в хранимых подразделах, значит обновился хэш
                            // просто добавляем новый хэш в таблицу
                            updatedTopicsHashes[noDbTopicEntry.value] = keepingUpdatedTopics[noDbTopicEntry.key]!!
                            incrementCurrentProgress()
                        } else {
                            // айдишника нет в хранимых подразделах, значит из другого раздела
                            untrackedTopicsIds.add(noDbTopicEntry.key)
                            if (topicInfoRequestLimit < 0)
                                topicInfoRequestLimit = keeperRetrofit.getLimit().execute().body()?.limit ?: 100
                        }
                        if (untrackedTopicsIds.size == topicInfoRequestLimit || ++noDbTopicsProcessed == noDbTopics.size && untrackedTopicsIds.isNotEmpty()) {
                            if (cancelTaskIfRequested {
                                    TorrentRepository.commitTorrentsFromClient(torrentClientItem.key, false)
                                    TorrentRepository.updateClientsUpdated(
                                        updatedTopicsHashes,
                                        torrentClientItem.key,
                                        false
                                    )
                                    TorrentRepository.commitUntrackedTopics(false)
                                }) return
                            val untrackedTopicsData =
                                keeperRetrofit.getTorrentTopicsData(untrackedTopicsIds.joinToString(","))
                                    .execute().body()!!.result
                            incrementCurrentProgress(untrackedTopicsIds.size)
                            untrackedTopicsIds.clear()
                            TorrentRepository.appendUntrackedTopics(untrackedTopicsData)
                            for (untrackedTopic in untrackedTopicsData) {
                                untrackedTopic.value?.infoHash?.let { untrackedTopicHash ->
                                    noDbTopics[untrackedTopic.key]?.let { noDbTopicHash ->
                                        if (noDbTopicHash != untrackedTopicHash)
                                        // тема не только не отслеживается, но ещё и обновилась, ужас!
                                            updatedTopicsHashes[noDbTopicHash] = untrackedTopicHash
                                    }
                                }
                            }
                        }
                    }
                    noDbTopics.clear()
                }
                TorrentRepository.appendTorrentsFromClient(rutrackerTopicsFromClient)
                rutrackerTopicsFromClient.clear()
            }
            // TODO: 11.11.2022 обрабатывать "хвосты", которые меньше размера пачки
            TorrentRepository.commitTorrentsFromClient(torrentClientItem.key, true)
            TorrentRepository.updateClientsUpdated(updatedTopicsHashes, torrentClientItem.key, true)
        }
        TorrentRepository.commitUntrackedTopics(true)
    }
}