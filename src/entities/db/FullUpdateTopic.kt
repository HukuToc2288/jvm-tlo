package entities.db

class FullUpdateTopic(
    val id: Int,
    val forumId: Int,
    val title: String,
    val hash: String,
    val seedsCount: Int,
    val size: Long,
    val status: Int,
    val date: Int,
    val updatesCount: Int,
    val daysUpdate: Int,
    val priority: Int
)