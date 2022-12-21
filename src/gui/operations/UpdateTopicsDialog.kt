package gui.operations

import api.ForumSession
import api.forumRetrofit
import api.keeperRetrofit
import db.TorrentRepository
import entities.db.FullUpdateTopic
import entities.db.KeeperReportItem
import entities.db.SeedsUpdateTopic
import entities.torrentclient.TorrentClientTorrent
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import retrofit2.Call
import utils.ConfigRepository
import utils.LogUtils
import utils.pluralForum
import java.awt.Frame
import java.io.IOException
import java.text.DateFormatSymbols
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.min


class UpdateTopicsDialog(frame: Frame?) : OperationDialog(frame, "Обновление списка раздач") {

    private val forumDateFormat = SimpleDateFormat("dd-MMM-yy hh:mm").apply {
        val ru = Locale("ru")
        locale = ru
        val symbols = DateFormatSymbols.getInstance(ru)
        symbols.months = arrayOf("Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек")
        dateFormatSymbols = symbols
    }

    private val packSize = 1000

    override fun doTask() {
        val updateTorrentsThread = Thread {
            try {
                updateSubsections()
                if (cancelTaskIfRequested { })
                    return@Thread
                updateKeepersReports()
                if (cancelTaskIfRequested { })
                    return@Thread
                updateTopicsFromClient()
                if (cancelTaskIfRequested { })
                    return@Thread
                onTaskSuccess()
            } catch (e: Exception) {
                LogUtils.e("Ошибка обновления списка раздач: " + e.localizedMessage)
                onTaskFailed()
            }
        }
        updateTorrentsThread.start()
    }

