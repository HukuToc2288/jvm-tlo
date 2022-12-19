package gui

import api.forumRetrofit
import api.keeperRetrofit
import api.rebuildForumApi
import api.rebuildKeeperApi
import gui.settings.*
import utils.ConfigRepository
import java.awt.Dimension
import javax.swing.*
import java.awt.event.WindowEvent

import java.awt.event.WindowAdapter
import java.lang.reflect.Proxy
import javax.swing.JOptionPane


class SettingsWindow : JFrame("Настройки JVM-TLO") {

    private var shouldRebuildRetrofits = false
    private val proxyTab = ProxyTab()
    private val tabs = arrayOf<Pair<String,JPanel>>(
        "Авторизация" to AuthSettingsTab(),
        "Прокси" to proxyTab,
        "Хранимые подразделы" to SubsectionsTab(),
        "Торрент-клиенты" to TorrentClientsTab(),
    )

    init {
        val tabbedPane = JTabbedPane()

        val mainPanel = JPanel().apply {
            layout = OverlayLayout(this)
        }
        add(mainPanel)

        mainPanel.add(tabbedPane)
        for (tab in tabs) {
            tabbedPane.addTab(tab.first, tab.second)
        }
        tabbedPane.addChangeListener {
            if (tabbedPane.selectedComponent == proxyTab) {
                // обновим настройки прокси при покидании этой вкладки
                shouldRebuildRetrofits = true
            } else if (shouldRebuildRetrofits){
                shouldRebuildRetrofits = false
                proxyTab.saveSettings()
                rebuildForumApi()
                rebuildKeeperApi()
            }
        }

        minimumSize = Dimension(640, 480)
        preferredSize = minimumSize

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                // сохраняем несохранённые изменения в текущий конфиг
                var showError = true
                for (tabPair in tabs) {
                    val tab = tabPair.second
                    if (showError && tab is SavableTab && !tab.saveSettings()) {
                        val options = arrayOf<Any>(
                            "К настройкам",
                            "Закрыть без сохранения"
                        )
                        if (JOptionPane.showOptionDialog(
                                this@SettingsWindow,
                                "На вкладке ${tabPair.first} обнаружены неправильные настройки!\n" +
                                        "Если не исправить их, часть настроек может быть не сохранена",
                                "Неправильные настройки",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.ERROR_MESSAGE,
                                null,  //do not use a custom Icon
                                options,  //the titles of buttons
                                options[0]
                            ) == JOptionPane.YES_OPTION
                        ){
                            tabbedPane.selectedComponent = tab
                            return
                        } else {
                            showError = false
                        }
                    }
                }
                // пересоздаём API
                if (tabbedPane.selectedComponent == proxyTab){
                    rebuildForumApi()
                    rebuildKeeperApi()
                }
                // записываем в постоянную память
                tryWriteConfig()
                e.window.dispose()
            }
        })

        defaultCloseOperation = DO_NOTHING_ON_CLOSE
    }

    fun tryWriteConfig() {
        try {
            ConfigRepository.write()
        } catch (e: Exception) {
            e.printStackTrace()
            val options = arrayOf<Any>(
                "Попробовать ещё раз",
                "Закрыть без сохранения"
            )
            if (JOptionPane.showOptionDialog(
                    this,
                    "Не удаётся сохранить настройки в файл!\n" +
                            "Поверьте права доступа, а затем нажмите \"${options[0]}\"\n" +
                            "Если вы нажмёте \"${options[1]}\", то новые настройки будут действовать, пока открыто приложение\n\n" +
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