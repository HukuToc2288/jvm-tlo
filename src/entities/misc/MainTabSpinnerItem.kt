package entities.misc

class MainTabSpinnerItem(
    val name: String,
    val onSelect: () -> Unit
) {
    override fun toString(): String {
        return name
    }
}