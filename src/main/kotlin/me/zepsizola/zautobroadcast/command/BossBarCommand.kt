package me.zepsizola.zautobroadcast.command

import me.zepsizola.zautobroadcast.ZAutoBroadcast
import me.zepsizola.zautobroadcast.bossbar.BossBarDefinition
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class BossBarCommand(private val plugin: ZAutoBroadcast) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!CommandUtils.hasPermission(sender, ZAutoBroadcast.ADMIN_PERMISSION)) return true

        when (args.firstOrNull()?.lowercase()) {
            "custom" -> custom(sender, label, args)
            "preset" -> {
                if (args.size != 2) {
                    sender.sendMessage("Usage: /$label bossbar preset <key>")
                } else if (plugin.bossBarManager.announcePreset(args[1])) {
                    sender.sendMessage("Bossbar preset '${args[1]}' announced to all players.")
                } else {
                    sender.sendMessage("Unknown bossbar preset '${args[1]}'. Check bossbar.yml and use /$label reload.")
                }
            }
            else -> {
                sender.sendMessage("Usage: /$label bossbar custom <target> <duration> <colour> <content>")
                sender.sendMessage("Usage: /$label bossbar preset <key>")
            }
        }
        return true
    }

    private fun custom(sender: CommandSender, label: String, args: Array<String>) {
        if (args.size < 5) {
            sender.sendMessage("Usage: /$label bossbar custom <target> <duration> <colour> <content>")
            sender.sendMessage("Target: all, @a, or an online player name. Duration: seconds, or -1 for indefinite.")
            return
        }

        val duration = args[2].toLongOrNull()
        if (duration == null || !BossBarDefinition.isValidDuration(duration)) {
            sender.sendMessage("Duration must be -1 or a whole number of seconds between 1 and ${BossBarDefinition.MAX_DURATION_SECONDS}.")
            return
        }
        val colour = BossBarDefinition.parseColour(args[3])
        if (colour == null) {
            sender.sendMessage("Invalid bossbar colour. Use: ${BossBarDefinition.colours.joinToString(", ")}.")
            return
        }

        val allPlayers = args[1].equals("all", ignoreCase = true) || args[1].equals("@a", ignoreCase = true)
        val target = if (allPlayers) null else plugin.server.getPlayerExact(args[1])
        if (!allPlayers && target == null) {
            sender.sendMessage("Player '${args[1]}' is not online. Use all, @a, or an online player name.")
            return
        }

        val content = args.drop(4).joinToString(" ")
        if (content.isBlank()) {
            sender.sendMessage("Please provide the bossbar content in MiniMessage format.")
            return
        }
        val title = try {
            MiniMessage.miniMessage().deserialize(content)
        } catch (exception: IllegalArgumentException) {
            sender.sendMessage("Invalid MiniMessage content: ${exception.message}")
            return
        }

        plugin.bossBarManager.announce(BossBarDefinition(title, duration, colour), target?.uniqueId)
        val audience = if (allPlayers) "all players" else target!!.name
        val lifetime = if (duration == -1L) "until server shutdown" else "for $duration seconds"
        sender.sendMessage("Bossbar announced to $audience $lifetime.")
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): MutableList<String> {
        if (!sender.hasPermission(ZAutoBroadcast.ADMIN_PERMISSION)) return mutableListOf()
        val suggestions = when {
            args.size == 1 -> listOf("custom", "preset")
            args.size == 2 && args[0].equals("preset", ignoreCase = true) -> plugin.bossBarManager.presetKeys()
            args.size == 2 && args[0].equals("custom", ignoreCase = true) ->
                listOf("all", "@a") + plugin.server.onlinePlayers.map { it.name }
            args.size == 3 && args[0].equals("custom", ignoreCase = true) -> listOf("60", "600", "10800", "-1")
            args.size == 4 && args[0].equals("custom", ignoreCase = true) -> BossBarDefinition.colours
            else -> emptyList()
        }
        return suggestions.filter { it.startsWith(args.lastOrNull().orEmpty(), ignoreCase = true) }.toMutableList()
    }
}
