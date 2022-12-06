package gui.settings

import db.TorrentRepository
import entities.config.SubsectionsConfigSubsection
import torrentclients.AbstractTorrentClient
import utils.ConfigRepository
import java.awt.*
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class SubsectionsTab : JPanel(GridBagLayout()), SavableTab {

    private val subsectionsSuggestionsCount = 10
    private val parametersComponents = ArrayList<Component>()

    private var selectedSubsection: SubsectionsConfigSubsection? = null

    private val subsectionsAddField: JTextField = object : JTextField() {

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
            val thisField = this
            for (i in subsections.indices) {
                val subsection = subsections[i]
                if (i >= subsectionsSuggestionsCount) {
                    val menuItem = JMenuItem("Показаны первые $subsectionsSuggestionsCount подразделов").apply {
                        isEnabled = false
                    }
                    popupMenu.add(menuItem)
                    break
                }
                val menuItem = JMenuItem("${subsection.id}: ${subsection.name}").apply {
                    addActionListener {
                        // TODO: 02.12.2022 enter not working
                        val subsectionsConfigSubsection = SubsectionsConfigSubsection(
                            subsection.id,
                            subsection.name,
                            1,
                            "",
                            "",
                            false,
                            false
                        )
                        ConfigRepository.subsections.add(subsectionsConfigSubsection)
                        subsectionSelector.addItem(subsectionsConfigSubsection)
                        subsectionSelector.selectedItem = subsectionsConfigSubsection
                        setParametersVisibility(true)
                        thisField.text = ""
                    }
                }
                popupMenu.add(menuItem)
            }
            popupMenu.show(this, 0, this.size.height)
            requestFocus()
        }
    }
    val subsectionSelector = JComboBox<SubsectionsConfigSubsection>().apply {
        addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                saveSettings()
                val selectedItem = this.selectedItem as SubsectionsConfigSubsection?
                selectedSubsection = selectedItem
                selectedItem ?: return@addItemListener
                if (ConfigRepository.torrentClients.containsKey(selectedItem.clientId)) {
                    for (i in 1 until torrentClientSelector.itemCount) {
                        val item = torrentClientSelector.getItemAt(i) as TorrentClientSelectorItem
                        if (item.id == selectedItem.clientId) {
                            torrentClientSelector.selectedIndex = i
                            break
                        }
                    }
                } else {
                    torrentClientSelector.selectedIndex = 0
                }

                categoryNameField.text = selectedItem.category
                downloadDirectoryField.text = selectedItem.dataFolder
                createSubFoldersCheckbox.isSelected = selectedItem.createSubFolders
                hideInListCheckbox.isSelected = selectedItem.hideInList
            }
        }
    }
    private val torrentClientSelector = JComboBox<TorrentClientSelectorItem>()
    private val categoryNameField = JTextField()
    private val downloadDirectoryField = JTextField()
    private val createSubFoldersCheckbox = JCheckBox("Создавать подкаталоги с ID раздач", false)
    private val hideInListCheckbox = JCheckBox("Скрывать раздачи в общем списке", false)
    private val deleteButton = JButton("Удалить").apply {
        addActionListener {
            if (JOptionPane.showConfirmDialog(
                    this,
                    "Удалить \"${selectedSubsection?.title}\" из списка хранимых подразделов?",
                    "Удалить подраздел?",
                    JOptionPane.YES_NO_OPTION,
                ) == JOptionPane.YES_OPTION
            ) {
                ConfigRepository.subsections.remove(selectedSubsection)
                subsectionSelector.removeItem(selectedSubsection)
                if (subsectionSelector.itemCount == 0)
                    setParametersVisibility(false)
            } else {
                // pass
            }
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
        add(JLabel("Добавить подраздел"), constraints)

        constraints.gridy++
        add(JLabel("Подраздел:").also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(JLabel("Клиент:").also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(JLabel("Категория:").also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(JLabel("Каталог для данных:").also { parametersComponents.add(it) }, constraints)


        constraints.gridwidth = 2
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.gridy++
        add(createSubFoldersCheckbox.also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(hideInListCheckbox.also { parametersComponents.add(it) }, constraints)

        constraints.gridwidth = 1
        constraints.gridx = 1
        constraints.gridy = 0
        constraints.weightx = 2.0
        add(subsectionsAddField, constraints)

        constraints.gridy++
        add(subsectionSelector.also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(torrentClientSelector.also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(categoryNameField.also { parametersComponents.add(it) }, constraints)

        constraints.gridy++
        add(downloadDirectoryField.also { parametersComponents.add(it) }, constraints)


        constraints.gridy += 3
        constraints.weightx = 1.0
        constraints.gridwidth = 1
        constraints.gridx = 1
        constraints.fill = GridBagConstraints.NONE
        constraints.anchor = GridBagConstraints.EAST
        add(deleteButton.also { parametersComponents.add(it) }, constraints)

        fillTorrentClientsSelector()
        fillSubsectionsSelector()

        if (subsectionSelector.itemCount > 0) {
            subsectionSelector.selectedIndex = 0
        }
        setParametersVisibility(subsectionSelector.itemCount > 0)
    }

    private fun setParametersVisibility(isVisible: Boolean) {
        for (component in parametersComponents)
            component.isVisible = isVisible
    }

    private fun fillSubsectionsSelector() {
        subsectionSelector.removeAllItems()
        for (subsection in ConfigRepository.subsections) {
            subsectionSelector.addItem(subsection)
        }
    }

    private fun fillTorrentClientsSelector() {
        torrentClientSelector.removeAllItems()
        torrentClientSelector.addItem(TorrentClientSelectorItem(0, null))
        for (client in ConfigRepository.torrentClients) {
            torrentClientSelector.addItem(TorrentClientSelectorItem(client.key, client.value))
        }
    }

    class TorrentClientSelectorItem(
        val id: Int, client: AbstractTorrentClient?
    ) {
        val name = client?.name ?: "(не выбран)"

        override fun toString(): String {
            return name
        }
    }

    override fun saveSettings(): Boolean {
        selectedSubsection?.let {
            it.clientId = (torrentClientSelector.selectedItem as TorrentClientSelectorItem?)?.id ?: 0
            it.category = categoryNameField.text
            it.dataFolder = downloadDirectoryField.text
            it.createSubFolders = createSubFoldersCheckbox.isSelected
            it.hideInList = hideInListCheckbox.isSelected
        }
        return true
    }
}