package api

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class SingleUrlCookieJar : CookieJar {
    // банка печенья, которая складывает все куки в общее хранилище
    // для случаев, когда все запросы идут к одному домену, как в API
    val cookieStore = HashMap<String,Cookie>()
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (cookie in cookies){
            cookieStore[cookie.name()] = cookie
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore.values.toList()
    }
}