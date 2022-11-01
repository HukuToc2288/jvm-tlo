package gui.tabs

import db.TorrentRepository
import entities.KeeperItem
import utils.TorrentFilterCriteria
import utils.TorrentTableItem
import utils.TorrentTableModel
import java.awt.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.Timer
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.collections.ArrayList


class MainTab : JPanel(GridBagLayout()) {

    var updateFilterTimer = Timer()

    // Кнопки управления

    val showFilterButton = buildControlButton("filter.png") {
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

    val sortTitleRadio = buildFilterRadiobutton("по названию")
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

    val notVerifiedCheckbox = buildFilterCheckbox("не проверено")
    val verifiedCheckbox = buildFilterCheckbox("проверено")
    val notFormalizedCheckbox = buildFilterCheckbox("недооформлено")
    val suspiciousCheckbox = buildFilterCheckbox("сомнительно")
    val temporaryCheckbox = buildFilterCheckbox("временная")

    val lowPriorityCheckbox = buildFilterCheckbox("низкий")
    val normalPriorityCheckbox = buildFilterCheckbox("обычный")
    val highPriorityCheckbox = buildFilterCheckbox("высокий")

    val secondFilter = buildSecondFilter()

    // Третий фильтр

    val averageSeedsSpinner = JSpinner().apply {
        value = 14
        addChangeListener {
            enqueueFilterUpdate()
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
                    enqueueFilterUpdate()
            }
        })
    }

    val searchByPhraseField = JTextField().apply {
        columns = 15
        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(p0: DocumentEvent?) {
                enqueueFilterUpdate()
            }

            override fun removeUpdate(p0: DocumentEvent?) {
                enqueueFilterUpdate()

            }

            override fun changedUpdate(p0: DocumentEvent?) {
                enqueueFilterUpdate()
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
            if (this.isSelected) {
                noDownloadedCheckbox.isSelected = false
                hasDownloadedCheckbox.isSelected = false
            }
        }
    }
    val noDownloadedCheckbox: JCheckBox = buildFilterCheckbox("нет скачавших хранителей").apply {
        addItemListener {
            if (this.isSelected) {
                noKeepersCheckbox.isSelected = false
            }
        }
    }
    val hasDownloadedCheckbox: JCheckBox = buildFilterCheckbox("есть скачавшие хранители").apply {
        addItemListener {
            if (this.isSelected) {
                noKeepersCheckbox.isSelected = false
            }
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
            enqueueFilterUpdate()
        }
    }

    val seedHighSpinner: JSpinner = JSpinner(SpinnerNumberModel(3.0, 0.0, 99.0, 0.5)).apply {
        (editor as JSpinner.NumberEditor).textField.columns = 2
        addChangeListener {
            if ((value as Double) < (seedLowSpinner.value as Double))
                seedLowSpinner.value = value
            enqueueFilterUpdate()
        }
    }

    val fourthFilter = buildFourthFilter()

    // Таблица раздач
    val torrentsTable = JTable().apply {
        val tableModel = TorrentTableModel()
        tableHeader.reorderingAllowed = false
        model = tableModel
        setDefaultEditor(Any::class.java, null)
        columnModel.apply {
            getColumn(0).maxWidth = 80
            getColumn(0).minWidth = 80
            getColumn(2).maxWidth = 48
            getColumn(2).minWidth = 48
            getColumn(3).maxWidth = 320
            getColumn(3).preferredWidth = 320
        }
    }

    init {
        buildGui()
    }

    fun buildGui() {
        var constraints = GridBagConstraints()

        val topicSelector = JComboBox<String>(
            arrayOf(
                "Раздачи из всех хранимых подразделов",
                "Раздачи с высоким приоритетом хранения",
                "Ъеъ"
            )
        ).apply {

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

        resetFilter()

        constraints.gridx = 0
        constraints.gridy = 3
        constraints.gridwidth = 5
        constraints.weightx = 1.0
        constraints.weighty = 1.0
        constraints.fill = GridBagConstraints.BOTH
        add(JScrollPane(torrentsTable), constraints)
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
            add(suspiciousCheckbox)
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
            add(noDownloadedCheckbox.apply {
                setAlignmentX(Component.LEFT_ALIGNMENT)
            })
            add(hasDownloadedCheckbox.apply {
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
            enqueueFilterUpdate()
        }
        return checkbox
    }

    fun buildFilterRadiobutton(title: String, checked: Boolean = false): JRadioButton {
        val checkbox = JRadioButton(title)
        checkbox.isSelected = checked
        checkbox.setAlignmentX(Component.LEFT_ALIGNMENT)
        checkbox.addItemListener {
            enqueueFilterUpdate()
        }
        return checkbox
    }

    fun buildFilterSpinner(value: Int): JSpinner {
        val spinner = JSpinner()
        spinner.value = value
        spinner.addChangeListener {
            enqueueFilterUpdate()
        }
        return spinner
    }

    fun enqueueFilterUpdate() {
        // ждём немного времени чтобы пользователь завершил установку всех параметров
        updateFilterTimer.cancel()
        updateFilterTimer = Timer()
        updateFilterTimer.schedule(object : TimerTask() {
            override fun run() {
                queryAndUpdateTable()
            }

        }, 1500)
    }

    fun queryAndUpdateTable() {
        val torrentFilter = TorrentFilterCriteria(
            sortAscendingRadio.isSelected,
            when {
                sortTitleRadio.isSelected -> TorrentFilterCriteria.SortOrder.NAME
                sortSizeRadio.isSelected -> TorrentFilterCriteria.SortOrder.SIZE
                sortSeedsRadio.isSelected -> TorrentFilterCriteria.SortOrder.SEEDS
                sortDateRadio.isSelected -> TorrentFilterCriteria.SortOrder.DATE
                else -> TorrentFilterCriteria.SortOrder.SEEDS
            },
            ArrayList<TorrentFilterCriteria.ForumStatus>().apply {
                if (notVerifiedCheckbox.isSelected)
                    add(TorrentFilterCriteria.ForumStatus.NOT_VERIFIED)
                if (verifiedCheckbox.isSelected)
                    add(TorrentFilterCriteria.ForumStatus.VERIFIED)
                if (notFormalizedCheckbox.isSelected)
                    add(TorrentFilterCriteria.ForumStatus.NOT_FORMALIZED)
                if (suspiciousCheckbox.isSelected)
                    add(TorrentFilterCriteria.ForumStatus.SUSPICIOUS)
                if (temporaryCheckbox.isSelected)
                    add(TorrentFilterCriteria.ForumStatus.TEMPORARY)
            },
            ArrayList<TorrentFilterCriteria.Priority>().apply {
                if (lowPriorityCheckbox.isSelected)
                    add(TorrentFilterCriteria.Priority.LOW)
                if (normalPriorityCheckbox.isSelected)
                    add(TorrentFilterCriteria.Priority.NORMAL)
                if (highPriorityCheckbox.isSelected)
                    add(TorrentFilterCriteria.Priority.HIGH)
            },
            seedLowSpinner.value as Double,
            seedHighSpinner.value as Double,
            try {
                SimpleDateFormat("dd.MM.yyyy").parse(registerDateInput.value as String)
            } catch (e: Exception) {
                // TODO: 01.11.2022 prompt incorrect date
                Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1)
                }.time
            },
            searchByPhraseField.text,
            "", // TODO: 01.11.2022 
            noKeepersCheckbox.isSelected,
            noDownloadedCheckbox.isSelected,
            hasDownloadedCheckbox.isSelected,
            noSeedersCheckbox.isSelected,
            hasSeedersCheckbox.isSelected,
        )
        val torrentsFromDb = TorrentRepository.getFilteredTorrents(torrentFilter)
        val model = torrentsTable.model as TorrentTableModel
        model.clear()
        var currentTorrentTableItem: TorrentTableItem? = null
        var hasDownloadingStatus = false
        var hasKeepingStatus = false
        var hasSeedingStatus = false
        var hasFullStatus = false
        for (torrentItem in torrentsFromDb) {
            if (currentTorrentTableItem == null || currentTorrentTableItem.topicId == torrentItem.topicId) {
                // дропаем бота статистики
                if (torrentItem.keeper?.name == "StatsBot")
                    continue
                // добавляем хранителей к текущей раздаче
                torrentItem.keeper?.let {
                    when (it.status) {
                        KeeperItem.Status.DOWNLOADING -> hasDownloadingStatus = true
                        KeeperItem.Status.KEEPING -> hasKeepingStatus = true
                        KeeperItem.Status.SEEDING -> hasSeedingStatus = true
                        KeeperItem.Status.FULL -> {
                            hasKeepingStatus = true
                            hasSeedingStatus = true
                            hasFullStatus = true
                        }
                    }
                    if (currentTorrentTableItem == null)
                        currentTorrentTableItem = TorrentTableItem.fromTorrentItem(torrentItem)
                    else
                        currentTorrentTableItem!!.keepers.add(it)
                } ?: kotlin.run {
                    currentTorrentTableItem = TorrentTableItem.fromTorrentItem(torrentItem)
                }
            } else {
                if (currentTorrentTableItem == null)
                    currentTorrentTableItem = TorrentTableItem.fromTorrentItem(torrentItem)
                // добавляем или не добавляем в таблицу
                var meetCriteria = true
                if (hasSeedersCheckbox.isSelected && !hasSeedingStatus || noSeedersCheckbox.isSelected && hasSeedingStatus)
                    meetCriteria = false
                if (noKeepersCheckbox.isSelected && (hasKeepingStatus || hasDownloadingStatus))
                    meetCriteria = false
                if (noDownloadedCheckbox.isSelected && hasDownloadedCheckbox.isSelected && !(hasKeepingStatus || hasDownloadingStatus) ||
                    noDownloadedCheckbox.isSelected && !hasDownloadedCheckbox.isSelected && (hasKeepingStatus || !hasDownloadingStatus) ||
                    hasDownloadedCheckbox.isSelected && !noDownloadedCheckbox.isSelected && !hasKeepingStatus
                )
                    meetCriteria = false

                if (meetCriteria)
                    model.addTorrent(currentTorrentTableItem!!)

                // начинаем новую раздачу
                hasDownloadingStatus = false
                hasKeepingStatus = false
                hasSeedingStatus = false
                hasFullStatus = false
                currentTorrentTableItem = null
            }
        }
    }

    fun resetFilter() {
        keepCheckbox.isSelected = false
        noKeepCheckbox.isSelected = true
        downloadingCheckbox.isSelected = false

        sortAscendingRadio.isSelected = true
        sortSeedsRadio.isSelected = true

        notVerifiedCheckbox.isSelected = false
        verifiedCheckbox.isSelected = true
        notFormalizedCheckbox.isSelected = true
        suspiciousCheckbox.isSelected = true
        temporaryCheckbox.isSelected = false

        lowPriorityCheckbox.isSelected = false
        normalPriorityCheckbox.isSelected = true
        highPriorityCheckbox.isSelected = true

        averageSeedsSpinner.value = 14
        registerDateInput.value = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
        }.time
        searchByPhraseField.text = ""
        searchTitleRadio.isSelected = true

        greenCheckbox.isSelected = false
        noKeepersCheckbox.isSelected = true
        noDownloadedCheckbox.isSelected = false
        hasDownloadedCheckbox.isSelected = false
        noSeedersCheckbox.isSelected = false
        hasSeedersCheckbox.isSelected = false
    }
}