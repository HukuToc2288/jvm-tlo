package gui

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import gui.tabs.LogTab
import gui.tabs.MainTab
import utils.ConfigRepository

import java.awt.Dimension
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.Exception
import javax.swing.*

import javax.swing.JPanel

import javax.swing.JComponent
import javax.swing.UIManager
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    //Schedule a job for the event dispatch thread:
    //creating and showing this application's GUI.
    SwingUtilities.invokeLater {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {
        }
        initConfig()
        createAndShowGUI()
    }
}

private fun initConfig() {
    try {
        ConfigRepository.read()
    } catch (e: Exception) {
        e.printStackTrace()
        var message = when (e) {
            is FileNotFoundException -> "Файл конфигурации отсутствует и не может быть создан, либо недоступен! Проверьте права доступа"
            is JsonProcessingException -> "Файл конфигурации испорчен! Исправьте ошибки или удалите файл (будут сброшены все настройки)"
            is IOException -> "Файл конфигурации не может быть прочитан! Проверьте права доступа"
            else -> "Ошибка загрузки конфигурации"
        }
        message += "\n\n" + e.localizedMessage
        JOptionPane.showMessageDialog(null, message, "Ошибка конфигурации", JOptionPane.ERROR_MESSAGE)
        exitProcess(1)
    }
}

private fun createAndShowGUI() {
    //Create and set up the window.
    val frame = JFrame("JVM-TLO")
    initConfig()
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

    //Add content to the window.
    val tabbedPane = JTabbedPane()

    val mainTab = MainTab()
    tabbedPane.addTab("Главная", mainTab)

    val logTab = LogTab()

    val mainPanel = JPanel().apply {
        layout = OverlayLayout(this)
    }
    frame.add(mainPanel)


    val settingsButton = JButton("Настройки").apply {
        addActionListener {
            SettingsWindow().isVisible = true
        }
    }
    settingsButton.isOpaque = false
    settingsButton.alignmentX = 1.0f
    settingsButton.alignmentY = 0.0f

    mainPanel.add(settingsButton)
    tabbedPane.alignmentX = 1f
    tabbedPane.alignmentY = 0f
    mainPanel.add(tabbedPane)

    //Display the window.
    frame.pack()
    tabbedPane.addTab("Логи", logTab)
    frame.isVisible = true
    frame.minimumSize = Dimension(frame.size)
}