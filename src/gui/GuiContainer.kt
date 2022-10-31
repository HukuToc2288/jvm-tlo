package gui

import gui.tabs.MainTab
import gui.tabs.SettingsTab

import java.awt.Dimension
import java.awt.GridLayout
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
    tabbedPane.addTab("Настройки",SettingsTab())
    val mainPanel = JPanel(GridLayout(1, 1))
    frame.add(mainPanel)
    mainPanel.add(tabbedPane)

    //Display the window.
    frame.pack()
    frame.isVisible = true
    frame.minimumSize = Dimension(frame.size)
}