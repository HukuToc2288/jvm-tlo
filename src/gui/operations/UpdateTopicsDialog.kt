package gui.operations

import api.keeperRetrofit
import db.TorrentRepository
import entities.db.FullUpdateTopic
import entities.db.SeedsUpdateTopic
import entities.keeper.ForumKeepers
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import utils.Settings
import utils.unquote
import java.awt.Frame

class UpdateTopicsDialog(frame: Frame?) : OperationDialog(frame, "Обновление списка раздач") {

    private val fullTextFormat = "Обработано %s из %s подразделов"

    override fun executeTask() {
        updateSubsections()
    }

    private fun publishFullProgress(processed: Int, full: Int) {
        setFullProgress(processed, full)
        setFullText(fullTextFormat.format(processed, full))
    }

    fun updateSubsections() {
        val updateTorrentsThread = Thread {
            // FIXME: 06.11.2022 сувать всё в один try-catch блок это плохо
            try {
                val subsections =
                    Settings.node("sections")["subsections", ""].unquote()?.split(',') ?: emptyList<String>()
                publishFullProgress(0, subsections.size)
                setCurrentText("Получение данных с сервера...")
                if (cancelTaskIfRequested {})
                    return@Thread
                val keepersMap = keeperRetrofit.keepersUserData().execute().body()!!

                val torrentStatus = setOf(0, 2, 3, 8, 10)
                if (cancelTaskIfRequested {})
                    return@Thread
                val limit = keeperRetrofit.getLimit().execute().body()!!.limit
                for (i in subsections.indices) {
                    val subsection = subsections[i].toInt()
                    if (!TorrentRepository.shouldUpdate(subsection)) {
                        println("Notice: Не требуется обновление для подраздела № $subsection")
                        continue
                    }
                    setCurrentText("Обновление раздела $subsection")
                    try {
                        // запрашиваем инфу о раздачах в подразделе с сервера
                        if (cancelTaskIfRequested {})
                            return@Thread
                        val forumTorrents =
                            keeperRetrofit.getForumTorrents(subsection).execute().body()!!.result
                        var processedTorrents = 0
                        setCurrentProgress(processedTorrents, forumTorrents.size)
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
                                        return@Thread
                                    requestTorrentTopicsData(topicsToQuery, fullUpdateTopics)
                                    processedTorrents += limit
                                    setCurrentProgress(processedTorrents, forumTorrents.size)
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
                                setCurrentProgress(++processedTorrents, forumTorrents.size)
                            }
                        }
                        if (topicsToQuery.isNotEmpty()) {
                            if (cancelTaskIfRequested {
                                    // добавим то, что уже обновили
                                    TorrentRepository.appendSeedsUpdatedTopics(seedsUpdateTopics)
                                    TorrentRepository.appendFullUpdateTopics(fullUpdateTopics)
                                    TorrentRepository.commitTopics(subsection, false)
                                })
                                return@Thread
                            requestTorrentTopicsData(topicsToQuery, fullUpdateTopics)
                            processedTorrents += topicsToQuery.size
                            setCurrentProgress(processedTorrents, forumTorrents.size)
                            topicsToQuery.clear()
                        }
                        TorrentRepository.appendSeedsUpdatedTopics(seedsUpdateTopics)
                        TorrentRepository.appendFullUpdateTopics(fullUpdateTopics)
                        TorrentRepository.commitTopics(subsection, true)
                        println("Обновление подраздела $subsection успешно завершено")
                        publishFullProgress(i + 1, subsections.size)
                    } catch (e: Exception) {
                        System.err.println("Error: Не получены данные о подразделе № $subsection")
                        e.printStackTrace()
                    }
                }
                onTaskCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                onTaskFailed()
            }
        }
        updateTorrentsThread.start()
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
}