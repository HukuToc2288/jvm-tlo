package gui.tabs

import api.keeperRetrofit
import db.TorrentRepository
import entities.db.ForumItem
import entities.db.KeeperItem
import entities.keeper.ForumSize
import entities.keeper.ForumTree
import entities.misc.MainTabSpinnerItem
import entities.misc.TorrentFilterCriteria
import entities.misc.TorrentTableItem
import entities.misc.UpdatedTorrentTableItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import gui.operations.UpdateTopicsDialog
import utils.*
import java.awt.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.Timer
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import javax.swing.SwingUtilities

import javax.swing.JFrame
import java.awt.event.ItemEvent
import javax.swing.JTable

import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.Desktop
import java.net.URL


class MainTab : JPanel(GridBagLayout()) {

    var updateFilterTimer = Timer()

    // Кнопки управления

    val showFilterButton = buildControlButton("filter") {
        firstFilter.isVisible = !fourthFilter.isVisible
        secondFilter.isVisible = !fourthFilter.isVisible
        thirdFilter.isVisible = !fourthFilter.isVisible
        fourthFilter.isVisible = !fourthFilter.isVisible
    }

    val resetFilterButton = buildControlButton("resetFilter") {
        resetFilter()
    }

    val selectAllButton = buildControlButton("selectAll") {
        torrentsTable.selectAll()
    }

    val unselectAllButton = buildControlButton("unselectAll") {
        torrentsTable.clearSelection()
    }

    val invertSelectionButton = buildControlButton("invertSelection") {
        // FIXME: 02.11.2022 если будет тормозить на большой таблице то сделать через интервалы
        for (i in 0 until torrentsTable.rowCount) {
            if (torrentsTable.isRowSelected(i))
                torrentsTable.removeRowSelectionInterval(i, i)
            else
                torrentsTable.addRowSelectionInterval(i, i)
        }
    }

    val updateForumButton = buildControlButton("refresh", "Обновить сведения") {
        val updateForumResult = UpdateTopicsDialog(
            SwingUtilities.getWindowAncestor(this@MainTab) as JFrame
        ).executeTask()
        println(updateForumResult)
        (topicSelector.selectedItem as MainTabSpinnerItem?)?.onSelect?.invoke()
    }

