package gui

import gui.tabs.MainTab
import javax.swing.*
import javax.swing.border.EmptyBorder

class SettingsWindow: JFrame("Настройки JVM-TLO") {

    init {
        val tabbedPane = JTabbedPane()

        val panel1: JComponent = MainTab()

        val mainPanel = JPanel().apply {
            layout = OverlayLayout(this)
        }
        add(mainPanel)


        mainPanel.add(tabbedPane)

        //Display the window.
        pack()
    }
}