package gui.settings

import db.TorrentRepository
import entities.config.Config
import entities.config.SubsectionsConfigSubsection
import torrentclients.AbstractTorrentClient
import torrentclients.TorrentClientTypes
import utils.ConfigRepository
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class TorrentClientsTab : JPanel(GridBagLayout()) {

    private val subsectionsSuggestionsCount = 10
    private val parametersComponents = ArrayList<Component>()

    private var selectedClient: TorrentClientSelectorItem? = null

    private val torrentClientSelector = JComboBox<TorrentClientSelectorItem>()
    private val clientNameField = JTextField()
    private val clientTypeField = JComboBox<TorrentClientTypes>()
    private val urlField = JTextField()
    private val loginField = JTextField()
    private val passwordField = JTextField()
    private val deleteButton = JButton("Удалить").apply {
        addActionListener {
            if (JOptionPane.showConfirmDialog(
                    this,
                    "Удалить клиент \"${selectedClient?.name}\"?",
                    "Удалить клиент?",
                    JOptionPane.YES_NO_OPTION,
                ) == JOptionPane.YES_OPTION
            ) {
                ConfigRepository.torrentClients.remove(selectedClient!!.id)
                torrentClientSelector.removeItem(selectedClient)
                if (torrentClientSelector.itemCount == 0)
                    setParametersVisibility(false)
            } else {
                // pass
            }
        }
    }
    private val addButton = JButton("Добавить").apply {
        addActionListener {
            val torrentClient = AbstractTorrentClient.fromJson(
                "",
                TorrentClientTypes.QBITTORRENT,
                "",
                "",
                ""
            )
            val id = ConfigRepository.torrentClients.keys.maxOrNull()
            ConfigRepository.torrentClients
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
        constraints.anchor = GridBagConstraints.WEST
        add(JLabel("Клиент:").also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(JLabel("Название").also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(JLabel("Тип:").also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(JLabel("Адрес:").also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(JLabel("Логин:").also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(JLabel("Пароль:").also { parametersComponents.add(it) }, constraints)

        constraints.gridwidth = 1
        constraints.gridx = 1
        constraints.gridy = 0
        constraints.weightx = 2.0
        add(torrentClientSelector, constraints)

        constraints.gridy++
        add(clientNameField.also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(clientTypeField.also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(urlField.also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(loginField.also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(passwordField.also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        constraints.weightx = 1.0
        constraints.gridwidth = 1
        constraints.gridx = 1
        constraints.fill = GridBagConstraints.NONE
        constraints.anchor = GridBagConstraints.EAST
        add(deleteButton.also { parametersComponents.add(it) }, constraints)

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
            torrentClientSelector.addItem(TorrentClientSelectorItem(client.key, client.value))
        }
    }

    fun updateCurrentTorrentClient() {
        selectedClient?.let {
            ConfigRepository.torrentClients[it.id] = AbstractTorrentClient.fromJson(
                clientNameField.text,
                clientTypeField.selectedItem as TorrentClientTypes,
                urlField.text,
                loginField.text,
                passwordField.text
            )
        }
    }

    class TorrentClientSelectorItem(
        val id: Int, client: AbstractTorrentClient
    ) {
        val name = client.name

        override fun toString(): String {
            return name
        }
    }
}