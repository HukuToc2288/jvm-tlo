package api.torrentclients


import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface QbittorrentApi {

    @POST("v2/auth/login")
    @FormUrlEncoded
    fun login(@Field("username") username: String, @Field("password") password: String): Call<String>

    @GET("v2/app/version")
    fun version(): Call<String>
}