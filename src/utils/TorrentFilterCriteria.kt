package utils

import java.util.*

class TorrentFilterCriteria(
    val sortAscending: Boolean,
    val sortOrder: SortOrder,

    val statuses: List<ForumStatus>,
    val priorities: List<Priority>,

    val minAverageSeeds: Double,
    val maxAverageSeeds: Double,
    // FIXME: 31.10.2022 можно отхватить люлей на часовых поясах
    val registerDate: Date,

    val titleSearchText: String,
    val keeperSearchText: String,

    // TODO: 31.10.2022 add other criteria
) {
    enum class SortOrder {
        NAME, SIZE, SEEDS, DATE
    }

    enum class ForumStatus(val code: Int) {
        NOT_VERIFIED(0),
        VERIFIED(2),
        NOT_FORMALIZED(3),
        SUSPICIOUS(8),
        TEMPORARY(10);

        override fun toString(): String {
            return code.toString()
        }
    }

    enum class Priority(val code: Int) {
        LOW(0),
        NORMAL(1),
        HIGH(2);

        override fun toString(): String {
            return code.toString()
        }
    }
}