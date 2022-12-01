package api

import entities.config.ProxyConfigProxy
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import utils.ConfigRepository
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.*

// TODO: 03.11.2022 SOCKS4 прокси не работают, и с большой вероятностью не работают HTTPS
// TODO: 03.11.2022 прокси с авторизацией

val forumCookieJar = SingleUrlCookieJar()

var forumRetrofit = createForumApi()
    private set

private fun createForumApi(): ForumApi {
    val clientBuilder = OkHttpClient.Builder()
        .addInterceptor(UserAgentInterceptor("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36"))
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .cookieJar(forumCookieJar)

    with(ConfigRepository.proxyConfig) {
        if (proxyForum) {
            // TODO: 01.12.2022 поддержка нескольких прокси
            val selectedProxy = proxies[0]
            val proxyType =
                if (selectedProxy.type == ProxyConfigProxy.ProxyType.SOCKS5) {
                    Proxy.Type.SOCKS
                } else {
                    Proxy.Type.HTTP
                }
            val proxy = Proxy(
                proxyType, InetSocketAddress(
                    selectedProxy.hostname,
                    selectedProxy.port
                )
            )
            clientBuilder.proxy(proxy)
        }
    }
    return Retrofit.Builder()
        .baseUrl("https://rutracker.org/forum/")
        .addConverterFactory(ScalarsConverterFactory.create())
        .client(clientBuilder.build())
        .build()
        .create(ForumApi::class.java)

}

fun rebuildForumApi() {
    forumRetrofit = createForumApi()
}