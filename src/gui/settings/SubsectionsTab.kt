package gui.settings

import db.TorrentRepository
import entities.config.SubsectionsConfigSubsection
import entities.db.SubsectionSearchItem
import torrentclients.AbstractTorrentClient
import utils.ConfigRepository
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URL
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class SubsectionsTab : JPanel(GridBagLayout()) {

    val subsectionsSuggestionsCount = 10

    val subsectionsAddField: JTextField = object : JTextField() {

        val popupMenu = JPopupMenu()

        init {
            document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(p0: DocumentEvent?) {
                    suggestSubsections()
                }

                override fun removeUpdate(p0: DocumentEvent?) {
                    suggestSubsections()
                }

                override fun changedUpdate(p0: DocumentEvent?) {
                    suggestSubsections()
                }
            })
        }

        fun suggestSubsections() {
            popupMenu.isVisible = false
            if (text.isEmpty()) {
                return
            }
            val subsections = TorrentRepository.findSubsections(text, subsectionsSuggestionsCount)
            popupMenu.removeAll()
            for (i in subsections.indices) {
                if (i >= subsectionsSuggestionsCount) {
                    val menuItem = JMenuItem("Показаны первые $subsectionsSuggestionsCount подразделов").apply {
                        isEnabled = false
                    }
                    popupMenu.add(menuItem)
                    break
                }
                val menuItem = JMenuItem("${subsections[i].id}: ${subsections[i].name}").apply {
                    addActionListener {
                        // TODO: 02.12.2022 enter not working
                        println(this@apply.label)
                    }
                }
                popupMenu.add(menuItem)
            }
            popupMenu.show(this, 0, this.size.height)
            requestFocus()
        }
    }
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
        add(JLabel("Добавить подраздел"), constraints)

        constraints.gridy++
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
        add(subsectionsAddField, constraints)

        constraints.gridy++
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