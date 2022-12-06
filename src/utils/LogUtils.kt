package utils

import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object LogUtils {
    private val logFile = File("files/log.txt")
    private val dateFormatter = SimpleDateFormat("dd-MM-yyyy hh:mm:ss")
    private val logListeners = ArrayList<LogListener>()

    fun log(level: Level, message: String) {
        try {
            val logWriter = FileWriter(logFile)
            for (line in message.lines()) {
                val generalLogLine = when (level) {
                    Level.INFO -> "I"
                    Level.WARN -> "W"
                    Level.ERROR -> "E"
                } + "/" + dateFormatter.format(Date()) + " " + line
                logWriter.write(generalLogLine)
                logWriter.write("\n")
                fireLogListeners(generalLogLine)
            }

            logWriter.close()
        } catch (e: Exception) {
            fireLogListeners("E/" + dateFormatter.format(Date()) + " Невозможно записать лог-файл: " + e.localizedMessage)
        }
    }

    fun readLogFile(): List<String> {
        return try {
            logFile.readLines()
        } catch (e: Exception) {
            listOf("E/" + dateFormatter.format(Date()) + " Невозможно прочитать лог-файл: " + e.localizedMessage)
        }
    }

    fun addLogListener(listener: LogListener) {
        logListeners.add(listener)
    }

    private fun fireLogListeners(line: String) {
        for (listener in logListeners) {
            listener.onLogUpdated(line)
        }

    }

    fun getLineType(line: String): Level? {
        return when (line[0]) {
            'I' -> Level.INFO
            'W' -> Level.WARN
            'E' -> Level.ERROR
            else -> null
        }
    }

    @FunctionalInterface
    interface LogListener {
        fun onLogUpdated(line: String)
    }

    enum class Level {
        INFO, WARN, ERROR
    }
}