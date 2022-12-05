package gui

import api.rebuildForumApi
import gui.settings.AuthSettingsTab
import gui.settings.ProxyTab
import gui.settings.SubsectionsTab
import gui.settings.TorrentClientsTab
import utils.ConfigRepository
import java.awt.Dimension
import javax.swing.*
import java.awt.event.WindowEvent

import java.awt.event.WindowAdapter
import javax.swing.JOptionPane


class SettingsWindow : JFrame("Настройки JVM-TLO") {

    var shouldRebuildRetrofits = false
    val proxyTab = ProxyTab()
    val subsectionsTab = SubsectionsTab()
    val torrentClientsTab = TorrentClientsTab()

    init {
        val tabbedPane = JTabbedPane()

        val mainPanel = JPanel().apply {
            layout = OverlayLayout(this)
        }
        add(mainPanel)

        mainPanel.add(tabbedPane)
        tabbedPane.addTab("Авторизация", AuthSettingsTab())
        tabbedPane.addTab("Прокси", proxyTab)
        tabbedPane.addTab("Хранимые подразделы", subsectionsTab)
        tabbedPane.add("Торрент-клиенты", torrentClientsTab)
        tabbedPane.addChangeListener {
            if (tabbedPane.selectedIndex == 1) {
                // обновим настройки прокси при покидании этой вкладки
                shouldRebuildRetrofits = true
            }
        }

        minimumSize = Dimension(640, 480)
        preferredSize = minimumSize

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                // сохраняем несохранённые изменения в текущий конфиг
                if (shouldRebuildRetrofits) {
                    proxyTab.saveSettings()
                    rebuildForumApi()
                }
                subsectionsTab.updateCurrentSubsection()
                // записываем в постоянную память
                tryWriteConfig()
                e.window.dispose()
            }
        })
    }

    fun tryWriteConfig() {
        try {
            ConfigRepository.write()
        } catch (e: Exception) {
            e.printStackTrace()
            val options = arrayOf<Any>(
                "Попробовать ещё раз",
                "Закрыть"
            )
            if (JOptionPane.showOptionDialog(
                    this,
                    "Не удаётся сохранить настройки в файл!\n" +
                            "Поверьте права доступа, а затем нажмите \"${options[0]}\"\n" +
                            " Если вы нажмёте \"${options[1]}\", то новые настройки будут действовать, пока открыто приложение\n\n" +
                            e.localizedMessage,
                    "Ошибка сохранения настроек",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.ERROR_MESSAGE,
                    null,  //do not use a custom Icon
                    options,  //the titles of buttons
                    options[0]
                ) == JOptionPane.YES_OPTION
            ) {
                tryWriteConfig()
            } else {
                // pass
            }
        }
    }
}