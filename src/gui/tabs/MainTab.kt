package gui.tabs

import java.awt.*
import java.text.SimpleDateFormat
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel


class MainTab : JPanel(GridBagLayout()) {

    // Кнопки управления

    val showFilterButton = buildControlButton("filter.png"){
        firstFilter.isVisible = !fourthFilter.isVisible
        secondFilter.isVisible = !fourthFilter.isVisible
        thirdFilter.isVisible = !fourthFilter.isVisible
        fourthFilter.isVisible = !fourthFilter.isVisible
    }

    // Первый фильтр
    val keepCheckbox = buildFilterCheckbox("храню")
    val noKeepCheckbox = buildFilterCheckbox("не храню")
    val downloadingCheckbox = buildFilterCheckbox("качаю")

    val sortAscendingRadio = buildFilterRadiobutton("по возрастанию", true)
    val sortDescendingRadio = buildFilterRadiobutton("по убыванию")
    val ascDescRadioGroup = ButtonGroup().apply {
        add(sortAscendingRadio)
        add(sortDescendingRadio)
    }

    val sortTitleRadio = buildFilterRadiobutton("по объёму")
    val sortSizeRadio = buildFilterRadiobutton("по объёму")
    val sortSeedsRadio = buildFilterRadiobutton("по количеству сидов", true)
    val sortDateRadio = buildFilterRadiobutton("по дате регистрации")
    val sortCriteriaRadioGroup = ButtonGroup().apply {
        add(sortTitleRadio)
        add(sortSizeRadio)
        add(sortSeedsRadio)
        add(sortDateRadio)
    }

    val firstFilter = buildFirstFilter()

    // Второй фильтр

    val verifiedCheckbox = buildFilterCheckbox("не проверено")
    val notVerifiedCheckbox = buildFilterCheckbox("проверено")
    val notFormalizedCheckbox = buildFilterCheckbox("недооформлено")
    val doubtfulCheckbox = buildFilterCheckbox("сомнительно")
    val temporaryCheckbox = buildFilterCheckbox("временная")

    val lowPriorityCheckbox = buildFilterCheckbox("низкий")
    val normalPriorityCheckbox = buildFilterCheckbox("обычный")
    val highPriorityCheckbox = buildFilterCheckbox("высокий")

    val secondFilter = buildSecondFilter()

    // Третий фильтр

