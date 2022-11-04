package entities.keeper

import com.fasterxml.jackson.annotation.JsonProperty

class ForumTopicsInfo(
    @JsonProperty("total_size_bytes") val totalSize: Long,
    @JsonProperty("result") val result: HashMap<Int,List<Any>>
) {
}