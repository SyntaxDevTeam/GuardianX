package pl.syntaxerr.commands

import com.destroystokyo.paper.profile.PlayerProfile
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.configuration.PluginMeta
import io.papermc.paper.ban.BanListType
import net.kyori.adventure.text.Component
import org.bukkit.BanList
import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import pl.syntaxerr.GuardianX
import pl.syntaxerr.helpers.Logger
import pl.syntaxerr.helpers.MessageHandler
import pl.syntaxerr.helpers.TimeHandler
import pl.syntaxerr.helpers.UUIDManager
import java.util.*

@Suppress("UnstableApiUsage")
class BanCommand(private val plugin: PunisherX, pluginMetas: PluginMeta) : BasicCommand {

    private var config = plugin.config
    private var debugMode = config.getBoolean("debug")
    private val logger = Logger(pluginMetas, debugMode)
    private val uuidManager = UUIDManager(plugin)
    private val messageHandler = MessageHandler(plugin, pluginMetas)
    private val timeHandler = TimeHandler(plugin.config.getString("language") ?: "PL")

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (args.isNotEmpty()) {
            if (stack.sender.hasPermission("GuardianX.ban")) {
                if (args.size < 2) {
                    stack.sender.sendRichMessage(messageHandler.getMessage("ban", "usage"))
                } else {
                    val player = args[0]
                    val uuid = uuidManager.getUUID(player)
                    if (uuid == null) {
                        stack.sender.sendRichMessage(messageHandler.getMessage("error", "player_not_found", mapOf("player" to player)))
                        return
                    }
                    val gtime = if (args.size > 2) args[1] else null
                    val reason = if (args.size > 2) args.slice(2 until args.size).joinToString(" ") else args[1]
                    val punishmentType = "BAN"
                    val start = System.currentTimeMillis().toString()
                    val end = if (gtime != null) (System.currentTimeMillis() + timeHandler.parseTime(gtime) * 1000).toString() else if (plugin.config.getString("language") == "PL") "nieokreślony" else "undefined"

                    plugin.databaseHandler.addPunishment(player, uuid, reason, stack.sender.name, punishmentType, start, end)
                    plugin.databaseHandler.addPunishmentHistory(player, uuid, reason, stack.sender.name, punishmentType, start, end)
/*
                    val playerProfile = Bukkit.createProfile(UUID.fromString(uuid), player)
                    val banList: BanList<PlayerProfile> = Bukkit.getBanList(BanListType.PROFILE)
                    val banEndDate = if (gtime != null) Date(System.currentTimeMillis() + timeHandler.parseTime(gtime) * 1000) else null
                    banList.addBan(playerProfile, reason, banEndDate, stack.sender.name)
*/
                    val targetPlayer = Bukkit.getPlayer(player)
                    if (targetPlayer != null) {
                        val kickMessages = messageHandler.getComplexMessage("ban", "kick_message", mapOf("reason" to reason, "time" to timeHandler.formatTime(gtime)))
                        val kickMessage = Component.text()
                        kickMessages.forEach { line ->
                            kickMessage.append(line)
                            kickMessage.append(Component.newline())
                        }
                        targetPlayer.kick(kickMessage.build())
                    }

                    stack.sender.sendRichMessage(messageHandler.getMessage("ban", "ban", mapOf("player" to player, "reason" to reason, "time" to timeHandler.formatTime(gtime))))
                    val message = Component.text(messageHandler.getMessage("ban", "ban", mapOf("player" to player, "reason" to reason, "time" to timeHandler.formatTime(gtime))))
                    plugin.server.broadcast(message)
                    logger.info("Player " + player + "(" + uuid + ") has banned for " + reason + " to time " + timeHandler.formatTime(gtime))
                }
            } else {
                stack.sender.sendRichMessage(messageHandler.getMessage("error", "no_permission"))
            }
        } else {
            stack.sender.sendRichMessage(messageHandler.getMessage("ban", "usage"))
        }
    }
}