package gui.settings

import api.forumCookieJar
import api.forumRetrofit
import org.ini4j.spi.EscapeTool
import org.jsoup.Jsoup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Url
import utils.ResetBackgroundListener
import utils.Settings
import utils.unquote
import java.awt.*
import java.lang.Exception
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.math.log
import kotlin.math.min

class AuthSettingsTab : JPanel(GridBagLayout()) {

    var captchaId: String? = null
    lateinit var captchaParamName: String
    val errorBackground = Color(255,0,0,191)

    // TODO: 02.11.2022 не все ошибки обработаны, так что лучше особо не косячить при входе
    val authButton = JButton("Авторизоваться").apply {
        addActionListener {
            if (loginField.text.isEmpty()){
                loginField.background = errorBackground
                return@addActionListener
            }
            if (passwordField.text.isEmpty()){
                passwordField.background = errorBackground
                return@addActionListener
            }
            if (captchaField.isVisible && captchaField.text.isEmpty()){
                captchaField.background = errorBackground
                return@addActionListener
            }
            isEnabled = false
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
                    if (forumCookieJar.hasCookie("bb_session")) {
                        // наверное мы победили
                        showAuthCompleted("Авторизация успешна", "lime")
                        isEnabled = true
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
                    showAuthCompleted(errorMessage, "red")
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    isEnabled = true
                    t.printStackTrace()
                }
            })
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
        // FIXME: 03.11.2022 если текст ошибки не влезает в строчку, все поля скукоживаются
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
        add(authButton, constraints)

        constraints.gridy = 4
        constraints.weighty = 1.0
        constraints.anchor = GridBagConstraints.NORTH
        add(authStatusLabel, constraints)

        loginField.text = Settings.node("torrent-tracker")["login", ""].unquote()
        passwordField.text = Settings.node("torrent-tracker")["password", ""].unquote()
    }

    private fun showAuthStatus(message: String, color: String? = null) {
        authStatusLabel.text = if (color != null) {
            "<html><font color='$color'>$message</font></html"
        } else {
            message
        }
    }

    private fun showAuthCompleted(message: String, color: String? = null) {
        authStatusLabel.text = if (color != null) {
            "<html><font color='$color'>$message</font></html"
        } else {
            message
        }
        authButton.isEnabled = true
    }
}
