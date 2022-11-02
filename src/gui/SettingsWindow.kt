package gui

import gui.settings.AuthSettingsTab
import gui.tabs.MainTab
import java.awt.Dimension
import javax.swing.*
import javax.swing.border.EmptyBorder

class SettingsWindow: JFrame("Настройки JVM-TLO") {

    init {
        val tabbedPane = JTabbedPane()

        val mainPanel = JPanel().apply {
            layout = OverlayLayout(this)
        }
        add(mainPanel)

        mainPanel.add(tabbedPane)
        tabbedPane.addTab(
            "Авторизация",
            AuthSettingsTab()
        )

        minimumSize = Dimension(360, 280)
        preferredSize = minimumSize
    }
}