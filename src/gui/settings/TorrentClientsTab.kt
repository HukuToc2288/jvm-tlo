package gui.settings

import com.fasterxml.jackson.databind.AbstractTypeResolver
import db.TorrentRepository
import entities.config.Config
import entities.config.SubsectionsConfigSubsection
import gui.GuiUtils.verifyNotEmpty
import torrentclients.AbstractTorrentClient
import torrentclients.TorrentClientTypes
import utils.ConfigRepository
import utils.ResetBackgroundListener
import utils.SimpleDocumentListener
import java.awt.Component
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class TorrentClientsTab : JPanel(GridBagLayout()), SavableTab {

    private val subsectionsSuggestionsCount = 10
    private val parametersComponents = ArrayList<Component>()

    private var selectedClient: TorrentClientSelectorItem? = null

    private var fireClientSelectorListener = true
    private val torrentClientSelector: JComboBox<TorrentClientSelectorItem> = JComboBox<TorrentClientSelectorItem>().apply {
        addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                if (!fireClientSelectorListener)
                    return@addItemListener
                if (!saveSettings()) {
                    selectedItem = selectedClient
                    return@addItemListener
                }
                selectedClient = this.selectedItem as TorrentClientSelectorItem?
                    ConfigRepository.torrentClients[selectedClient?.id]?.let {
                        clientNameField.text = it.name
                        clientTypeField.selectedItem = it.type
                        hostnameField.text = it.hostname
                        portField.value = it.port
                        loginField.text = it.login
                        passwordField.text = it.password
                        sslCheckbox.isSelected = it.ssl
                    }
                }
        }
    }
    private val clientNameField = JTextField().apply {
        document.addDocumentListener(ResetBackgroundListener(this))
        document.addDocumentListener(SimpleDocumentListener {
            selectedClient?.name = this.text
            torrentClientSelector.repaint()
        })
    }
    private val clientTypeField = JComboBox<TorrentClientTypes>(TorrentClientTypes.values())
    private val hostnameField = JTextField().apply {
        document.addDocumentListener(ResetBackgroundListener(this))
    }
    private val portField = JSpinner(SpinnerNumberModel(1, 1, 65535, 1)).apply {
        (editor as JSpinner.NumberEditor).apply {
            textField.columns = 6
            format.isGroupingUsed = false
        }

    }
    private val loginField = JTextField()
    private val passwordField = JTextField()
    private val sslCheckbox = JCheckBox("SSL")
    private val deleteButton = JButton("Удалить").apply {
        addActionListener {
            if (JOptionPane.showConfirmDialog(
                    this,
                    "Удалить клиент \"${selectedClient?.name}\"?",
                    "Удалить клиент?",
                    JOptionPane.YES_NO_OPTION,
                ) == JOptionPane.YES_OPTION
            ) {
                selectedClient?.let {
                    selectedClient = null
                    ConfigRepository.torrentClients.remove(it.id)
                    torrentClientSelector.removeItem(it)
                    if (torrentClientSelector.itemCount == 0)
                        setParametersVisibility(false)
                }
            } else {
                // pass
            }
        }
    }
    private val addButton = JButton("Добавить").apply {
        addActionListener {
            if (!saveSettings()) {
                return@addActionListener
            }
            val id = ConfigRepository.torrentClients.keys.maxOrNull()?.plus(1) ?: 1
            selectedClient = TorrentClientSelectorItem(id, "Новый клиент $id")
            selectedClient?.let {
                fireClientSelectorListener = false
                torrentClientSelector.addItem(it)
                torrentClientSelector.selectedItem = it
                clientNameField.text = "Новый клиент $id"
                clientTypeField.selectedItem = TorrentClientTypes.QBITTORRENT
                hostnameField.text = ""
                portField.value = 8080
                loginField.text = ""
                passwordField.text = ""
                sslCheckbox.isSelected = false
                fireClientSelectorListener = true
            }
            setParametersVisibility(true)
        }
    }

    init {
        buildGui()
    }


    private fun buildGui() {
        val constraints = GridBagConstraints()
        constraints.insets = Insets(2, 2, 2, 2)
        constraints.gridy = 0
        constraints.gridx = 0
        constraints.anchor = GridBagConstraints.WEST
        add(JLabel("Клиент:").also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(JLabel("Название").also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(JLabel("Тип:").also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(JLabel("Адрес:").also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(JLabel("Порт:").also { parametersComponents.add(it) }, constraints)

        constraints.gridx = 1
        add(portField.also { parametersComponents.add(it) }, constraints)

        constraints.gridx++
        add(sslCheckbox.also { parametersComponents.add(it) }, constraints)


        constraints.gridx = 0
        constraints.gridy++
        add(JLabel("Логин:").also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(JLabel("Пароль:").also { parametersComponents.add(it) }, constraints)

        constraints.gridwidth = 2
        constraints.gridx = 1
        constraints.gridy = 0
        constraints.weightx = 1.0
        constraints.fill = GridBagConstraints.HORIZONTAL
        add(torrentClientSelector.also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(clientNameField.also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(clientTypeField.also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(hostnameField.also { parametersComponents.add(it) }, constraints)

        constraints.gridy += 2
        add(loginField.also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(passwordField.also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        constraints.gridwidth = 3
        constraints.gridx = 0
        add(JPanel().apply {
            layout = FlowLayout(FlowLayout.RIGHT)
            add(addButton)
            add(deleteButton.also { parametersComponents.add(it) })

        }, constraints)

        fillTorrentClientsSelector()

        if (torrentClientSelector.itemCount > 0) {
            torrentClientSelector.selectedIndex = 0
        }
        setParametersVisibility(torrentClientSelector.itemCount > 0)
    }

    private fun setParametersVisibility(isVisible: Boolean) {
        for (component in parametersComponents)
            component.isVisible = isVisible
    }


    private fun fillTorrentClientsSelector() {
        torrentClientSelector.removeAllItems()
        for (client in ConfigRepository.torrentClients) {
            torrentClientSelector.addItem(TorrentClientSelectorItem(client.key, client.value.name))
        }
    }

    class TorrentClientSelectorItem(
        val id: Int, var name: String
    ) {
        override fun toString(): String {
            return name
        }
    }

    override fun saveSettings(): Boolean {
        if (torrentClientSelector.itemCount == 0 || selectedClient == null)
            return true
        if (!(clientNameField.verifyNotEmpty() and hostnameField.verifyNotEmpty()))
            return false
        selectedClient?.let {
            ConfigRepository.torrentClients[it.id] = AbstractTorrentClient.fromJson(
                clientNameField.text,
                clientTypeField.selectedItem as TorrentClientTypes,
                hostnameField.text,
                portField.value as Int,
                sslCheckbox.isSelected,
                loginField.text,
                passwordField.text
            )
        }
        return true
    }
}