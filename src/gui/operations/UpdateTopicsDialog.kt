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

import java.util.HashSet


class UpdateTopicsDialog(frame: Frame?) : OperationDialog(frame, "Обновление списка раздач") {


    override fun doTask() {
        val updateTorrentsThread = Thread {
            try {
                updateSubsections()
                if (cancelTaskIfRequested { })
                    return@Thread
                updateTopicsFromClient()
                if (cancelTaskIfRequested { })
                    return@Thread
                onTaskSuccess()
            } catch (e: Exception) {
                onTaskFailed()
                e.printStackTrace()
            }
        }
        updateTorrentsThread.start()
    }

    fun updateSubsections() {
        val subsections =
            Settings.node("sections")["subsections", ""].unquote().split(',')
        setFullProgress(0, subsections.size)
        setFullText("Обновляются темы в ${subsections.size.pluralForum("подраздела", "подразделов")}")
        setCurrentText("Получение данных с сервера...")
        if (cancelTaskIfRequested {})
            return
        // TODO: 13.11.2022 обновлять хранителей
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
            } catch (e: Exception) {
                e.printStackTrace()
                showNonCriticalError()
            } finally {
                incrementFullProgress()
            }
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
        val torrentClients = Settings.getTorrentClients()
        TorrentRepository.createTempUntrackedTopics()
        TorrentRepository.createTempUnregisteredTopics()
        setFullText("Получение раздач из ${torrentClients.size.pluralForum("торрент-клиента", "торрент-клиентов")}")
        setFullProgress(0, torrentClients.size)
        val updatedTopicsHashes = HashMap<String, String>()    // старый и новый хэши
        for (torrentClientItem in torrentClients) {
            try {
                val torrentClient = torrentClientItem.value
                setCurrentText("Получение раздач из клиента ${torrentClientItem.value.name}")
                setCurrentProgress(-1)
                if (cancelTaskIfRequested {
                        TorrentRepository.commitUntrackedTopics(false)
                        TorrentRepository.commitUnregisteredTopics(false)
                    }) return
                torrentClient.auth()
                val torrents = torrentClient.getTorrents()
                if (cancelTaskIfRequested {
                        TorrentRepository.commitUntrackedTopics(false)
                        TorrentRepository.commitUnregisteredTopics(false)
                    }) return
                setCurrentText(
                    "Обработка ${torrents.size.pluralForum("раздачи", "раздач")}" +
                            " из клиента ${torrentClient.name}"
                )
                setCurrentProgress(0, torrents.size)
                TorrentRepository.createTempClients()
                val rutrackerTopicsFromClient = ArrayList<TorrentClientTorrent>()
                // мапа "перевёрнута", т.к. может быть несколько раздач с одной темой, но разными хэшами
                // если конечно господин не использовал эту замечательную программу для поиска обновлённых раздач
                // FIXME: 12.11.2022 в идеале здесь нужна структура, нормально работающая в обе стороны
                val noDbTopics = HashMap<String, Int>()
                val untrackedTopicsIds = ArrayList<Int>()
                val unregisteredTopics = HashMap<String, Int>()    // хэш который в клиенте и номер темы
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
                        if (countDiff == 0) {
                            TorrentRepository.appendTorrentsFromClient(torrents, j, i + currentTorrentsCount)
                            incrementCurrentProgress(i + currentTorrentsCount - j)
                            break
                        }
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
                                noDbTopics[currentTorrent.hash] = currentTorrent.topicId

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
                            noDbTopics.values,
                            keepingSubsectionsString
                        )
                        var noDbTopicsProcessed = 0
                        var yes = 0
                        for (noDbTopicEntry in noDbTopics) {
                            if (noDbTopicEntry.value in keepingUpdatedTopics.keys) {
                                // айдишник есть в хранимых подразделах, значит обновился хэш
                                // просто добавляем новый хэш в таблицу
                                updatedTopicsHashes[noDbTopicEntry.key] =
                                    keepingUpdatedTopics[noDbTopicEntry.value]!!
                                incrementCurrentProgress()
                                yes++
                            } else {
                                // айдишника нет в хранимых подразделах, значит из другого раздела
                                untrackedTopicsIds.add(noDbTopicEntry.value)
                                if (topicInfoRequestLimit < 0)
                                    topicInfoRequestLimit = keeperRetrofit.getLimit().execute().body()?.limit ?: 100
                            }
                            noDbTopicsProcessed++
                            if (untrackedTopicsIds.size == topicInfoRequestLimit || noDbTopicsProcessed == noDbTopics.size && untrackedTopicsIds.isNotEmpty()) {
                                if (cancelTaskIfRequested {
                                        TorrentRepository.commitTorrentsFromClient(torrentClientItem.key, false)
                                        TorrentRepository.updateTopicsUpdated(updatedTopicsHashes,false)
                                        TorrentRepository.commitUntrackedTopics(false)
                                        TorrentRepository.commitUnregisteredTopics(false)
                                    }) return
                                val untrackedTopicsData =
                                    keeperRetrofit.getTorrentTopicsData(untrackedTopicsIds.joinToString(","))
                                        .execute().body()!!.result
                                for (untrackedTopic in untrackedTopicsData) {
                                    if (untrackedTopic.value == null) {
                                        // торрент не зарегистрирован, а значит скорее всего разрегистрирован
                                        val clientHashesOfTopics = noDbTopics.getKeysByValue(untrackedTopic.key)
                                        for (clientHashOfTopic in clientHashesOfTopics) {
                                            if (clientHashOfTopic != untrackedTopic.value?.infoHash) {
                                                // добавляем все хэши от данной темы
                                                unregisteredTopics[clientHashOfTopic] = untrackedTopic.key
                                            }
                                        }
                                        continue
                                    }
                                    untrackedTopic.value?.forumId?.let { untrackedTopicId ->
                                        val clientHashesOfTopics = noDbTopics.getKeysByValue(untrackedTopicId)
                                        untrackedTopic.value?.infoHash?.let { untrackedTopicHash ->
                                            for (clientHashOfTopic in clientHashesOfTopics) {
                                                if (clientHashOfTopic != untrackedTopic.value?.infoHash) {
                                                    // тема не только не отслеживается, но ещё и обновилась, ужас!
                                                    updatedTopicsHashes[clientHashOfTopic] = untrackedTopicHash
                                                }
                                            }
                                        }
                                    }
                                }
                                TorrentRepository.appendUntrackedTopics(untrackedTopicsData)
                                TorrentRepository.appendUnregisteredTopics(unregisteredTopics)
                                incrementCurrentProgress(untrackedTopicsIds.size)
                                untrackedTopicsIds.clear()
                                unregisteredTopics.clear()
                            }
                        }
                        noDbTopics.clear()
                    }
                    TorrentRepository.appendTorrentsFromClient(rutrackerTopicsFromClient)
                    rutrackerTopicsFromClient.clear()
                }
                TorrentRepository.commitTorrentsFromClient(torrentClientItem.key, true)
            } catch (e: Exception) {
                e.printStackTrace()
                showNonCriticalError()
            } finally {
                incrementFullProgress()
            }
        }
        TorrentRepository.commitUntrackedTopics(true)
        TorrentRepository.commitUnregisteredTopics(true)
        TorrentRepository.updateTopicsUpdated(updatedTopicsHashes, true)
    }

    fun <T, E> Map<T, E>.getKeysByValue(value: E): Set<T> {
        val keys: MutableSet<T> = HashSet()
        for (entry in this) {
            if (value == entry.value) {
                keys.add(entry.key)
            }
        }
        return keys
    }
}