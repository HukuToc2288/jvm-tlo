package gui

import java.awt.Color
import javax.swing.JLabel
import javax.swing.JSeparator
import javax.swing.JTextField
import javax.swing.border.EmptyBorder
import javax.swing.text.JTextComponent

object GuiUtils {

    val errorBackground = Color(255, 0, 0, 191)
    lateinit var defaultFieldBackground: Color
    lateinit var defaultTextColor: Color

    init {
        reloadColors()
    }

    fun reloadColors() {
        defaultFieldBackground = JTextField().background
        defaultTextColor = JLabel().foreground

    }

    fun JTextComponent.verifyNotEmpty(): Boolean {
        return !text.isEmpty().also {
            background = if (it)
                errorBackground
            else
                defaultFieldBackground
        }
    }

    fun simpleSeparator(): JSeparator {
        return JSeparator().apply {
            border = EmptyBorder(5, 5, 5, 5)
        }
    }
}