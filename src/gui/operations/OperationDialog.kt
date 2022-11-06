package gui.operations

import java.awt.*
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.border.EmptyBorder

abstract class OperationDialog(frame: Frame? = null, title: String) : JDialog(frame, title, true) {

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

    private val cancelButton: JButton = JButton("Остановить")

    private val cancelListener = ActionListener {
        cancelButton.isEnabled = false
        taskCancelRequested = true
        cancelButton.text = "Остановка..."
    }

    private val closeListener = ActionListener{
        dispose()
    }

    private val statusText = JLabel()

    private var taskCancelRequested = false

    init {
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        layout = GridBagLayout()
        buildGui()
        preferredSize = Dimension(480, 200)
        size = preferredSize
        isResizable = false
        cancelButton.addActionListener(cancelListener)
    }

    abstract fun executeTask()

    /**
     * Проверяет, была ли запрошена отмена операции (нажата кнопка Остановить)
     * Данный метод следует вызывать перед каждой долгой подзадачей в задаче
     * @return true если запрошена, false иначе
     * @param finalize функция, которая будет выполнена перед возвратом, если отмена была запрошена
     *
     * Работа с локальной БД в общем случае не считается долгой подзадачей
     */
    fun cancelTaskIfRequested(finalize: () ->Unit): Boolean{
        if (taskCancelRequested){
            finalize.invoke()
            onTaskCancelled()
            return true
        }
        return false
    }

    /**
     * Вызывается после того, как задача была успешно завершена
     */
    fun onTaskCompleted() {
        dispose()
    }

    /**
     * Вызывается, если при выполнении операции возникла неисправимая ошибка
     */
    fun onTaskFailed() {
        statusText.text = "Произошла ошибка, проверьте журнал"
        // TODO: 06.11.2022 а журнала-то нет, ыыы
        statusText.foreground = Color.GREEN
        defaultCloseOperation = DISPOSE_ON_CLOSE
        cancelButton.text = "Закрыть"
        cancelButton.removeActionListener(cancelListener)
        cancelButton.addActionListener(closeListener)
        cancelButton.isEnabled = true
    }
    /**
     * Вызывается после того, как операция была отменена и выполнена соответствующая функция
     */
    fun onTaskCancelled() {
        statusText.text = "Операция отменена"
        statusText.foreground = Color.YELLOW
        defaultCloseOperation = DISPOSE_ON_CLOSE
        cancelButton.text = "Закрыть"
        cancelButton.removeActionListener(cancelListener)
        cancelButton.addActionListener(closeListener)
        cancelButton.isEnabled = true
    }

    private fun buildGui() {
        val constraints = GridBagConstraints()
        constraints.insets = Insets(2, 10, 2, 10)
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.anchor = GridBagConstraints.CENTER
        constraints.gridwidth = 2
        constraints.weightx = 0.5
        constraints.weighty = 0.5
        add(operationBigTitle, constraints)

        constraints.gridy = 1
        add(currentText, constraints)
        constraints.gridy = 2
        add(currentProgress, constraints)

        constraints.gridy = 3
        add(fullText, constraints)
        constraints.gridy = 4
        add(fullProgress, constraints)

        constraints.gridwidth = 1
        constraints.gridy = 5
        add(statusText, constraints)
        constraints.fill = GridBagConstraints.NONE
        constraints.anchor = GridBagConstraints.EAST
        add(statusText, constraints)

        constraints.gridx = 1
        constraints.weightx = 0.0
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