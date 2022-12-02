package entities.db

class SubsectionSearchItem(
    val id: Int,
    val name: String
) {
    override fun toString(): String {
        return name
    }
}