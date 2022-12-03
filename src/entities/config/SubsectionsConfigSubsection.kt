package entities.config

class SubsectionsConfigSubsection(
    var id: Int,
    var title: String,
    var clientId: Int,
    var category: String,
    var dataFolder: String,
    var createSubFolders: Boolean,
    var hideInList: Boolean
){
    override fun toString(): String {
        return title
    }
}