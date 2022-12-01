package utils

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerationException
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import entities.config.*
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
    val torrentClients get() = config.torrentClients
    private val mapper = ObjectMapper()
        .registerKotlinModule()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(SerializationFeature.INDENT_OUTPUT)

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
    }

    @Throws(IOException::class, JsonGenerationException::class, JsonMappingException::class)
    fun write() {
        val configFile = File("./files/config.json")
        mapper.writeValue(configFile, config)
    }
}