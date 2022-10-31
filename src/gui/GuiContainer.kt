package gui

import gui.tabs.MainTab

import java.awt.Dimension
import java.lang.Exception
import javax.swing.*

import javax.swing.JPanel

import javax.swing.JComponent
import javax.swing.UIManager


fun main(args: Array<String>) {
    //Schedule a job for the event dispatch thread:
    //creating and showing this application's GUI.
    SwingUtilities.invokeLater {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {
        }
        createAndShowGUI()
    }
}

private fun createAndShowGUI() {
    //Create and set up the window.
    val frame = JFrame("JVM-TLO")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

    //Add content to the window.
    val tabbedPane = JTabbedPane()

    val panel1: JComponent = MainTab()
    tabbedPane.addTab(
        "Главная", panel1
    )
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
    frame.isVisible = true
    frame.minimumSize = Dimension(frame.size)
}