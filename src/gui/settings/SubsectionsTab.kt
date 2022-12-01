package gui.settings

import entities.config.SubsectionsConfigSubsection
import torrentclients.AbstractTorrentClient
import utils.ConfigRepository
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

class SubsectionsTab : JPanel(GridBagLayout()) {

    val subsectionSelector = JComboBox<SubsectionsConfigSubsection>().apply {
    }
    val torrentClientSelector = JComboBox<TorrentClientSelectorItem>()
    val categoryNameField = JTextField()
    val downloadDirectoryField = JTextField()
    val addButton = JButton("Добавить")
    val deleteButton = JButton("Удалить")

    init {
        buildGui()
    }


    private fun buildGui() {
        val constraints = GridBagConstraints()
        constraints.insets = Insets(2, 2, 2, 2)
        constraints.gridy = 0
        constraints.anchor = GridBagConstraints.WEST
        add(JLabel("Подраздел:"), constraints)

        constraints.gridy++
        add(JLabel("Клиент:"), constraints)

        constraints.gridy++
        add(JLabel("Категория:"), constraints)

        constraints.gridy++
        add(JLabel("Каталог для данных:"), constraints)

        constraints.gridwidth = 2
        constraints.gridx = 1
        constraints.gridy = 0
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.weightx = 2.0
        add(subsectionSelector, constraints)

        constraints.gridy++
        add(torrentClientSelector, constraints)

        constraints.gridy++
        add(categoryNameField, constraints)

        constraints.gridy++
        add(downloadDirectoryField, constraints)

        constraints.gridy++
        constraints.weightx = 1.0
        constraints.gridwidth = 1
        constraints.gridx = 1
        constraints.fill = GridBagConstraints.NONE
        constraints.anchor = GridBagConstraints.SOUTHEAST
        add(addButton, constraints)

        constraints.gridx = 2
        constraints.weightx = 0.0
        add(deleteButton, constraints)

        fillSubsectionsSelector()
        fillTorrentClientsSelector()
        if (subsectionSelector.itemCount > 0) {
            subsectionSelector.selectedIndex = 0
            if (torrentClientSelector.itemCount > 0)
                torrentClientSelector.selectedIndex = 1
        } else {
            torrentClientSelector.isEnabled = false
        }
    }


    private fun fillSubsectionsSelector() {
        subsectionSelector.removeAllItems()
        for (subsection in ConfigRepository.subsections) {
            subsectionSelector.addItem(subsection)
        }
    }

    private fun fillTorrentClientsSelector() {
        torrentClientSelector.removeAllItems()
        for (client in ConfigRepository.torrentClients) {
            torrentClientSelector.addItem(TorrentClientSelectorItem(client.key, client.value))
        }
    }

    class TorrentClientSelectorItem(
        val id: Int, val client: AbstractTorrentClient
    ) {
        override fun toString(): String {
            return client.name
        }
    }
}