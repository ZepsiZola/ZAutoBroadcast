package me.zepsizola.zautobroadcast

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class IntervalCommand(private val plugin: ZAutoBroadcast) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!CommandUtils.hasPermission(sender, "zautobroadcast.admin")) {
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("The current interval is ${plugin.interval} seconds.")
            return true
        } else if (args[0].equals("get", ignoreCase = true)) {
            sender.sendMessage("The current interval is ${plugin.interval} seconds.")
        } else if (args[0].equals("set", ignoreCase = true)) {
            if (args.size < 2) {
                sender.sendMessage("Please provide the new interval in seconds.")
                return true
            }
            val newInterval = args[1].toLongOrNull()
            if (newInterval == null || newInterval <= 0) {
                sender.sendMessage("Invalid interval. Please provide a positive number.")
                return true
            }
            plugin.config.set("interval", newInterval)
            plugin.saveConfig()
            plugin.reloadConfigs(false)
            sender.sendMessage("Interval has been set to $newInterval seconds.")
        } else {
            sender.sendMessage("Invalid subcommand. Use 'get' to get the current interval or 'set' to set a new interval.")
        }
        return true
    }
}