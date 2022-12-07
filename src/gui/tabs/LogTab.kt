package gui.tabs

import utils.LogUtils
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledEditorKit


class LogTab : JPanel(GridBagLayout()) {

    val logArea = JTextPane().apply {
        editorKit = WrapEditorKit()
        isEditable = false
        border = EmptyBorder(5, 5, 5, 5)
    }
    val logScroll = JScrollPane(
        logArea,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    ).apply {

    }

    val logInfoRadioButton = buildLogRadioButton("все логи", true).apply {
        addItemListener {
            displayLogLevel = LogUtils.Level.INFO
            updateLogs()
        }
    }
    val logWarningRadioButton = buildLogRadioButton("предупреждения и ошибки").apply {
        addItemListener {
            displayLogLevel = LogUtils.Level.WARN
            updateLogs()
        }
    }
    val logErrorRadioButton = buildLogRadioButton("только ошибки").apply {
        addItemListener {
            displayLogLevel = LogUtils.Level.ERROR
            updateLogs()
        }
    }
    val logLevelRadioGroup = ButtonGroup().apply {
        add(logInfoRadioButton)
        add(logWarningRadioButton)
        add(logErrorRadioButton)
    }
    val wrapCheckBox = JCheckBox("Перенос строк", true).apply {
        addItemListener {
            logArea.editorKit = if (isSelected) WrapEditorKit() else StyledEditorKit()
            updateLogs()
        }
    }

    var displayLogLevel = LogUtils.Level.INFO

    init {
        buildGui()
        updateLogs()
        LogUtils.addLogListener { level, line ->
            addLogLine(level, line)
        }
    }

    fun buildGui() {
        val constraints = GridBagConstraints()
        constraints.gridy = 0
        constraints.gridx = 0
        constraints.insets = Insets(2, 5, 2, 2)
        constraints.fill = GridBagConstraints.HORIZONTAL
        add(buildLogLevelSelector(), constraints)

        constraints.gridx++
        constraints.anchor = GridBagConstraints.NORTH
        add(wrapCheckBox, constraints)

        constraints.gridx = 0
        constraints.gridy = 1
        constraints.gridwidth = 2
        constraints.weightx = 1.0
        constraints.weighty = 1.0
        constraints.fill = GridBagConstraints.BOTH
        add(logScroll, constraints)
    }

    fun buildLogLevelSelector(): JPanel {
        val container = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }
        container.add(JLabel("Показывать:"))
        container.add(logInfoRadioButton)
        container.add(logWarningRadioButton)
        container.add(logErrorRadioButton)
        return container
    }

    private fun updateLogs() {
        logArea.text = ""
        val doc = logArea.styledDocument
        val logLines = LogUtils.readLogFile()
        for (lineNum in logLines.indices) {
            val line = logLines[lineNum]
            if (line.isEmpty())
                continue
            if (line.length < 2 || line[1] != '/') {
                val textAttrs = SimpleAttributeSet()
                StyleConstants.setForeground(textAttrs, Color.ORANGE)
                doc.insertString(doc.length, "Строка ${lineNum + 1}: некорректный формат лога!\n", textAttrs)
                continue
            }
            val level = LogUtils.getLineType(line)
            if (level == null) {
                val textAttrs = SimpleAttributeSet()
                StyleConstants.setForeground(textAttrs, Color.ORANGE)
                doc.insertString(doc.length, "Строка ${lineNum + 1}: неизвестный префикс ${line[0]}!\n", textAttrs)
                continue
            }
            val lineNoPrefix = line.substring(2)
            addLogLine(level, lineNoPrefix)
        }
    }

    private fun addLogLine(level: LogUtils.Level, line: String) {
        if (displayLogLevel == LogUtils.Level.INFO ||
            displayLogLevel == level ||
            level == LogUtils.Level.ERROR
        ) {
            val textAttrs = SimpleAttributeSet()
            when (level) {
                LogUtils.Level.INFO -> {}
                LogUtils.Level.WARN -> StyleConstants.setForeground(textAttrs, Color.YELLOW)
                LogUtils.Level.ERROR -> StyleConstants.setForeground(textAttrs, Color.RED)
            }
            logArea.styledDocument.insertString(logArea.styledDocument.length, "$line\n", textAttrs)
        }
    }

    private fun buildLogRadioButton(title: String, checked: Boolean = false): JRadioButton {
        val checkbox = JRadioButton(title)
        checkbox.isSelected = checked
        checkbox.alignmentX = Component.LEFT_ALIGNMENT
        return checkbox
    }
}