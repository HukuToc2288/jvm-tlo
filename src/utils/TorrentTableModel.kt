package utils

import entities.TorrentItem
import java.text.SimpleDateFormat
import javax.swing.table.AbstractTableModel

class TorrentTableModel: AbstractTableModel() {

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
        return when (column){
            0 -> dateFormat.format(torrent.date)
            1 -> torrent.name
            2 -> torrent.seeds
            3 -> torrent.keepers.joinToString(", ")
            else -> "WTF!?"
        }
    }

    override fun getColumnName(column: Int): String {
        return columnNames[column]
    }

    override fun getColumnClass(c: Int): Class<*> {
        return getValueAt(0, c)::class.java
    }

    fun clear(){
        torrentList.clear()
        fireTableDataChanged()
    }

    fun getValueAt(row: Int): TorrentTableItem{
        return torrentList[row]
    }

    // assuming that everything grouped by topic id
    fun addOrUpdateTorrent(item: TorrentItem){
        if (torrentList.isNotEmpty() && torrentList.last().topicId == item.topicId){
            torrentList.last().keepers.add(item.keeper)
            fireTableCellUpdated(rowCount-1,3)
        } else {
            torrentList.add(TorrentTableItem.fromTorrentItem(item))
            fireTableRowsInserted(rowCount-1,rowCount-1)
        }
    }
}