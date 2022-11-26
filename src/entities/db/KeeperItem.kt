package entities.db


class KeeperItem(
    val name: String,
    var status: Status
) {
    companion object {
        fun fromDbResult(keeper: String?, seeder: String?, complete: Int): KeeperItem? {
            return if (keeper != null) {
                // есть официальный хранитель
                KeeperItem(
                    keeper,
                    when {
                        seeder == keeper -> {
                            // официальный хранитель раздаёт
                            KeeperItem.Status.FULL
                        }
                        complete == 1 -> {
                            // официальный хранитель скачал и не раздаёт (вот редиска)
                            KeeperItem.Status.KEEPING
                        }
                        else -> {
                            // официальный хранитель ещё не скачал
                            KeeperItem.Status.DOWNLOADING
                        }
                    }
                )
            } else if (seeder != null) {
                // хранитель раздаёт неофициально
                KeeperItem(seeder, KeeperItem.Status.SEEDING)
            } else {
                null
            }
        }
    }

    override fun toString(): String {
        return name
    }

    fun coloredName(): String {
        return "<font color='" + when (status) {
            Status.DOWNLOADING -> "red"
            Status.KEEPING -> "yellow"
            Status.SEEDING -> "aqua"
            Status.FULL -> "lime"
        } + "'>" + name + "</font>"
    }

    override fun equals(other: Any?): Boolean {
        if (other === this)
            return true
        return if (other !is KeeperItem)
            false
        else
            other.name.equals(this.name, true)
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