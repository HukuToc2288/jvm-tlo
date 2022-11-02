package api

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class SimpleCookieJar: CookieJar {
    private val cookieStore = HashMap<HttpUrl, List<Cookie>>()
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore[url] = cookies
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies = cookieStore[url]
        return cookies ?: ArrayList()
    }

    fun hasCookie(cookieName: String): Boolean {
        for (item in cookieStore) {
            for (cookie in item.value) {
                if (cookieName == cookie.name())
                    return true
            }
        }
        return false
    }
}