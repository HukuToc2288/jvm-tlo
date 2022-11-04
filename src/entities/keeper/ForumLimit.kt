package entities.keeper

import com.fasterxml.jackson.annotation.JsonProperty

class ForumLimit(
    @JsonProperty("result") result: HashMap<String, Int>
) {
    val limit = result["limit"]
}