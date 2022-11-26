package utils

import java.awt.Component
import java.awt.Container
import javax.swing.JPanel

// поможем свингу научиться нормально пробрасывать состояния
fun Component.deepSetEnabled(enabled: Boolean) {
    if (this is Container) {
        for (component in components) {
            component.deepSetEnabled(enabled)
        }

    }
    if (this !is JPanel) {
        // выключенные панели выглядят странно
        isEnabled = enabled
    }
}