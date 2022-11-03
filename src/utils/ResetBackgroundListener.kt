package utils

import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class ResetBackgroundListener(private val component: JComponent): DocumentListener {
    private val defaultBackground = component.background

    override fun insertUpdate(p0: DocumentEvent?) {
        resetBackground()
    }

    override fun removeUpdate(p0: DocumentEvent?) {
        resetBackground()

    }

    override fun changedUpdate(p0: DocumentEvent?) {
        resetBackground()
    }

    private fun resetBackground(){
        component.background = defaultBackground
    }
}