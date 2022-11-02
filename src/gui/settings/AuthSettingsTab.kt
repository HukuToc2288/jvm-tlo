package gui.settings

import api.forumCookieJar
import api.forumRetrofit
import org.jsoup.Jsoup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Url
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.math.min

class AuthSettingsTab : JPanel(GridBagLayout()) {

    var captchaId: String? = null
    lateinit var captchaParamName: String

    // TODO: 02.11.2022 не все ошибки обработаны, так что лучше особо не косячить при входе
    val authButton = JButton("Авторизоваться").apply {
        addActionListener {
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
                        println("success")
                        isEnabled = true
                        return
                    }
                    // пытаемся выковырить капчу со страницы
                    val responsePage = Jsoup.parse(response.body())
                    val loginElements = responsePage.select("div.mrg_16 > table tr")
                    if (loginElements.size < 3) {
                        // где-то потеряли капчу, такого происходить не должно
                        println("NO CAPTCHA!!")
                        isEnabled = true
                        return
                    }
                    val captchaItem = loginElements[2]
                    // ура у нас есть капча
                    captchaImage.icon = ImageIcon(ImageIO.read(URL(captchaItem.select("img").attr("src"))))
                    captchaId = captchaItem.select("input")[0].attr("value")
                    captchaParamName = captchaItem.select("input")[1].attr("name")
                    captchaField.isVisible = true
                    captchaLabel.isVisible = true
                    isEnabled = true
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
    }
    val passwordField = JPasswordField().apply {
        columns = 20
    }
    val captchaField = JTextField().apply {
        columns = 20
        isVisible = false
    }
    val captchaLabel = JLabel("Капча").apply {
        isVisible = false
    }
    val captchaImage = JLabel()

    init {
        buildGui()
    }

    private fun buildGui() {
        val constraints = GridBagConstraints()
        constraints.insets = Insets(2, 2, 2, 2)

        constraints.gridy = 0
        add(JLabel("Логин"), constraints)

        constraints.gridy = 1
        add(JLabel("Пароль"), constraints)

        constraints.gridy = 2
        add(captchaLabel, constraints)

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
        constraints.gridy = 4
        constraints.gridwidth = 2
        constraints.fill = GridBagConstraints.HORIZONTAL
        add(authButton, constraints)
    }

}