    fun updateSubsections() {
        setFullProgress(0, ConfigRepository.subsections.size)
        setFullText("Обновляются темы в ${ConfigRepository.subsections.size.pluralForum("подразделе", "подразделах")}")
        setCurrentText("Получение данных с сервера...")
        if (cancelTaskIfRequested {})
            return
        // TODO: 13.11.2022 обновлять хранителей с форума
        val keepersMap = try {
            responseOrThrow(keeperRetrofit.keepersUserData(), "Не получены ID хранителей").keepers
        } catch (e: Exception) {
            showNonCriticalError()
            LogUtils.w(e.localizedMessage)
            emptyMap()
        }
        val torrentStatus = setOf(0, 2, 3, 8, 10)
        if (cancelTaskIfRequested {})
            return
        val limit = try {
            responseOrThrow(keeperRetrofit.getLimit(), "Не получены лимиты на запрос").limit
        } catch (e: Exception) {
            showNonCriticalError()
            LogUtils.w(e.localizedMessage)
            100
        }
        // сиды-хранители не привязаны к подразделам, обновляем их в конце
        val keepersSeeders = HashSet<Pair<Int, String>>() // номер темы и ник хранителя
        val appendKeepersSeeders = {
            TorrentRepository.appendKeepersSeeders(keepersSeeders)
            keepersSeeders.clear()
        }
        TorrentRepository.createTempKeepersSeeders()
        for (subsectionEntry in ConfigRepository.subsections) {
            val subsection = subsectionEntry.id
            if (!TorrentRepository.shouldUpdate(subsection)) {
                LogUtils.i("Не требуется обновление для подраздела $subsection")
                continue
            }
            // TODO: 21.12.2022 переделать косяки с петлёй
            setCurrentText("Обновление подраздела $subsection")
            val fullUpdateTopics = HashSet<FullUpdateTopic>()
            val seedsUpdateTopics = HashSet<SeedsUpdateTopic>()
            val topicsToQuery = HashMap<Int, List<Any>>()

            val requestTorrentTopics = {
                requestTorrentTopicsData(topicsToQuery, fullUpdateTopics)
                incrementCurrentProgress(topicsToQuery.size)
                topicsToQuery.clear()
            }

            try {
                // запрашиваем инфу о раздачах в подразделе с сервера
                // даты регистрации с клиента
                val existingTopicsDates = TorrentRepository.getTopicsRegistrationDate(subsection)
                if (cancelTaskIfRequested {})
                    return
                val forumTorrents = responseOrThrow(
                    keeperRetrofit.getForumTorrents(subsection),
                    "не получена информация о раздачах"
                ).result
                setCurrentProgress(0, forumTorrents.size)

                var processedForumTorrents = 0
                for (forumTorrent in forumTorrents) {
                    // основной блок обработки
                    processedForumTorrents++
                    if (forumTorrent.value[0] !in torrentStatus)
                        continue
                    if (forumTorrent.value[5] is List<*> && (forumTorrent.value[5] as List<*>).isNotEmpty()) {
                        for (keeperId in forumTorrent.value[5] as List<*>) {
                            if (keeperId !is Int || keeperId == ConfigRepository.trackerConfig.keeperId)
                                continue
                            if (keepersMap.containsKey(keeperId))
                                keepersSeeders.add(forumTorrent.key to keepersMap[keeperId]!!)

                            // добавление сидов-хранителей по размеру пачки
                            if (keepersSeeders.size == packSize) {
                                appendKeepersSeeders()
                            }
                        }
                    }
                    val inDb = forumTorrent.key in existingTopicsDates.keys
                    val needToQuery =
                        !inDb || forumTorrent.value[2] != existingTopicsDates[forumTorrent.key]
                    if (needToQuery) {
                        // нужно запросить инфу о раздаче
                        // добавляем тему в очередь
                        topicsToQuery.put(forumTorrent.key, forumTorrent.value)

                        // запрос и добавление тем на полное обновление по лимитам
                        if (topicsToQuery.size == limit) {
                            if (cancelTaskIfRequested {
                                    // добавим то, что уже обновили
                                    TorrentRepository.appendSeedsUpdatedTopics(seedsUpdateTopics)
                                    TorrentRepository.appendFullUpdateTopics(fullUpdateTopics)
                                    TorrentRepository.commitTopics(subsection, false)
                                    TorrentRepository.commitKeepersSeeders(false)
                                })
                                return
                            requestTorrentTopics()
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
                if (keepersSeeders.isNotEmpty())
                    appendKeepersSeeders()
                if (topicsToQuery.isNotEmpty())
                    requestTorrentTopics()

                TorrentRepository.appendSeedsUpdatedTopics(seedsUpdateTopics)
                TorrentRepository.appendFullUpdateTopics(fullUpdateTopics)
                TorrentRepository.commitTopics(subsection, true)

                LogUtils.i("Обновление подраздела $subsection успешно завершено")
            } catch (e: Exception) {
                LogUtils.w("Не удалось обновить подраздел ${subsection}: " + e.localizedMessage)
                showNonCriticalError()
            } finally {
                incrementFullProgress()
            }
        }
        if (keepersSeeders.isNotEmpty())
            appendKeepersSeeders()
        TorrentRepository.commitKeepersSeeders(true)
    }

    // TODO: 21.12.2022 расставить точки отмены операции и обновлять прогресс
    fun updateKeepersReports() {
        val keepersReports = ArrayList<KeeperReportItem>()
        val appendKeepers = {
            TorrentRepository.appendKeepers(keepersReports)
            keepersReports.clear()
        }
        TorrentRepository.createTempKeepers()
        // получаем отчёты хранителей
        for (subsectionEntry in ConfigRepository.subsections) {
            val subsection = subsectionEntry.id
            // ошибки авторизации считаем критическими
            if (!ForumSession.hasSession()) {
                throw Exception("Вы не авторизованы на форуме. Авторизуйтесь в настройках")
            }
            val searchResponse = forumRetrofit.searchReportsTopic(subsectionEntry.title).execute()
            if (ForumSession.needAuth(searchResponse)) {
                throw Exception("Сессия устарела. Вам нужно заново авторизоваться в настройках")
            }
            try {
                val reportsTopicsElements = Jsoup.parse(searchResponse.body()!!).select("a[href].topictitle")
                if (reportsTopicsElements.isEmpty()) {
                    throw Exception("Не найдены отчёты для подраздела $subsection")
                }
                if (reportsTopicsElements.size > 1){
                    showNonCriticalAndLog("Для подраздела ${subsection} получено больше одной темы с отчётом." +
                            " Этого не должно происходить, заведите issue")
                }
                for (element in reportsTopicsElements) {
                    var reportsTopicLink = element.attr("href") ?: continue
                    var reportsPage: Document
                    do {
                        reportsPage = Jsoup.parse(forumRetrofit.loadPage(reportsTopicLink).execute().body()!!)
                        val postElements = reportsPage.select("tbody[id^=post_]")
                        for (postElement in postElements) {
                            val keeperNick = postElement.selectFirst("p.nick")?.select("a")?.text()
                            if (keeperNick == null) {
                                showNonCriticalAndLog("Пост ${postElement.id()}: Не получен ник хранителя")
                                continue
                            }
                            if (keeperNick == "StatsBot")
                                continue
                            val updateDateString = try {
                                // пробуем получить дату редактирования
                                val postedSinceText = postElement.selectFirst("span.posted_since")?.text()
                                postedSinceText?.substring(postedSinceText.indexOf("ред. ") + 5,postedSinceText.length-1)
                                    ?: throw NullPointerException()
                            } catch (e: Exception) {
                                // нет даты редактирования
                                postElement.selectFirst("a.p-link")?.text()
                            }
                            if (updateDateString == null) {
                                showNonCriticalAndLog("Пост ${postElement.id()}: Не получена дата сообщения")
                                continue
                            }
                            val updateDate = try {
                                forumDateFormat.parse(updateDateString)
                            } catch (e: ParseException) {
                                showNonCriticalAndLog("Пост ${postElement.id()}: Неправильный формат даты: $updateDateString")
                                continue
                            }.time / 1000
                            // теперь можно извлекать ссылки
                            val topicsLinksElements = postElement.select("a.postLink")
                            for (topicLinkElement in topicsLinksElements) {
                                var topicId: Int? = null
                                var completed: Boolean? = null
                                if (topicLinkElement.attr("href").matches(Regex("viewtopic.php\\?t=\\d+$"))) {
                                    topicId = topicLinkElement.attr("href").replace(Regex(".*?(\\d*)$"), "\$1").toIntOrNull()
                                    completed = true
                                } else if (topicLinkElement.attr("href").matches(Regex("viewtopic.php\\?t=\\d+#dl\$"))) {
                                    topicId =
                                        topicLinkElement.attr("href").replace(Regex(".*?(\\d*)#dl\$"), "\$1").toIntOrNull()
                                    completed = false
                                }
                                if (topicId == null || completed == null)
                                    continue
                                keepersReports.add(
                                    KeeperReportItem(
                                        topicId,
                                        keeperNick,
                                        updateDate.toInt(),
                                        completed
                                    )
                                )
                                if (keepersReports.size == packSize)
                                    appendKeepers()
                            }
                        }
                        // TODO: 19.12.2022 что-то делаем со страницей
                        println("Открыта страница с отчётами $reportsTopicLink")
                    } while (
                        reportsPage.selectFirst("a.pg:matchesOwn(След.)")?.attr("href").let {
                            if (it != null) {
                                reportsTopicLink = it
                                true
                            } else {
                                false
                            }
                        })
                }
            } catch (e: Exception){
                showNonCriticalAndLog("Не удалось обновить отчёты хранителей для подраздела $subsection: ${e.localizedMessage}")
            }
        }
        if (keepersReports.isNotEmpty())
            appendKeepers()
        TorrentRepository.commitKeepers(true)
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
        val torrentClients = ConfigRepository.torrentClients
        TorrentRepository.createTempUntrackedTopics()
        TorrentRepository.createTempUnregisteredTopics()
        setFullText("Получение раздач из ${torrentClients.size.pluralForum("торрент-клиента", "торрент-клиентов")}")
        setFullProgress(0, torrentClients.size)
        val updatedTopicsHashes = HashMap<String, String>()    // старый и новый хэши
        for (torrentClientItem in torrentClients) {
            val torrentClient = torrentClientItem.value
            try {
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
                            ConfigRepository.subsections
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
                                        TorrentRepository.updateTopicsUpdated(updatedTopicsHashes, false)
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
                LogUtils.w("Не получены раздачи от торрент-клиента ${torrentClient.name}: " + e.localizedMessage)
                showNonCriticalError()
            } finally {
                incrementFullProgress()
            }
        }
        TorrentRepository.commitUntrackedTopics(true)
        TorrentRepository.commitUnregisteredTopics(true)
        TorrentRepository.updateTopicsUpdated(updatedTopicsHashes, true)
    }

    fun showNonCriticalAndLog(message: String) {
        showNonCriticalError()
        LogUtils.w(message)
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

    fun <T> responseOrThrow(call: Call<T>, errorMessage: String): T {
        val response = try {
            call.execute()
        } catch (e: IOException) {
            throw IOException(errorMessage + ": " + e.localizedMessage)
        }
        if (response.body() == null) {
            throw NullPointerException("$errorMessage: ${response.message()}")
        } else {
            return response.body()!!
        }
    }
}