    val averageSeedsSpinner = JSpinner().apply {
        value = 14
        addChangeListener {
            updateFilter()
        }
    }
    val registerDateInput = JFormattedTextField(SimpleDateFormat("dd.MM.yyyy")).apply {
        columns = 7
        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(p0: DocumentEvent?) {
                checkDate()
            }

            override fun removeUpdate(p0: DocumentEvent?) {

            }

            override fun changedUpdate(p0: DocumentEvent?) {
                checkDate()
            }

            fun checkDate() {
                if (this@apply.text.length == 10)
                    updateFilter()
            }
        })
    }

    val searchByPhraseField = JTextField().apply {
        columns = 15
        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(p0: DocumentEvent?) {
                updateFilter()
            }

            override fun removeUpdate(p0: DocumentEvent?) {
                updateFilter()

            }

            override fun changedUpdate(p0: DocumentEvent?) {
                updateFilter()
            }
        })
    }

    val searchTitleRadio = buildFilterRadiobutton("В названии раздачи", true)
    val searchKeeperRadio = buildFilterRadiobutton("В имени хранителя")
    val searchRadioGroup = ButtonGroup().apply {
        add(searchTitleRadio)
        add(searchKeeperRadio)
    }

    val thirdFilter = buildThirdFilter()

    // Четвёртый фильтр (последний, ура!)
    val greenCheckbox = buildFilterCheckbox("\"зелёные\"")
    val noKeepersCheckbox: JCheckBox = buildFilterCheckbox("нет хранителей").apply {
        addItemListener {
            if (this.isSelected) hasKeepersCheckbox.isSelected = false
        }
    }
    val hasKeepersCheckbox: JCheckBox = buildFilterCheckbox("есть хранители").apply {
        addItemListener {
            if (this.isSelected) noKeepersCheckbox.isSelected = false
        }
    }
    val noSeedersCheckbox: JCheckBox = buildFilterCheckbox("нет сидов-хранителей").apply {
        addItemListener {
            if (this.isSelected) hasSeedersCheckbox.isSelected = false
        }
    }
    val hasSeedersCheckbox: JCheckBox = buildFilterCheckbox("есть сиды-хранители").apply {
        addItemListener {
            if (this.isSelected) noSeedersCheckbox.isSelected = false
        }
    }
    val seedLowSpinner: JSpinner = JSpinner(SpinnerNumberModel(0.0, 0.0, 99.0, 0.5)).apply {
        (editor as JSpinner.NumberEditor).textField.columns = 2
        addChangeListener {
            if ((value as Double) > (seedHighSpinner.value as Double))
                seedHighSpinner.value = value
            updateFilter()
        }
    }

    val seedHighSpinner: JSpinner = JSpinner(SpinnerNumberModel(3.0, 0.0, 99.0, 0.5)).apply {
        (editor as JSpinner.NumberEditor).textField.columns = 2
        addChangeListener {
            if ((value as Double) < (seedLowSpinner.value as Double))
                seedLowSpinner.value = value
            updateFilter()
        }
    }

    val fourthFilter = buildFourthFilter()

    // Таблица раздач
    val torrentsTable = JTable().apply {
        val tableModel = object : DefaultTableModel(null,arrayOf("", "Дата", "Название", "Сиды", "Хранители")){
            override fun getColumnClass(c: Int): Class<*> {
                return getValueAt(0, c)::class.java
            }

        }
        model = tableModel
        setDefaultEditor(Any::class.java, null)
        columnModel.apply {
            getColumn(0).maxWidth = 24
            getColumn(0).minWidth = 24
            getColumn(1).maxWidth = 80
            getColumn(1).minWidth = 80
            getColumn(3).maxWidth = 48
            getColumn(3).minWidth = 48
            getColumn(4).maxWidth = 480
            getColumn(4).preferredWidth = 480
        }
    }

    fun addTorrentToTable(){
        // stub
        val model = torrentsTable.model as DefaultTableModel
        model.addRow(arrayOf(java.lang.Boolean(false),"27.01.2029","Полураспад ПолуСССР — Движение к Совершенству Через Задний Проход — 2020, MP3, 320 kbps",1,"HukuToc2288"))
    }

    init {
        buildGui()
    }

    fun buildGui() {
        var constraints = GridBagConstraints()

        val topicSelector = JComboBox<String>(arrayOf("Раздачи из всех хранимых подразделов", "Раздачи с высоким приоритетом хранения", "Ъеъ")).apply {

        }
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.gridwidth = 5
        constraints.gridx = 0
        constraints.ipady = 10
        constraints.gridy = 0
        add(topicSelector, constraints)

        constraints.ipady = 0
        constraints.gridy = 1
        add(buildControlButtonsPanel(), constraints)

        constraints.gridy = 2
        constraints.gridwidth = 1
        add(firstFilter, constraints)

        constraints.gridx = 1
        add(secondFilter, constraints)

        constraints.gridx = 2
        add(thirdFilter, constraints)

        constraints.gridx = 3
        add(fourthFilter, constraints)

        constraints.gridx = 0
        constraints.gridy = 3
        constraints.gridwidth = 5
        constraints.weightx = 1.0
        constraints.weighty = 1.0
        constraints.fill = GridBagConstraints.BOTH
        add(JScrollPane(torrentsTable), constraints)

        addTorrentToTable()
    }

    fun buildControlButtonsPanel(): JComponent {
        val container: JPanel = JPanel()
        container.setLayout(FlowLayout(FlowLayout.LEFT, 0, 0))
        container.add(showFilterButton)
        return container
    }

    fun buildControlButton(image: String, onClick: () -> Unit): JButton {
        val buttonImage = ImageIcon(javaClass.getResource("/res/images/" + image))
        val button = JButton().apply {
            icon = buttonImage
            addActionListener {
                onClick()
            }
        }
        return button
    }

    fun buildFirstFilter(): JPanel {
        val container = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }
        container.add(keepCheckbox)
        container.add(noKeepCheckbox)
        container.add(downloadingCheckbox)
        container.add(buildSimpleSeparator())

        container.add(sortAscendingRadio)
        container.add(sortDescendingRadio)
        container.add(buildSimpleSeparator())

        container.add(sortTitleRadio)
        container.add(sortSizeRadio)
        container.add(sortSeedsRadio)
        container.add(sortDateRadio)

        return container
    }

    fun buildSecondFilter(): JPanel {
        val container = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(notVerifiedCheckbox)
            add(verifiedCheckbox)
            add(notFormalizedCheckbox)
            add(doubtfulCheckbox)
            add(temporaryCheckbox)
            add(buildSimpleSeparator())
            add(lowPriorityCheckbox)
            add(normalPriorityCheckbox)
            add(highPriorityCheckbox)
        }

        return container
    }

    fun buildThirdFilter(): JPanel {
        val container = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JLabel("Период средних сидов:"))
                add(averageSeedsSpinner)
            }.apply {
                setAlignmentX(Component.LEFT_ALIGNMENT)
            })
            add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JLabel("Дата регистрации до:"))
                add(registerDateInput)
            }.apply {
                setAlignmentX(Component.LEFT_ALIGNMENT)
            })
            add(buildSimpleSeparator().apply {
                setAlignmentX(Component.LEFT_ALIGNMENT)
            })
            add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JLabel("Поиск по фразе:"))
                add(searchByPhraseField)
            }.apply {
                setAlignmentX(Component.LEFT_ALIGNMENT)
            })
            add(searchTitleRadio.apply {
                setAlignmentX(Component.LEFT_ALIGNMENT)
            })
            add(searchKeeperRadio.apply {
                setAlignmentX(Component.LEFT_ALIGNMENT)
            })
        }
        return container
    }

    fun buildFourthFilter(): JPanel {
        val container = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(greenCheckbox.apply {
                setAlignmentX(Component.LEFT_ALIGNMENT)
            })
            add(noKeepersCheckbox.apply {
                setAlignmentX(Component.LEFT_ALIGNMENT)
            })
            add(hasKeepersCheckbox.apply {
                setAlignmentX(Component.LEFT_ALIGNMENT)
            })
            add(noSeedersCheckbox.apply {
                setAlignmentX(Component.LEFT_ALIGNMENT)
            })
            add(hasSeedersCheckbox.apply {
                setAlignmentX(Component.LEFT_ALIGNMENT)
            })
            add(buildSimpleSeparator().apply {
                setAlignmentX(Component.LEFT_ALIGNMENT)
            })
            add(JLabel("Количество сидов:").apply {
                setAlignmentX(Component.LEFT_ALIGNMENT)
            })
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                add(JLabel("от"))
                add(seedLowSpinner)
                add(JLabel("до"))
                add(seedHighSpinner)
            }.apply {
                setAlignmentX(Component.LEFT_ALIGNMENT)
            })

        }
        return container
    }

    fun buildSimpleSeparator(): JSeparator {
        return JSeparator().apply {
            border = EmptyBorder(5, 5, 5, 5)
        }
    }

    fun buildFilterCheckbox(title: String, checked: Boolean = false): JCheckBox {
        val checkbox = JCheckBox(title)
        checkbox.isSelected = checked
        checkbox.addItemListener {
            updateFilter()
        }
        return checkbox
    }

    fun buildFilterRadiobutton(title: String, checked: Boolean = false): JRadioButton {
        val checkbox = JRadioButton(title)
        checkbox.isSelected = checked
        checkbox.setAlignmentX(Component.LEFT_ALIGNMENT)
        checkbox.addItemListener {
            updateFilter()
        }
        return checkbox
    }

    fun buildFilterSpinner(value: Int): JSpinner {
        val spinner = JSpinner()
        spinner.value = value
        spinner.addChangeListener {
            updateFilter()
        }
        return spinner
    }

    fun updateFilter() {
        println("Пока фильтр не обновляется")
    }
}