package utils

import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object LogUtils {
    private val logFile = File("files/log.txt")
    private val dateFormatter = SimpleDateFormat("dd-MM-yyyy hh:mm:ss")
    private val logListeners = ArrayList<(Level, String) -> Unit>()

    fun log(level: Level, message: String) {
        try {
            val logWriter = FileWriter(logFile)
            for (line in message.lines()) {
                logWriter.write(
                    when (level) {
                        Level.INFO -> "I/"
                        Level.WARN -> "W/"
                        Level.ERROR -> "E/"
                    }
                )
                val generalLogLine = dateFormatter.format(Date()) + " " + line
                logWriter.write(generalLogLine)
                logWriter.write("\n")
                fireLogListeners(level, generalLogLine)
            }

            logWriter.close()
        } catch (e: Exception) {
            fireLogListeners(
                Level.ERROR,
                dateFormatter.format(Date()) + " Невозможно записать лог-файл: " + e.localizedMessage
            )
        }
    }

    fun readLogFile(): List<String> {
        return try {
            if (!logFile.exists())
                logFile.createNewFile()
            logFile.readLines()
        } catch (e: Exception) {
            listOf("E/" + dateFormatter.format(Date()) + " Невозможно прочитать лог-файл: " + e.localizedMessage)
        }
    }

    fun addLogListener(listener: (level: Level, line: String) -> Unit) {
        logListeners.add(listener)
    }

    private fun fireLogListeners(level: Level, line: String) {
        for (listener in logListeners) {
            listener.invoke(level, line)
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

    enum class Level {
        INFO, WARN, ERROR
    }
}