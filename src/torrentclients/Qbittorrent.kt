package torrentclients

import api.*
import api.torrentclients.QbittorrentApi
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import utils.TorrentClientException

// https://github.com/qbittorrent/qBittorrent/wiki/WebUI-API-(qBittorrent-4.1)
class Qbittorrent(
    baseUrl: String,
    ssl: Boolean,
    val login: String,
    val password: String
) : AbstractTorrentClient() {

    private val cookieJar = SingleUrlCookieJar()
    private val SESSION_COOKIE_NAME = "SID"

    val api = Retrofit.Builder()
        .baseUrl("http${if (ssl) "s" else ""}://$baseUrl/api/")
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(JacksonConverterFactory.create(ObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }))
        .client(
            OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                .cookieJar(cookieJar).build()
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
            if (response.code() == 403) {
                // сессия протухла
                auth()
                return version()
            } else {
                throw TorrentClientException("Ошибка ${response.code()}: ${response.body()}")
            }
        }
        return response.body().toString()
    }
}