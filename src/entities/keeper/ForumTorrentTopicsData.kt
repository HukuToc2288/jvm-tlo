package entities.keeper

import com.fasterxml.jackson.annotation.JsonProperty

class ForumTorrentTopicsData(
    @JsonProperty("result") val result: HashMap<Int, TopicData?>
) {
    class TopicData(
        @JsonProperty("info_hash") var infoHash: String,
        @JsonProperty("forum_id") var forumId: Int,
        @JsonProperty("poster_id") var posterId: Int,
        @JsonProperty("size") var size: Long,
        @JsonProperty("reg_time") var regTime: Int,
        @JsonProperty("tor_status") var torStatus: Int,
        @JsonProperty("seeders") var seeders: Int,
        @JsonProperty("topic_title") var topicTitle: String,
        @JsonProperty("seeder_last_seen") var seederLastSeen: Int,
        @JsonProperty("dl_count") var dlCount: Int

    )
}