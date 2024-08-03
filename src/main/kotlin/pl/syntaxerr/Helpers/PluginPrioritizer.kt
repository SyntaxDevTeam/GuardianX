package pl.syntaxerr.helpers

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import pl.syntaxerr.GuardianX

class PluginPrioritizer(private val plugin: GuardianX) : Listener {

    val registeredPlugins = mutableListOf<Pair<String, Int>>()

    fun registerPlugin() {
        val pluginName = plugin.name
        val priority = 2
        val myEvent = CustomEvent(pluginName, priority)
        plugin.logger.info("Calling CustomEvent for plugin: $pluginName with priority: $priority")
        Bukkit.getPluginManager().callEvent(myEvent)
        plugin.logger.info("CustomEvent called for plugin: $pluginName")
    }

    @EventHandler
    fun onMyCustomEvent(event: CustomEvent) {
        plugin.logger.info("Received CustomEvent for plugin: ${event.pluginName} with priority: ${event.priority}")
        registeredPlugins.add(event.pluginName to event.priority)
        plugin.logger.info("Plugin ${event.pluginName} with priority ${event.priority} added to registeredPlugins")
    }

    fun displayLogo() {
        plugin.logger.info("Displaying logo and information about other plugins...")
        plugin.logger.pluginStart(registeredPlugins)
    }
}
