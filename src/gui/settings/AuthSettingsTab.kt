package gui.settings

import api.ForumSession
import api.forumRetrofit
import gui.GuiUtils
import org.jsoup.Jsoup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import utils.ConfigRepository
import utils.ResetBackgroundListener
import java.awt.*
import java.lang.Exception
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.*

class AuthSettingsTab : JPanel(GridBagLayout()), SavableTab {

    var captchaId: String? = null
    lateinit var captchaParamName: String
    val errorBackground = Color(255, 0, 0, 191)

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
            forumRetrofit.login(
                loginField.text,
                passwordField.text,
                captchaId,
                if (captchaId != null) {
                    mapOf(captchaParamName to captchaField.text)
                } else {
                    emptyMap()
                }
            ).enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (ForumSession.hasSession()) {
                        // наверное мы победили
                        ForumSession.save()
                        ConfigRepository.trackerConfig.login = loginField.text
                        buildHasAuthGui()
                        //showAuthCompleted("Авторизация успешна", "lime")
                        return
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
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    authStatusLabel.foreground = Color.RED
                    authStatusLabel.text = "Ошибка: " + t.localizedMessage
                }
            })
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
                authStatusLabel.foreground = Color.YELLOW
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
        constraints.insets = Insets(2, 2, 2, 2)

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
        constraints.gridy = 5
        constraints.gridwidth = 2
        add(checkSessionButton, constraints)

        constraints.gridy = 6
        add(authButton, constraints)

        constraints.gridy = 4
        constraints.weighty = 1.0
        constraints.anchor = GridBagConstraints.NORTH
        add(authStatusLabel, constraints)
        authButton.isEnabled = true
        loginField.text = ConfigRepository.trackerConfig.login
        repaint()
    }

    private fun buildHasAuthGui() {
        removeAll()

        val constraints = GridBagConstraints()
        constraints.insets = Insets(2, 2, 2, 2)

        constraints.anchor = GridBagConstraints.CENTER
        constraints.gridy = 0
        constraints.gridx = 0
        add(authStatusLabel, constraints)

        constraints.gridy++
        add(checkSessionButton, constraints)

        constraints.gridy++
        add(logoutButton, constraints)

        authStatusLabel.foreground = GuiUtils.defaultTextColor
        authStatusLabel.text = "Вы авторизованы как ${ConfigRepository.trackerConfig.login}"
        passwordField.text = ""
        repaint()
    }

    override fun saveSettings(): Boolean {
        return true
    }
}
