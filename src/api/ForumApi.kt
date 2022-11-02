package api

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface ForumApi {

    @FormUrlEncoded
    @POST("login.php")
    fun login(
        @Field("login_username") username: String,
        @Field("login_password") password: String,
        @Field("cap_sid") captchaId: String? = null,
        @FieldMap captcha: Map<String,String> = emptyMap(),
        @Field("login", encoded = true) login: String = "%C2%F5%EE%E4",
    ): Call<String>
}