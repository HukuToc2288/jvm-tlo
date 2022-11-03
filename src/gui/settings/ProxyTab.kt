package gui.settings

import utils.*
import java.awt.Color
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.net.Proxy
import java.util.*
import javax.swing.*
import kotlin.math.log

class ProxyTab : JPanel(GridBagLayout()) {
    val errorBackground = Color(255, 0, 0, 191)

    // TODO: 03.11.2022 socks5 proxy in okhttp?
    val proxyTypes = arrayOf("HTTP", "SOCKS")

    val proxyForumCheckbox = JCheckBox("Проксировать форум")
    val proxyApiCheckbox = JCheckBox("Проксировать API")

    val proxyTypeSelector = JComboBox(proxyTypes)
    val hostField = JTextField().apply {
        columns = 20
        document.addDocumentListener(ResetBackgroundListener(this))
    }
    val portField = JTextField().apply {
        columns = 20
        document.addDocumentListener(ResetBackgroundListener(this))
    }
    val loginField = JTextField().apply {
        columns = 20
    }
    val passwordField = JTextField().apply {
        columns = 20
    }

    init {
        buildGui()
    }

    private fun buildGui() {
        val constraints = GridBagConstraints()
        constraints.insets = Insets(2, 2, 2, 2)

        constraints.anchor = GridBagConstraints.WEST
        constraints.gridwidth = 2
        constraints.fill = GridBagConstraints.HORIZONTAL
        add(proxyForumCheckbox, constraints)

        constraints.gridy = 1
        add(proxyApiCheckbox, constraints)

        constraints.gridy = 2
        constraints.gridwidth = 1
        constraints.fill = GridBagConstraints.NONE
        add(JLabel("Протокол"), constraints)

        constraints.gridy = 3
        add(JLabel("Адрес"), constraints)

        constraints.gridy = 4
        add(JLabel("Порт"), constraints)

        constraints.gridy = 5
        add(JLabel("Логин"), constraints)

        constraints.gridy = 6
        add(JLabel("Пароль"), constraints)

        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.weightx = 1.0
        constraints.gridx = 1
        constraints.gridy = 2
        add(proxyTypeSelector, constraints)

        constraints.gridy = 3
        add(hostField, constraints)

        constraints.gridy = 4
        add(portField, constraints)

        constraints.gridy = 5
        add(loginField, constraints)

        constraints.gridy = 6
        add(passwordField, constraints)

        Settings.node("proxy").let {
            // TODO: 03.11.2022 set it by default to 1
            proxyForumCheckbox.isSelected = it["activate_forum", "0"].unquote() == "1"
            proxyApiCheckbox.isSelected = it["activate_api", "0"].unquote() == "1"
            proxyTypeSelector.selectedIndex =
                if (it["type", "HTTP"].unquote()?.uppercase(Locale.getDefault())?.contains("SOCKS") == true) {
                    1
                } else {
                    0
                }
            hostField.text = it["hostname", ""].unquote()
            portField.text = it["port", ""].unquote()
            loginField.text = it["login", ""].unquote()
            passwordField.text = it["password", ""].unquote()
        }
    }

    fun saveSettings() {
        with(Settings.node("proxy")) {
            put("activate_forum", proxyForumCheckbox.isSelected.toZeroOne())
            put("activate_api", proxyApiCheckbox.isSelected.toZeroOne())
            put(
                "type", when (proxyTypeSelector.selectedIndex) {
                    0 -> "http"
                    else -> "socks5h"
                }.quote()
            )
            put("hostname", hostField.text.quote())
            put("port", portField.text.quote())
            put("login", loginField.text.quote())
            put("password", passwordField.text.quote())
        }
    }
}