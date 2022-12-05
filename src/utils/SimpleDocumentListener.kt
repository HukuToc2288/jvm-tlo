package utils

import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

@FunctionalInterface
class SimpleDocumentListener(val onUpdate: (event: DocumentEvent?)->Unit) : DocumentListener {
    override fun insertUpdate(p0: DocumentEvent?) {
        onUpdate(p0)
    }

    override fun removeUpdate(p0: DocumentEvent?) {
        onUpdate(p0)
    }

    override fun changedUpdate(p0: DocumentEvent?) {
        onUpdate(p0)
    }
}