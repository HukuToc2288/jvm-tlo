package gui.operations

import utils.LogUtils
import utils.pluralForum
import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.JOptionPane
import javax.swing.text.DefaultCaret
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledEditorKit


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
        showCancellationDialog()
    }

    private val closeListener = ActionListener {
        dispose()
    }

    private val statusText = JLabel()

    private var taskCancelRequested = false

    private val windowCloseListener = object : WindowAdapter() {
        override fun windowClosing(windowEvent: WindowEvent) {
            showCancellationDialog()
        }
    }

    val logListener: (level: LogUtils.Level, line: String) -> Unit = { level, line ->
        addLogLine(level, line)
    }

    val logArea = JTextPane().apply {
        editorKit = StyledEditorKit()
        isEditable = false
        border = EmptyBorder(5, 5, 5, 5)
        (caret as DefaultCaret).updatePolicy = DefaultCaret.ALWAYS_UPDATE
    }
    val logScroll = JScrollPane(
        logArea,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    ).apply {

    }

    private var result: Result? = null
    private var nonCriticalErrors = 0

    init {
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        layout = GridBagLayout()
        buildGui()
        preferredSize = Dimension(480, 300)
        size = preferredSize
        isResizable = false
        cancelButton.addActionListener(cancelListener)
        addWindowListener(windowCloseListener)
        setLocationRelativeTo(frame)
        LogUtils.addLogListener(logListener)
    }

    private fun showCancellationDialog() {
        if (JOptionPane.showConfirmDialog(
                this,
                "Отменить операцию?\nПрогресс может быть частично или полностью потерян",
                "",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            ) == JOptionPane.YES_OPTION
        ) {
            SwingUtilities.invokeLater {
                if (result != null)
                    return@invokeLater
                removeWindowListener(windowCloseListener)
                cancelButton.isEnabled = false
                taskCancelRequested = true
                cancelButton.text = "Остановка..."
            }
        }
    }

    // Этот метод заблокирует поток, из которого будет вызван (и правильно сделает)
    fun executeTask(): Result {
        LogUtils.i("$title: начато")
        SwingUtilities.invokeLater {
            doTask()
        }
        isVisible = true
        return result!!
    }

    abstract fun doTask()

    /**
     * Проверяет, была ли запрошена отмена операции (нажата кнопка Остановить)
     * Данный метод следует вызывать перед каждой долгой подзадачей в задаче
     * @return true если запрошена, false иначе
     * @param finalize функция, которая будет выполнена перед возвратом, если отмена была запрошена
     *
     * Работа с локальной БД в общем случае не считается долгой подзадачей
     */
    fun cancelTaskIfRequested(finalize: () -> Unit): Boolean {
        if (taskCancelRequested) {
            finalize.invoke()
            onTaskCancelled()
            return true
        }
        if (result == Result.FAILED) {
            finalize.invoke()
            return true
        }
        return false
    }

    /**
     * Вызывается после того, как задача была успешно завершена
     */
    fun onTaskSuccess() {
        LogUtils.i("$title: операция успешна")
        SwingUtilities.invokeLater {
            // FIXME: 11.11.2022 оставлено для отладки, в готовой версии окно должно просто закрываться
            if (nonCriticalErrors > 0) {
                statusText.text = "Выполнено с " +
                        nonCriticalErrors.pluralForum("ошибкой", "ошибками") +
                        ", проверьте журнал"
                statusText.foreground = Color.YELLOW
            } else {
                statusText.text = "Успешно"
                statusText.foreground = Color.GREEN
            }
            result = Result.SUCCESS
            onTaskFinished()
        }
    }

    /**
     * Вызывается, если при выполнении операции возникла неисправимая ошибка
     */
    fun onTaskFailed() {
        LogUtils.e("$title: операция не выполнена")
        SwingUtilities.invokeLater {
            statusText.text = "Произошла критическая ошибка, проверьте журнал"
            statusText.foreground = Color.RED
            result = Result.FAILED
            onTaskFinished()
        }
    }

    /**
     * Вызывается после того, как операция была отменена и выполнена соответствующая функция
     */
    private fun onTaskCancelled() {
        LogUtils.i("$title: операция прервана пользователем")
        SwingUtilities.invokeLater {
            statusText.text = "Операция отменена"
            statusText.foreground = fullText.foreground
            result = Result.CANCELLED
            onTaskFinished()
        }
    }

    /**
     * Вызывается после любого результата задачи
     */
    protected open fun onTaskFinished() {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        cancelButton.text = "Закрыть"
        cancelButton.removeActionListener(cancelListener)
        cancelButton.addActionListener(closeListener)
        cancelButton.isEnabled = true
        with(currentProgress) {
            if (isIndeterminate) {
                isIndeterminate = false
                maximum = 1
                value = 0
            }
        }
        with(fullProgress) {
            if (isIndeterminate) {
                isIndeterminate = false
                maximum = 1
                value = 0
            }
        }
    }

    protected fun showNonCriticalError() {
        nonCriticalErrors++
        statusText.text =
            nonCriticalErrors.pluralForum("некритическая ошибка", "некритические ошибки", "некритических ошибок") +
                    ", работаем дальше"
        statusText.foreground = Color.ORANGE
    }

    private fun buildGui() {
        val constraints = GridBagConstraints()
        constraints.insets = Insets(2, 10, 2, 10)
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.anchor = GridBagConstraints.CENTER
        constraints.gridwidth = 2
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

        constraints.gridwidth = 1
        constraints.gridy = 20
        constraints.fill = GridBagConstraints.NONE
        constraints.anchor = GridBagConstraints.EAST
        add(statusText, constraints)

        constraints.gridx = 1
        //constraints.weightx = 0.0
        add(cancelButton, constraints)

        constraints.gridx = 0
        constraints.gridy = 10
        constraints.gridwidth = 2
        constraints.weighty = 1.0
        constraints.fill = GridBagConstraints.BOTH
        add(logScroll, constraints)
    }

    fun setCurrentText(text: String) {
        LogUtils.i(text)
        currentText.text = text
        minimumSize = size
    }


    fun setFullText(text: String) {
        LogUtils.i(text)
        fullText.text = text
        minimumSize = size
    }


    fun setCurrentProgress(newValue: Int, newMaximum: Int = -1) {
        with(currentProgress) {
            isIndeterminate = newValue < 0
            if (newMaximum >= 0)
                maximum = newMaximum
            value = newValue
        }
    }

    fun setFullProgress(newValue: Int, newMaximum: Int = -1) {
        with(fullProgress) {
            isIndeterminate = newValue < 0
            if (newMaximum >= 0)
                maximum = newMaximum
            value = newValue
        }
    }

    fun incrementCurrentProgress(value: Int = 1) {
        currentProgress.value += value
    }

    fun incrementFullProgress(value: Int = 1) {
        fullProgress.value += value
    }

    private fun addLogLine(level: LogUtils.Level, line: String) {
        val textAttrs = SimpleAttributeSet()
        when (level) {
            LogUtils.Level.INFO -> {}
            LogUtils.Level.WARN -> StyleConstants.setForeground(textAttrs, Color.YELLOW)
            LogUtils.Level.ERROR -> StyleConstants.setForeground(textAttrs, Color.RED)
        }
        logArea.styledDocument.insertString(logArea.styledDocument.length, "$line\n", textAttrs)
    }

    override fun dispose() {
        LogUtils.removeLogListener(logListener)
        super.dispose()
    }

    enum class Result {
        SUCCESS, FAILED, CANCELLED
    }
}