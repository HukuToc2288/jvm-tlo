package api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import utils.Settings
import utils.unquote
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.*

// TODO: 03.11.2022 SOCKS4 прокси не работают, и с большой вероятностью не работают HTTPS
// TODO: 03.11.2022 прокси с авторизацией

val forumCookieJar = SingleUrlCookieJar()

var forumRetrofit = createForumApi()
    private set

private fun createForumApi(): ForumApi {
    val clientBuildr = OkHttpClient.Builder()
        .addInterceptor(UserAgentInterceptor("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36"))
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .cookieJar(forumCookieJar)
    val proxySettings = Settings.node("proxy")

    Settings.node("proxy").let {
        if (proxySettings["activate_forum", "0"].unquote() == "1") {
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
            clientBuildr.proxy(proxy)
        }
    }
    return Retrofit.Builder()
        .baseUrl("https://rutracker.org/forum/")
        .addConverterFactory(ScalarsConverterFactory.create())
        .client(clientBuildr.build())
        .build()
        .create(ForumApi::class.java)

}

fun rebuildForumApi() {
    forumRetrofit = createForumApi()
}