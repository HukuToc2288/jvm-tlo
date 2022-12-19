package gui.settings

import api.ForumSession
import api.forumClient
import api.forumRetrofit
import gui.GuiUtils
import okhttp3.Request
import org.jsoup.Jsoup
import utils.ConfigRepository
import utils.ResetBackgroundListener
import java.awt.*
import java.io.IOException
import java.lang.Exception
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*

class AuthSettingsTab : JPanel(GridBagLayout()), SavableTab {

    var captchaId: String? = null
    lateinit var captchaParamName: String
    val errorBackground = Color(255, 0, 0, 191)
    val keeperKeysDateFormat = SimpleDateFormat("dd-MM-yy")

    // TODO: 02.11.2022 не все ошибки обработаны, так что лучше особо не косячить при входе
    val authButton = JButton("Авторизоваться").apply {
        addActionListener {
            if (loginField.text.isEmpty()) {
                loginField.background = errorBackground
                return@addActionListener
            }
            if (passwordField.text.isEmpty()) {
                passwordField.background = errorBackground
                return@addActionListener
            }
            if (captchaField.isVisible && captchaField.text.isEmpty()) {
                captchaField.background = errorBackground
                return@addActionListener
            }
            isEnabled = false
            authStatusLabel.foreground = GuiUtils.defaultTextColor
            authStatusLabel.text = "Попытка авторизоваться..."
            Thread {
                try {
                    val response = forumRetrofit.login(
                        loginField.text,
                        passwordField.text,
                        captchaId,
                        if (captchaId != null) {
                            mapOf(captchaParamName to captchaField.text)
                        } else {
                            emptyMap()
                        }
                    ).execute()

                    if (ForumSession.hasSession()) {
                        // наверное мы победили
                        ForumSession.save()
                        ConfigRepository.trackerConfig.login = loginField.text
                        try {
                            // попробуем получить "красивый" логин и ключи
                            val responsePage = Jsoup.parse(response.body())
                            val profileLink = responsePage.selectFirst("#logged-in-username")
                            ConfigRepository.trackerConfig.login = profileLink?.text() ?: throw NullPointerException()
                            requestKeeperKeys(profileLink.attr("href"))
                        } catch (e: Exception) {
                            ConfigRepository.trackerConfig.login = loginField.text
                        }
                        buildHasAuthGui()
                        return@Thread
                    }
                    val responsePage = Jsoup.parse(response.body())

                    // при неудачной авторизации есть ненулевой шанс получить текст ошибки
                    var errorMessage = try {
                        responsePage.select("h4.mrg_16")[0].text()
                    } catch (e: Exception) {
                        "Неизвестная ошибка"
                    }
                    // пытаемся выковырить капчу со страницы
                    val loginElements = responsePage.select("div.mrg_16 > table tr")
                    val captchaItem = loginElements[2]
                    // ура у нас есть капча
                    captchaImage.icon = ImageIcon(ImageIO.read(URL(captchaItem.select("img").attr("src"))))
                    captchaId = captchaItem.select("input")[0].attr("value")
                    captchaParamName = captchaItem.select("input")[1].attr("name")
                    captchaField.isVisible = true
                    captchaLabel.isVisible = true
                    captchaField.text = ""
                    authStatusLabel.foreground = Color.RED
                    authStatusLabel.text = errorMessage
                    this@apply.isEnabled = true
                } catch (e: IOException) {
                    authStatusLabel.foreground = Color.RED
                    authStatusLabel.text = "Ошибка: " + e.localizedMessage
                }
            }.start()
        }
    }
    val logoutButton = JButton("Выйти").apply {
        addActionListener {
            ForumSession.reset()
            buildNoAuthGui()
            authStatusLabel.text = ""
        }
    }
    val checkSessionButton = JButton("Проверить доступ").apply {
        addActionListener {
            val hadSession = ForumSession.hasSession()
            val response = try {
                forumRetrofit.getTracker().execute()
            } catch (e: Exception) {
                authStatusLabel.foreground = Color.RED
                authStatusLabel.text = "Трекер недоступен: ${e.message}"
                return@addActionListener
            }
            if (!hadSession) {
                authStatusLabel.foreground = Color.GREEN
                authStatusLabel.text = "Трекер доступен"
            } else if (ForumSession.needAuth(response)) {
                authStatusLabel.foreground = Color.YELLOW
                authStatusLabel.text = "Трекер доступен, но сессия устарела — авторизуйтесь заново"
                buildNoAuthGui()

            } else {
                authStatusLabel.foreground = Color.GREEN
                authStatusLabel.text = "Трекер доступен, сессия актуальна"
            }
        }
    }
    val loginField = JTextField().apply {
        columns = 20
        document.addDocumentListener(ResetBackgroundListener(this))
    }
    val passwordField = JPasswordField().apply {
        columns = 20
        document.addDocumentListener(ResetBackgroundListener(this))
    }
    val captchaField = JTextField().apply {
        columns = 20
        isVisible = false
        document.addDocumentListener(ResetBackgroundListener(this))
    }
    val captchaLabel = JLabel("Капча").apply {
        isVisible = false
    }
    val captchaImage = JLabel()

