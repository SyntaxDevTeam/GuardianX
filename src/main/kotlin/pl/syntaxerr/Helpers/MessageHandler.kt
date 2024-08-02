package pl.syntaxerr.helpers

import io.papermc.paper.plugin.configuration.PluginMeta
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

@Suppress("UnstableApiUsage")
class MessageHandler(private val plugin: JavaPlugin, pluginMetas: PluginMeta) {
    private var messages: FileConfiguration
    private val language = plugin.config.getString("language") ?: "PL"
    private var debugMode = plugin.config.getBoolean("debug")
    private val logger = Logger(pluginMetas.name, pluginMetas.version, pluginMetas.name, debugMode)

    init {
        copyDefaultMessages()
        messages = loadMessages()
    }

    private fun copyDefaultMessages() {
        val messageFile = File(plugin.dataFolder, "lang/messages_${language.lowercase()}.yml")
        if (!messageFile.exists()) {
            messageFile.parentFile.mkdirs()
            plugin.saveResource("lang/messages_${language.lowercase()}.yml", false)
        }
    }

    private fun loadMessages(): FileConfiguration {
        val langFile = File(plugin.dataFolder, "lang/messages_${language.lowercase()}.yml")
        return YamlConfiguration.loadConfiguration(langFile)
    }

    fun reloadMessages() {
        messages = loadMessages()
    }

    fun getMessage(category: String, key: String, placeholders: Map<String, String> = emptyMap()): String {
        val message = messages.getString("$category.$key") ?: "Message not found. Check console..."
        logger.err("There was an error loading the message $key from category $category")
        return placeholders.entries.fold(message) { acc, entry ->
            acc.replace("{${entry.key}}", entry.value)
        }
    }
}
