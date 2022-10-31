package utils

import javax.swing.table.DefaultTableCellRenderer
import java.text.DateFormat
import java.text.SimpleDateFormat

class DateTableCellRenderer(pattern: String) : DefaultTableCellRenderer() {
    var formatter = SimpleDateFormat(pattern)
    public override fun setValue(value: Any?) {
        text = formatter.format(value)
    }
}