    val authStatusLabel = JLabel("", SwingConstants.CENTER)

    val btKeyField = JTextField().apply {
        text = ConfigRepository.trackerConfig.btKey
        columns = 10
    }
    val apiKeyField = JTextField().apply {
        text = ConfigRepository.trackerConfig.apiKey
        columns = 10
    }
    val idKeyField = JTextField().apply {
        text = ConfigRepository.trackerConfig.btKey
        columns = 10
    }
    val keeperKeysContainer = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
        isVisible = false
        add(JLabel("bt"))
        add(btKeyField)
        add(JLabel("api"))
        add(apiKeyField)
        add(JLabel("id"))
        add(idKeyField)
    }
    val loadKeeperKeysButton = JButton("Обновить").apply {
        addActionListener {
            Thread {
                requestKeeperKeys(null)
            }.start()
        }
    }
    val manualKeeperKeysButton = JButton("Ввести вручную").apply {
        addActionListener {
            keeperKeysContainer.isVisible = !keeperKeysContainer.isVisible
        }
    }
    val authLabel = JLabel().apply {
        font = Font(font.family, Font.BOLD, 16)
    }

    init {
        buildGui()
    }

    private fun buildGui() {
        if (ForumSession.hasSession())
            buildHasAuthGui()
        else
            buildNoAuthGui()
    }

    private fun buildNoAuthGui() {
        removeAll()

        val constraints = GridBagConstraints()
        constraints.insets = Insets(2, 5, 2, 5)

        constraints.gridy = 0
        add(JLabel("Логин"), constraints)

        constraints.gridy = 1
        add(JLabel("Пароль"), constraints)

        constraints.gridy = 2
        add(captchaLabel, constraints)

        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.weightx = 1.0
        constraints.gridx = 1
        constraints.gridy = 0
        add(loginField, constraints)

        constraints.gridy = 1
        add(passwordField, constraints)

        constraints.gridy = 2
        add(captchaField, constraints)

        constraints.gridy = 3
        add(captchaImage, constraints)

        constraints.gridx = 0
        constraints.gridy = 50
        constraints.gridwidth = 2
        add(checkSessionButton, constraints)

        constraints.gridy = 51
        add(authButton, constraints)

        constraints.gridy = 49
        constraints.weighty = 1.0
        constraints.anchor = GridBagConstraints.SOUTH
        add(authStatusLabel, constraints)

        authButton.isEnabled = true
        loginField.text = ConfigRepository.trackerConfig.login

        revalidate()
        repaint()
    }

    private fun buildHasAuthGui() {
        removeAll()

        val constraints = GridBagConstraints()
        constraints.insets = Insets(2, 5, 2, 5)

        constraints.anchor = GridBagConstraints.WEST
        constraints.gridy = 0
        constraints.gridx = 0
        authLabel.text = "Вы авторизованы как ${ConfigRepository.trackerConfig.login}"
        add(authLabel, constraints)

        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.gridy = 50
        constraints.gridwidth = 2
        add(checkSessionButton, constraints)

        constraints.gridy = 51
        add(logoutButton, constraints)

        constraints.gridy = 49
        constraints.weighty = 1.0
        constraints.anchor = GridBagConstraints.SOUTH
        add(authStatusLabel, constraints)

        passwordField.text = ""

        addKeeperKeysLayout()
        revalidate()
        repaint()
    }

    fun addKeeperKeysLayout() {
        val constraints = GridBagConstraints()
        constraints.insets = Insets(2, 2, 2, 2)
        constraints.gridy = 10
        constraints.gridwidth = 2
        constraints.weightx = 1.0
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.anchor = GridBagConstraints.NORTH
        val keeperKeysLayout = JPanel(FlowLayout(0)).apply {
            add(JLabel("Хранительские ключи:"))
            add(loadKeeperKeysButton)
            add(manualKeeperKeysButton)
        }
        add(keeperKeysLayout, constraints)

        constraints.weighty = 1.0
        constraints.gridy++
        add(keeperKeysContainer, constraints)
    }


    @Throws(Exception::class)
    fun requestKeeperKeys(profileLink: String?) {
        authStatusLabel.foreground = GuiUtils.defaultTextColor
        if (!ForumSession.hasSession()){
            authStatusLabel.text = "Сначала авторизуйтесь"
            return
        }
        authStatusLabel.text = "Запрос хранительских ключей..."
        loadKeeperKeysButton.isEnabled = false
        try {
            // ключи вытаскиваем в 2 этапа, получаем ссылку на профиль, если её нет, а затем получаем ключи со страницы
            val profileLinkNotNull = profileLink
                ?: try {
                    val response = forumRetrofit.getTracker().execute()
                    if (ForumSession.needAuth(response)){
                        authStatusLabel.text = "Сессия устарела, авторизуйтесь заново"
                        buildNoAuthGui()
                        return
                    }
                    if (response.body() == null)
                        throw Exception(response.message())
                    val responsePage = Jsoup.parse(response.body()!!)
                    responsePage.selectFirst("#logged-in-username")?.attr("href")
                        ?: throw NullPointerException("Не удалось найти ссылку на профиль")
                } catch (e: Exception) {
                    throw Exception("Не получена ссылка на профиль: " + e.localizedMessage)
                }

            try {
                val response = forumClient.newCall(
                    Request.Builder()
                        .get()
                        .url(profileLinkNotNull)
                        .build()
                ).execute()
                if (response.body() == null)
                    throw Exception(response.message())
                val responsePage = Jsoup.parse(response.body()!!.string())
                val keysItems = responsePage.selectFirst("th:matchesOwn(Хранительские ключи:)")?.parent()
                    ?.selectFirst("td")?.text()?.split(' ')
                    ?: throw Exception("Не удалось найти ключи на странице. Если вы ещё не стали официальным хранителем, то, скорее всего, у вас их нет")
                if (keysItems.size % 2 != 0) throw Exception("Не удалось разобрать ключи. Возможно, изменился формат ключей на сайте")
                for (i in keysItems.indices step 2) {
                    when (keysItems[i]) {
                        "bt:" -> btKeyField
                        "api:" -> apiKeyField
                        "id:" -> idKeyField
                        else -> null
                    }?.text = keysItems[i + 1]
                }
                if (btKeyField.text.isEmpty() || apiKeyField.text.isEmpty() || idKeyField.text.isEmpty())
                    throw Exception("Не все ключи были получены. Возможно, изменился формат ключей на сайте")
            } catch (e: Exception) {
                throw Exception("Не получены ключи: " + e.localizedMessage)
            }
            with(ConfigRepository.trackerConfig) {
                btKey = btKeyField.text
                apiKey = apiKeyField.text
                idKey = idKeyField.text
                keysDate = keeperKeysDateFormat.format(Date())
            }
            authStatusLabel.foreground = Color.GREEN
            authStatusLabel.text = "Ключи успешно получены"
        } catch (e: Exception) {
            authStatusLabel.foreground = Color.RED
            authStatusLabel.text = "Не получены ключи: " + e.localizedMessage
        } finally {
            loadKeeperKeysButton.isEnabled = true
        }
    }

    override fun saveSettings(): Boolean {
        return true
    }
}
