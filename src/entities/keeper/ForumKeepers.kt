package entities.keeper

import com.fasterxml.jackson.annotation.JsonProperty

class ForumKeepers(
    @JsonProperty("result") result: HashMap<Int,List<String>>
){
    // формируем нормальный человеческий список
    val keepers = HashMap<Int,String>().apply {
        for (r in result){
            put(r.key,r.value[0])
        }
    }
}