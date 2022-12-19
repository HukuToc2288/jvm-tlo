package api

import retrofit2.Call
import retrofit2.http.*

interface ForumApi {

    @FormUrlEncoded
    @POST("login.php")
    fun login(
        @Field("login_username") username: String,
        @Field("login_password") password: String,
        @Field("cap_sid") captchaId: String? = null,
        @FieldMap captcha: Map<String, String> = emptyMap(),
        @Field("login", encoded = true) login: String = "%C2%F5%EE%E4",
    ): Call<String>

    @GET("/forum/search.php")
    fun searchReportsTopic(
        @Query("nm") topicName: String,
        @Query("f") forumId: Int = 1584
    ): Call<String>

    @GET("tracker.php")
    fun getTracker(): Call<String>

    @GET
    fun loadPage(@Url url: String): Call<String>
}