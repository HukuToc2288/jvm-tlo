package gui

import api.forumRetrofit
import api.rebuildForumApi
import gui.settings.AuthSettingsTab
import gui.settings.ProxyTab
import gui.tabs.MainTab
import java.awt.Dimension
import javax.swing.*
import javax.swing.border.EmptyBorder

class SettingsWindow : JFrame("Настройки JVM-TLO") {

    var shouldRebuildRetrofits = false
    val proxyTab = ProxyTab()

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
        tabbedPane.addTab(
            "Прокси",
            proxyTab
        )
        tabbedPane.addChangeListener {
            if (shouldRebuildRetrofits) {
                proxyTab.saveSettings()
                rebuildForumApi()
                shouldRebuildRetrofits = false
                return@addChangeListener
            }
            if (tabbedPane.selectedIndex == 1) {
                // обновим настройки прокси при покидании этой вкладки
                shouldRebuildRetrofits = true
            }
        }

        minimumSize = Dimension(400, 300)
        preferredSize = minimumSize
    }
}