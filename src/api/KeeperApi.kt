package api

import entities.keeper.ForumTree
import retrofit2.Call
import okhttp3.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface KeeperApi {

    @GET("v1/static/cat_forum_tree")
    fun catForumTree(): Call<ForumTree>
}