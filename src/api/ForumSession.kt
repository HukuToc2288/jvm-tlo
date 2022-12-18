package api

import okhttp3.Cookie
import retrofit2.Response
import utils.ConfigRepository

object ForumSession {
    const val COOKIE_NAME = "bb_session"

    fun hasSession(): Boolean {
        if (forumCookieJar.cookieStore.containsKey(COOKIE_NAME))
            return true
        return load()
    }

    fun needAuth(response: Response<out Any>): Boolean {
        val needAuth = response.raw().request().url().encodedPath().startsWith("/forum/login.php")
        if (needAuth) {
            // выкидываем протухшую печеньку
            reset()
        }
        return needAuth
    }

    fun reset() {
        forumCookieJar.cookieStore.remove(COOKIE_NAME)
        save()
    }

    fun load(): Boolean {
        ConfigRepository.trackerConfig.sessionCookie ?: return false
        forumCookieJar.cookieStore[COOKIE_NAME] =
            Cookie.Builder()
                .name(COOKIE_NAME)
                .value(ConfigRepository.trackerConfig.sessionCookie!!)
                .domain("example.com")
                .build()
        return true
    }

    fun save() {
        ConfigRepository.trackerConfig.sessionCookie = forumCookieJar.cookieStore[COOKIE_NAME]?.value()
    }
}