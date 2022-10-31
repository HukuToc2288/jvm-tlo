package gui

import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.border.EmptyBorder

class SettingsWindow: JFrame("Настройки JVM-TLO") {

    init {
        add(JLabel("<html>Пока тут ничего нет<br>Для настройки используйте Web-TLO</html>").apply {
            border = EmptyBorder(50,50,50,50)
        })
        pack()
    }
}