    val testButton = buildControlButton("test", "Для теста разных функций") {


        // TODO: 09.11.2022 сделать следующее:
        // получаем:
        // - торренты из клиента
        // - торренты из базы (зачем?)
        // - темы из базы
        // смотрим есть ли хэш в Topics
        // если есть, то
        // с торрентом всё ок
        // если нет, пытаемся получить id темы
        // если не получили, значит раздача не с рутрекера
        // если получили, то проверяем, есть ли такая тема
        // если есть, то раздача обновлена и с этим надо что-то делать
        // если нет, то раздача удалена?
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

    val searchByKeeperField = JTextField().apply {
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
            //getColumn(3).cellRenderer = KeeperTableCellRender()
        }

        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(mouseEvent: MouseEvent) {
                val table = mouseEvent.getSource() as JTable
                val point: Point = mouseEvent.getPoint()
                val row = table.rowAtPoint(point)
                if (mouseEvent.getClickCount() == 2 && table.selectedRow != -1) {
                    try {
                        Desktop.getDesktop()
                            .browse(URL("https://rutracker.org/forum/viewtopic.php?t=${tableModel.getValueAt(row).topicId}").toURI())
                    } catch (e: Exception) {
                        // TODO: 26.11.2022 если браузер не подхватывается, нужно предусмотреть ввод команды
                    }
                }
            }
        })
    }

    val topicSelector = JComboBox<MainTabSpinnerItem>().apply {
        addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                val item = event.item as MainTabSpinnerItem
                item.onSelect()
                // do something with object
            }
        }
    }

    init {
        buildGui()
    }

    fun buildGui() {
        var constraints = GridBagConstraints()

        addTopicSelectorItems(topicSelector)
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

    private fun addTopicSelectorItems(topicSelector: JComboBox<MainTabSpinnerItem>) {
        with(topicSelector) {
            addItem(
                MainTabSpinnerItem("Раздачи из всех хранимых подразделов") {
                    showKeepingForums()
                    firstFilter.deepSetEnabled(true)
                    secondFilter.deepSetEnabled(true)
                    thirdFilter.deepSetEnabled(true)
                    fourthFilter.deepSetEnabled(true)
                }
            )
            addItem(
                MainTabSpinnerItem("Обновлённые хранимые раздачи") {
                    showUpdatedTopics()
                    firstFilter.deepSetEnabled(false)
                    secondFilter.deepSetEnabled(false)
                    thirdFilter.deepSetEnabled(false)
                    fourthFilter.deepSetEnabled(false)
                }
            )
            addItem(
                MainTabSpinnerItem("Разрегистрированные хранимые раздачи") {
                    showUnregisteredTopics()
                    firstFilter.deepSetEnabled(false)
                    secondFilter.deepSetEnabled(false)
                    thirdFilter.deepSetEnabled(false)
                    fourthFilter.deepSetEnabled(false)
                }
            )
        }
    }

    fun buildControlButtonsPanel(): JComponent {
        val container: JPanel = JPanel()
        container.layout = FlowLayout(FlowLayout.LEFT, 0, 0)
        container.add(showFilterButton)
        container.add(resetFilterButton)
        container.add(Box.createHorizontalStrut(10))
        container.add(selectAllButton)
        container.add(unselectAllButton)
        container.add(invertSelectionButton)
        container.add(Box.createHorizontalStrut(10))
        container.add(updateForumButton)
        container.add(Box.createHorizontalStrut(10))
        container.add(testButton)
        return container
    }

    fun buildControlButton(image: String, text: String? = null, onClick: () -> Unit): JButton {
        val buttonImage = ImageIcon(javaClass.getResource("/res/images/$image.png"))
        val button = JButton(text).apply {
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
                alignmentX = Component.LEFT_ALIGNMENT
            })
            add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JLabel("Дата регистрации до:"))
                add(registerDateInput)
            }.apply {
                alignmentX = Component.LEFT_ALIGNMENT
            })
            add(buildSimpleSeparator().apply {
                alignmentX = Component.LEFT_ALIGNMENT
            })
            add(JPanel(GridBagLayout()).apply {
                border = EmptyBorder(5, 5, 5, 5)
                alignmentX = Component.LEFT_ALIGNMENT
                val constraints = GridBagConstraints()
                constraints.insets = Insets(2, 2, 2, 2)
                constraints.anchor = GridBagConstraints.WEST
                add(JLabel("Поиск"), constraints)
                constraints.gridy = 1
                add(JLabel("Название:"), constraints)
                constraints.gridy = 2
                add(JLabel("Хранитель:"), constraints)
                constraints.gridx = 1
                add(searchByKeeperField, constraints)
                constraints.gridy = 1
                add(searchByPhraseField, constraints)
                constraints.gridy = 0
                constraints.anchor = GridBagConstraints.EAST
                add(JButton("Очистить").apply {
                    addActionListener {
                        searchByPhraseField.text = ""
                        searchByKeeperField.text = ""
                    }
                }, constraints)
            })
        }
        return container
    }

    fun buildFourthFilter(): JPanel {
        val container = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(greenCheckbox.apply {
                alignmentX = Component.LEFT_ALIGNMENT
            })
            add(noKeepersCheckbox.apply {
                alignmentX = Component.LEFT_ALIGNMENT
            })
            add(noDownloadedCheckbox.apply {
                alignmentX = Component.LEFT_ALIGNMENT
            })
            add(hasDownloadedCheckbox.apply {
                alignmentX = Component.LEFT_ALIGNMENT
            })
            add(noSeedersCheckbox.apply {
                alignmentX = Component.LEFT_ALIGNMENT
            })
            add(hasSeedersCheckbox.apply {
                alignmentX = Component.LEFT_ALIGNMENT
            })
            add(buildSimpleSeparator().apply {
                alignmentX = Component.LEFT_ALIGNMENT
            })
            add(JLabel("Количество сидов:").apply {
                alignmentX = Component.LEFT_ALIGNMENT
            })
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                add(JLabel("от"))
                add(seedLowSpinner)
                add(JLabel("до"))
                add(seedHighSpinner)
            }.apply {
                alignmentX = Component.LEFT_ALIGNMENT
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
        checkbox.alignmentX = Component.LEFT_ALIGNMENT
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
                showKeepingForums()
            }

        }, 200)
    }


    private fun showUpdatedTopics() {
        val torrentsFromDb = TorrentRepository.getUpdatedTopics()
        val model = torrentsTable.model as TorrentTableModel
        model.clear()
        var currentTorrentTableItem: UpdatedTorrentTableItem? = null
        for (torrentItem in torrentsFromDb) {
            // начинаем обработку первой раздачи
            if (currentTorrentTableItem == null) {
                currentTorrentTableItem = UpdatedTorrentTableItem.fromUpdatedTorrentItem(torrentItem)
            } else if (torrentItem.oldHash == currentTorrentTableItem.oldHash) {
                torrentItem.torrentItem.keeper?.let { currentTorrentTableItem!!.keepers.add(it) }
            } else {
                // если айдишники разные, начинаем новую раздачу
                //if (checkTableItemCriteria(currentTorrentTableItem))
                model.addTorrent(currentTorrentTableItem)
                currentTorrentTableItem = UpdatedTorrentTableItem.fromUpdatedTorrentItem(torrentItem)
            }
        }
        // добавляем хвост
        currentTorrentTableItem?.let {
            //if (checkTableItemCriteria(currentTorrentTableItem))
            model.addTorrent(currentTorrentTableItem)
        }

        model.commit()
    }

    private fun showUnregisteredTopics() {
        val torrentsFromDb = TorrentRepository.getUnregisteredTopics()
        val model = torrentsTable.model as TorrentTableModel
        model.clear()
        for (torrentItem in torrentsFromDb) {
            model.addTorrent(TorrentTableItem.fromTorrentItem(torrentItem))
        }
        model.commit()
    }

    fun showKeepingForums() {
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
        val torrentsFromDb = TorrentRepository.getKeepingForums(torrentFilter)
        val model = torrentsTable.model as TorrentTableModel
        model.clear()
        var currentTorrentTableItem: TorrentTableItem? = null
        for (torrentItem in torrentsFromDb) {
            // начинаем обработку первой раздачи
            if (currentTorrentTableItem == null) {
                currentTorrentTableItem = TorrentTableItem.fromTorrentItem(torrentItem)
            } else if (torrentItem.topicId == currentTorrentTableItem.topicId) {
                // если равны айдишники, добавляем хранителя, если он есть
                torrentItem.keeper?.let { currentTorrentTableItem!!.keepers.add(it) }
            } else {
                // если айдишники разные, начинаем новую раздачу
                if (checkTableItemCriteria(currentTorrentTableItem))
                    model.addTorrent(currentTorrentTableItem)
                currentTorrentTableItem = TorrentTableItem.fromTorrentItem(torrentItem)
            }
        }
        // добавляем хвост
        currentTorrentTableItem?.let {
            if (checkTableItemCriteria(currentTorrentTableItem))
                model.addTorrent(currentTorrentTableItem)
        }

        model.commit()
        println("final row count is " + model.rowCount)
    }

    fun checkTableItemCriteria(item: TorrentTableItem): Boolean {
        var hasDownloadingStatus = false
        var hasKeepingStatus = false
        var hasSeedingStatus = false
        var hasFullStatus = false
        // собираем параметры хранителей
        for (keeper in item.keepers) {
            when (keeper.status) {
                KeeperItem.Status.DOWNLOADING -> hasDownloadingStatus = true
                KeeperItem.Status.KEEPING -> hasKeepingStatus = true
                KeeperItem.Status.SEEDING -> hasSeedingStatus = true
                KeeperItem.Status.FULL -> {
                    hasKeepingStatus = true
                    hasSeedingStatus = true
                    hasFullStatus = true
                }
            }
        }
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
        // TODO: 02.11.2022 сделать фильтрацию по тексту на лету без обращения к бд
        if (searchByKeeperField.text.isNotEmpty() && !item.keepers.contains(
                KeeperItem(
                    searchByKeeperField.text,
                    KeeperItem.Status.DOWNLOADING
                )
            )
        ) meetCriteria = false

        return meetCriteria
    }


    fun updateForums() {
        if (!TorrentRepository.shouldUpdateForums()) {
            println("Не требуется обновление разделов форума")
            //updateHighPriority()
            return
        }
        keeperRetrofit.catForumTree().enqueue(object : Callback<ForumTree> {
            override fun onResponse(call: Call<ForumTree>, response: Response<ForumTree>) {
                val forumTree = response.body()!!

                keeperRetrofit.forumSize().enqueue(object : Callback<ForumSize> {
                    override fun onResponse(call: Call<ForumSize>, response: Response<ForumSize>) {
                        val forumSize = response.body()!!
                        // всё получили, создаём список
                        val forumList = HashMap<Int, ForumItem>()
                        var maxCount = 0L
                        for (category in forumTree.result.tree) {
                            for (forum in category.value) {
                                // добавляем большой форум если есть раздачи
                                forumSize.result[forum.key]?.let {
                                    forumList.put(
                                        forum.key, ForumItem(
                                            "${forumTree.result.categories[category.key]} »" +
                                                    " ${forumTree.result.forums[forum.key]}",
                                            it[0],
                                            it[1]
                                        )
                                    )
                                }
                                for (subForum in forum.value) {
                                    // добавляем подфорум если есть раздачи
                                    forumSize.result[subForum]?.let {
                                        forumList.put(
                                            subForum, ForumItem(
                                                "${forumTree.result.categories[category.key]} »" +
                                                        " ${forumTree.result.forums[forum.key]} »" +
                                                        " ${forumTree.result.forums[subForum]}",
                                                it[0],
                                                it[1]
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        // обновляем БД
                        // FIXME: 04.11.2022 здесь была ошибка блокировки БД
                        TorrentRepository.updateForums(forumList)
                        println("Список разделов форума успешно обновлён")
                        // updateHighPriority()
                    }

                    override fun onFailure(call: Call<ForumSize>, t: Throwable) {
                        t.printStackTrace()
                        updateForumButton.isEnabled = true
                    }
                })
            }

            override fun onFailure(call: Call<ForumTree>, t: Throwable) {
                t.printStackTrace()
                updateForumButton.isEnabled = true
            }
        })
    }

//    fun updateHighPriority() {
//        // TODO: 03.11.2022 получать высокоприоритетные раздачи
//        // TODO: 03.11.2022 узнать как прилеплять хранителей к высокоприоритеткам
//
//        if (!TorrentRepository.shouldUpdateHighPriority()) {
//            println("Не требуется обновление высокоприоритетных раздач")
//            updateSubsections()
//            return
//        }
//        println("Обновление высокоприоритетных раздач требуется, но не реализовано")
//        updateSubsections()
//
//    }


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
        searchByKeeperField.text = ""

        greenCheckbox.isSelected = false
        noKeepersCheckbox.isSelected = true
        noDownloadedCheckbox.isSelected = false
        hasDownloadedCheckbox.isSelected = false
        noSeedersCheckbox.isSelected = false
        hasSeedersCheckbox.isSelected = false
    }
}