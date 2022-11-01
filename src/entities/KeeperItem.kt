package entities

import kotlin.test.todo


class KeeperItem(
    val name: String,
    var status: Status
) {
    override fun toString(): String {
        // TODO: 01.11.2022 make it more beautiful
        return when (status) {
            Status.DOWNLOADING -> "(D) "
            Status.KEEPING -> "(K) "
            Status.SEEDING -> "(S) "
            Status.FULL -> "(F) "
        } + name
    }

    override fun equals(other: Any?): Boolean {
        if (other === this)
            return true
        if (other !is KeeperItem)
            return false
        else
            return other.name == this.name
    }

    fun updateStatus(newStatus: Status) {
        when (status) {
            Status.FULL -> return   // самый приоритетный статус
            Status.DOWNLOADING -> status = newStatus // хранитель докачал, но не обновил отчёт
            Status.SEEDING -> {
                if (newStatus == Status.KEEPING)    // отчёт и фактическая раздача пришли в разных строках
                    status = Status.FULL
            }
            Status.KEEPING -> {
                if (newStatus == Status.SEEDING)    // отчёт и фактическая раздача пришли в разных строках
                    status = Status.FULL

            }
        }
    }

    enum class Status {
        DOWNLOADING,        // Есть в отчёте и качает
        SEEDING,            // Нет в отчёте, но раздаёт
        KEEPING,            // Есть в отчёте, но не раздаёт
        FULL,    // Есть в отчёте и раздаёт
    }
}