package utils

import entities.TorrentItem
import java.text.SimpleDateFormat
import javax.swing.table.AbstractTableModel

class TorrentTableModel : AbstractTableModel() {

    val torrentList = ArrayList<TorrentTableItem>()

    private val columnNames = arrayOf("Дата", "Название", "Сиды", "Хранители")
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy")

    override fun getRowCount(): Int {
        return torrentList.size
    }

    override fun getColumnCount(): Int {
        return 4
    }

    override fun getValueAt(row: Int, column: Int): Any {
        val torrent = torrentList[row]
        return when (column) {
            0 -> dateFormat.format(torrent.date)
            1 -> torrent.name
            2 -> torrent.seeds
            3 -> torrent.keepers.joinToString(",","<html>","</html>"){
                it.coloredName()
            }
            else -> "WTF!?"
        }
    }

    override fun getColumnName(column: Int): String {
        return columnNames[column]
    }

    override fun getColumnClass(c: Int): Class<*> {
        return getValueAt(0, c)::class.java
    }

    fun clear() {
        torrentList.clear()
    }

    fun getValueAt(row: Int): TorrentTableItem {
        return torrentList[row]
    }

    fun addTorrent(tableItem: TorrentTableItem) {
        torrentList.add(tableItem)
    }

    // assuming that everything grouped by topic id
    fun addOrUpdateTorrent(item: TorrentItem) {
        if (torrentList.isNotEmpty() && torrentList.last().topicId == item.topicId && item.keeper != null) {
            torrentList.last().keepers.let {
                if (it.indexOf(item.keeper) != -1)
                // обновить статус хранителя
                    it.get(it.indexOf(item.keeper)).updateStatus(item.keeper.status)
                else
                    it.add(item.keeper)

            }
        } else {
            torrentList.add(TorrentTableItem.fromTorrentItem(item))
        }
    }

    fun commit(){
        fireTableDataChanged()
    }
}