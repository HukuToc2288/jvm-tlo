package api

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

val forumCookieJar = SimpleCookieJar()

val forumRetrofit = Retrofit.Builder()
    .baseUrl("https://rutracker.org/forum/")
    .addConverterFactory(ScalarsConverterFactory.create())
    .client(
        OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36"))
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .cookieJar(forumCookieJar)
            .build()
    )
    .build()
    .create(ForumApi::class.java)