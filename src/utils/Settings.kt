package utils

import org.ini4j.IniPreferences
import org.ini4j.spi.EscapeTool
import java.io.File
import java.io.FileInputStream
import java.util.prefs.Preferences

object Settings : IniPreferences(FileInputStream("files/config.ini")) {
    // TODO: 03.11.2022 создавать файл если его нет

}

public fun String?.unquote(): String? {
    // все настройки в файле в кавычках, УЖАС!
    if (this == null)
        return null
    return EscapeTool.getInstance().unquote(this)
}

public fun String?.quote(): String {
    // все настройки в файле в кавычках, УЖАС!
    if (this.isNullOrEmpty())
        return ""
    return EscapeTool.getInstance().quote(this)
}

fun Boolean.toZeroOne(): String {
    return if (this) "\"1\"" else "\"0\""
}