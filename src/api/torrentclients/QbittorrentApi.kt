package api.torrentclients


import entities.torrentclient.QbittorrentInfoEntry
import entities.torrentclient.QbittorrentTorrentProperties
import retrofit2.Call
import retrofit2.http.*

interface QbittorrentApi {

    @POST("v2/auth/login")
    @FormUrlEncoded
    fun login(@Field("username") username: String?, @Field("password") password: String?): Call<String>

    @GET("v2/app/version")
    fun version(): Call<String>

    @GET("v2/torrents/info")
    fun getTorrents(): Call<List<QbittorrentInfoEntry>>

    @GET("v2/torrents/properties")
    fun getTorrentProperties(@Query("hash") hash: String): Call<QbittorrentTorrentProperties>
}