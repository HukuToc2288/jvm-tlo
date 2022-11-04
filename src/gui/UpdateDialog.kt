package gui

import tasks.LongRunningTask
import utils.*
import java.awt.*
import java.util.*
import javax.swing.*
import javax.swing.border.EmptyBorder

abstract class UpdateDialog(frame: Frame? = null, title: String) : JDialog(frame, title, true) {

    private val operationBigTitle = JLabel().apply {
        font = Font(font.family, Font.BOLD, 16)
        text = title
    }
    private val fullText = JLabel().apply {
        border = EmptyBorder(10, 0, 0, 0)
    }
    private val currentText = JLabel().apply {
        border = EmptyBorder(10, 0, 0, 0)
    }

    private val currentProgress = JProgressBar().apply {
        isIndeterminate = true
    }
    private val fullProgress = JProgressBar().apply {
        isIndeterminate = true
    }

    private val cancelButton = JButton("Остановить").apply {
        addActionListener {
            isEnabled = false
            text = "Остановка..."
        }
    }

    init {
        layout = GridBagLayout()
        buildGui()
        preferredSize = Dimension(480, 200)
        size = preferredSize
        isResizable = false
    }

    abstract fun executeTask()
    abstract fun cancelTask()

    private fun buildGui() {
        val constraints = GridBagConstraints()
        constraints.insets = Insets(2, 10, 2, 10)
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.anchor = GridBagConstraints.CENTER
        constraints.weightx = 0.5
        add(operationBigTitle, constraints)

        constraints.gridy = 1
        add(currentText, constraints)
        constraints.gridy = 2
        add(currentProgress, constraints)

        constraints.gridy = 3
        add(fullText, constraints)
        constraints.gridy = 4
        add(fullProgress, constraints)

        constraints.gridy = 5
        constraints.fill = GridBagConstraints.NONE
        constraints.anchor = GridBagConstraints.EAST
        add(cancelButton, constraints)
    }

    public fun setCurrentText(text: String) {
        currentText.text = text
        minimumSize = size
    }


    public fun setFullText(text: String) {
        fullText.text = text
        minimumSize = size
    }


    public fun setCurrentProgress(newValue: Int, newMaximum: Int = -1) {
        with(currentProgress) {
            isIndeterminate = value < 0
            if (newMaximum >= 0)
                maximum = newMaximum
            value = newValue
        }
    }


    public fun setFullProgress(newValue: Int, newMaximum: Int = -1) {
        with(fullProgress) {
            isIndeterminate = value < 0
            if (newMaximum >= 0)
                maximum = newMaximum
            value = newValue
        }
    }
}