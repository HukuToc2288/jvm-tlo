package api

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import utils.Settings
import utils.unquote
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.*

val keeperCookieJar = SimpleCookieJar()

var keeperRetrofit = createKeeperApi()
    private set

private fun createKeeperApi(): KeeperApi {
    val clientBuilder = OkHttpClient.Builder()
        .addInterceptor(UserAgentInterceptor("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36"))
        .addInterceptor(ApiKeyInterceptor(Settings.node("torrent-tracker")["api_key",""]))
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        .cookieJar(keeperCookieJar)
    val proxySettings = Settings.node("proxy")

    Settings.node("proxy").let {
        if (proxySettings["activate_api", "0"].unquote() == "1") {
            val proxyType =
                if (it["type", "HTTP"].unquote()?.uppercase(Locale.getDefault())?.contains("SOCKS") == true) {
                    Proxy.Type.SOCKS
                } else {
                    Proxy.Type.HTTP
                }
            val proxy = Proxy(
                proxyType, InetSocketAddress(
                    it["hostname", ""].unquote(),
                    it["port", ""].unquote()?.toInt() ?: 0
                )
            )
            clientBuilder.proxy(proxy)
        }
    }
    return Retrofit.Builder()
        .baseUrl("https://api.t-ru.org/")
        .addConverterFactory(JacksonConverterFactory.create(ObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false)
        }))
        .client(clientBuilder.build())
        .build()
        .create(KeeperApi::class.java)

}

fun rebuildKeeperApi() {
    keeperRetrofit = createKeeperApi()
}