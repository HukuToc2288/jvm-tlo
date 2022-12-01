package torrentclients

import api.*
import api.torrentclients.QbittorrentApi
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import entities.torrentclient.TorrentClientTorrent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import utils.TorrentClientException
import java.util.*
import java.util.concurrent.TimeUnit

// https://github.com/qbittorrent/qBittorrent/wiki/WebUI-API-(qBittorrent-4.1)
class Qbittorrent(
    name: String,
    baseUrl: String,
    login: String?,
    password: String?
) : AbstractTorrentClient(name, TorrentClientTypes.QBITTORRENT, baseUrl, login, password) {

    private val cookieJar = SingleUrlCookieJar()
    private val SESSION_COOKIE_NAME = "SID"

    private val api = Retrofit.Builder()
        .baseUrl("${baseUrl}api/")
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(JacksonConverterFactory.create(ObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }))
        .client(
            OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                .cookieJar(cookieJar)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
        )
        .build()
        .create(QbittorrentApi::class.java)

    override fun auth() {
        val response = api.login(login, password).execute()
        if (response.code() != 200)
            throw TorrentClientException("Ошибка ${response.code()}: ${response.body()}")
        if (!cookieJar.hasCookie(SESSION_COOKIE_NAME)) {
            throw TorrentClientException("Не получено cookie сессии: ${response.body()}")
        }
    }

    override fun version(): String {
        if (!cookieJar.hasCookie(SESSION_COOKIE_NAME))
            auth()  // нет сессии
        val response = api.version().execute()
        if (response.code() != 200) {
            throw TorrentClientException("Ошибка ${response.code()}: ${response.body()}")
        }
        return response.body().toString()
    }

    override fun getTorrents(): List<TorrentClientTorrent> {
        val response = api.getTorrents().execute()
        if (response.code() != 200)
            throw TorrentClientException("Ошибка ${response.code()}: ${response.body()}")
        val torrentsFromClient = response.body()!!
        val torrentList = List(torrentsFromClient.size) {
            val torrent = torrentsFromClient[it]
            TorrentClientTorrent(
                torrent.hash.uppercase(Locale.US),
                torrent.amountLeft == 0L,
                TorrentClientTorrent.TOPIC_NEED_QUERY
            )
        }
        return torrentList
    }

    override fun getTorrentTopicId(hash: String): Int {
        val response = api.getTorrentProperties(hash).execute()
        if (response.code() != 200)
            throw TorrentClientException("Ошибка ${response.code()}: ${response.body()}")
        val comment = response.body()!!.comment
        val found = rutrackerCommentRegex.find(comment) ?: return TorrentClientTorrent.TOPIC_THIRD_PARTY
        return try {
            comment.substring(found.range.last+1).toInt()
        } catch (e: NumberFormatException) {
            TorrentClientTorrent.TOPIC_THIRD_PARTY
        }
    }
}