package gui.tabs

import utils.LogUtils
import java.awt.Color
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.html.HTMLEditorKit

class LogTab : JPanel(GridBagLayout()) {

    val logArea = JTextPane().apply {
        editorKit = HTMLEditorKit()
        isEditable = false
        border = EmptyBorder(5, 5, 5, 5)
    }
    val logScroll = JScrollPane(
        logArea,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    ).apply {

    }

    init {
        buildGui()
        updateLogs()
    }

    fun buildGui() {
        val constraints = GridBagConstraints()
        constraints.gridy = 0
        constraints.gridx = 0
        constraints.weightx = 1.0
        constraints.weighty = 1.0
        constraints.fill = GridBagConstraints.BOTH
        add(logScroll, constraints)
    }

    private fun updateLogs() {
        logArea.text = ""
        val doc = logArea.styledDocument
        val logLines = LogUtils.readLogFile()
        for (lineNum in logLines.indices) {
            val line = logLines[lineNum]
            if (line.isEmpty())
                continue
            val textAttrs = SimpleAttributeSet()
            if (line.length < 2 || line[1] != '/') {
                StyleConstants.setForeground(textAttrs, Color.ORANGE)
                doc.insertString(doc.length, "Строка ${lineNum + 1}: некорректный формат лога!\n", textAttrs)
                continue
            }
            val lineType = LogUtils.getLineType(line)
            if (lineType == null) {
                StyleConstants.setForeground(textAttrs, Color.ORANGE)
                doc.insertString(doc.length, "Строка ${lineNum + 1}: неизвестный префикс ${line[0]}!\n", textAttrs)
                continue
            }
            val lineNoPrefix = line.substring(2)
            when (lineType) {
                LogUtils.Level.INFO -> {}
                LogUtils.Level.WARN -> StyleConstants.setForeground(textAttrs, Color.YELLOW)
                LogUtils.Level.ERROR -> StyleConstants.setForeground(textAttrs, Color.RED)
            }
            doc.insertString(doc.length, "$lineNoPrefix\n", textAttrs)
        }
    }
}