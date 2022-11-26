package utils

import java.awt.Component
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

class KeeperTableCellRender() : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
        c.toolTipText = "<html>" +
                "<font color='lime'>Зелёный</font> — хранит и раздаёт<br>" +
                "<font color='yellow'>Жёлтый</font> — хранит, но не раздаёт<br>" +
                "<font color='aqua'>Голубой</font> — раздаёт, но нет в списках хранимого<br>" +
                "<font color='red'>Красный</font> — качает" +
                "</html>"
        return c
    }
}