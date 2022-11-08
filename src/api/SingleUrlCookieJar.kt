package api

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class SingleUrlCookieJar : CookieJar {
    // банка печенья, которая складывает все куки в общее хранилище
    // для случаев, когда все запросы идут к одному домену, как в API
    private val cookieStore = ArrayList<Cookie>()
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore.addAll(cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore
    }

    fun hasCookie(cookieName: String): Boolean {
        for (cookie in cookieStore) {
            if (cookieName == cookie.name())
                return true
        }
        return false
    }
}