package utils

import com.fasterxml.jackson.core.JsonGenerationException
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import entities.config.*
import torrentclients.AbstractTorrentClient
import torrentclients.TorrentClientFactory
import java.io.File
import java.io.IOException
import kotlin.jvm.Throws

object ConfigRepository {
    private lateinit var config: Config
    val proxyConfig get() = config.proxyConfig
    val proxies get() = proxyConfig.proxies
    val trackerConfig get() = config.trackerConfig
    val subsectionsConfig get() = config.subsectionsConfig
    val subsections get() = subsectionsConfig.subsections
    val torrentClients = HashMap<Int, AbstractTorrentClient>()
    private val mapper = ObjectMapper()
        .registerKotlinModule()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(SerializationFeature.INDENT_OUTPUT)

    fun createTorrentClientsFromConfig() {
        torrentClients.clear()
        for (torrentClientConfig in config.torrentClients) {
            val torrentClient = TorrentClientFactory.buildFromConfig(torrentClientConfig)
            if (torrentClient == null) {
                // TODO: 01.12.2022 писать это в журнал
                continue
            }
            torrentClients[torrentClientConfig.id] = torrentClient
        }
    }

    @Throws(IOException::class, JsonParseException::class, JsonMappingException::class)
    fun read() {
        val configFile = File("./files/config.json")
        if (!configFile.exists()) {
            // создаём стандартный конфиг и записываем
            config = Config()
            try {
                write()
            } catch (e: Exception) {
                // pass
            }
        }
        config = mapper.readValue(configFile, Config::class.java)
        createTorrentClientsFromConfig()

    }

    @Throws(IOException::class, JsonGenerationException::class, JsonMappingException::class)
    fun write() {
        val configFile = File("./files/config.json")
        mapper.writeValue(configFile, config)
    }
}