package entities

import kotlin.test.todo


class KeeperItem(
    val name: String,
    var status: Status
) {
    override fun toString(): String {
        return name
    }

    fun coloredName():String {
        return "<font color='" + when (status) {
            Status.DOWNLOADING -> "red"
            Status.KEEPING -> "yellow"
            Status.SEEDING -> "aqua"
            Status.FULL -> "lime"
        } +"'>"+ name + "</font>"
    }

    override fun equals(other: Any?): Boolean {
        if (other === this)
            return true
        return if (other !is KeeperItem)
            false
        else
            other.name == this.name
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