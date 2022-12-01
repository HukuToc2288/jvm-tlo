package gui.settings

import entities.config.ProxyConfigProxy
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

    val proxyForumCheckbox = JCheckBox("Проксировать форум")
    val proxyApiCheckbox = JCheckBox("Проксировать API")

    val proxyTypeSelector = JComboBox<ProxyConfigProxy.ProxyType>(ProxyConfigProxy.ProxyType.values())
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

        with(ConfigRepository.proxyConfig){
            proxyForumCheckbox.isSelected = proxyForum
            proxyApiCheckbox.isSelected = proxyApi
            val proxy = proxies[0]
            proxyTypeSelector.selectedItem = proxy.type
            hostField.text = proxy.hostname
            portField.text = proxy.port.toString()
            loginField.text = proxy.login
            passwordField.text = proxy.password
        }
    }

    fun saveSettings() {
        with(ConfigRepository.proxyConfig){
            proxyForum = proxyForumCheckbox.isSelected
            proxyApi = proxyApiCheckbox.isSelected

            val proxy = proxies[0]
            with(proxy){
                type = proxyTypeSelector.selectedItem as ProxyConfigProxy.ProxyType
                hostname = hostField.text
                port = portField.text.toInt()
                login = loginField.text.toString()
                password = passwordField.text.toString()
            }
        }
    }
}