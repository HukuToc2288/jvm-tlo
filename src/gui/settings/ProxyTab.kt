package gui.settings

import entities.config.ProxyConfigProxy
import gui.GuiUtils.verifyNotEmpty
import utils.*
import java.awt.*
import java.net.Proxy
import java.util.*
import javax.swing.*
import kotlin.math.log

class ProxyTab : JPanel(GridBagLayout()), SavableTab {


    private val parametersComponents = ArrayList<Component>()
    val proxyForumCheckbox: JCheckBox = JCheckBox("Проксировать форум").apply {
        addItemListener {
            setParametersVisibility(isSelected || proxyApiCheckbox.isSelected)
        }
    }
    val proxyApiCheckbox: JCheckBox = JCheckBox("Проксировать API").apply {
        addItemListener {
            setParametersVisibility(isSelected || proxyForumCheckbox.isSelected)
        }
    }

    val proxyTypeSelector = JComboBox<ProxyConfigProxy.ProxyType>(ProxyConfigProxy.ProxyType.values())
    val hostField = JTextField().apply {
        columns = 20
        document.addDocumentListener(ResetBackgroundListener(this))
    }
    private val portField = JSpinner(SpinnerNumberModel(80, 1, 65535, 1)).apply {
        (editor as JSpinner.NumberEditor).apply {
            textField.columns = 6
            format.isGroupingUsed = false
        }

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
        add(JLabel("Протокол").also { parametersComponents.add(it) }, constraints)

        constraints.gridy = 3
        add(JLabel("Адрес").also { parametersComponents.add(it) }, constraints)

        constraints.gridy = 4
        add(JLabel("Порт").also { parametersComponents.add(it) }, constraints)

        constraints.gridy = 5
        add(JLabel("Логин").also { parametersComponents.add(it) }, constraints)

        constraints.gridy = 6
        add(JLabel("Пароль").also { parametersComponents.add(it) }, constraints)

        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.weightx = 1.0
        constraints.gridx = 1
        constraints.gridy = 2
        add(proxyTypeSelector.also { parametersComponents.add(it) }, constraints)

        constraints.gridy = 3
        add(hostField.also { parametersComponents.add(it) }, constraints)

        constraints.gridy = 4
        add(portField.also { parametersComponents.add(it) }, constraints)

        constraints.gridy = 5
        add(loginField.also { parametersComponents.add(it) }, constraints)

        constraints.gridy = 6
        add(passwordField.also { parametersComponents.add(it) }, constraints)

        with(ConfigRepository.proxyConfig) {
            proxyForumCheckbox.isSelected = proxyForum
            proxyApiCheckbox.isSelected = proxyApi
            val proxy = proxies[0]
            proxyTypeSelector.selectedItem = proxy.type
            hostField.text = proxy.hostname
            portField.value = proxy.port
            loginField.text = proxy.login
            passwordField.text = proxy.password
        }
    }

    private fun setParametersVisibility(isVisible: Boolean) {
        for (component in parametersComponents)
            component.isVisible = isVisible
    }

    override fun saveSettings(): Boolean {
        with(ConfigRepository.proxyConfig) {
            proxyForum = proxyForumCheckbox.isSelected
            proxyApi = proxyApiCheckbox.isSelected

            if (!proxyApiCheckbox.isSelected && !proxyForumCheckbox.isSelected)
                return true

            if (!(hostField.verifyNotEmpty()))
                return false

            val proxy = proxies[0]
            with(proxy) {
                type = proxyTypeSelector.selectedItem as ProxyConfigProxy.ProxyType
                hostname = hostField.text
                port = portField.value as Int
                login = loginField.text.toString()
                password = passwordField.text.toString()
            }
        }
        return true
    }

}