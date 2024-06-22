package me.zepsizola.zautobroadcast

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class MainCommand(private val plugin: ZAutoBroadcast) : CommandExecutor, TabCompleter {
    private val reloadCommand = ReloadCommand(plugin)
    private val broadcastCommand = BroadcastCommand(plugin)
    private val intervalCommand = IntervalCommand(plugin)
    private val customBroadcastCommand = CustomBroadcastCommand(plugin)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            // Handle the case where no subcommand is provided
            return false
        }

        return when (args[0].lowercase()) {
            "reload" -> reloadCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "broadcast" -> broadcastCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "interval" -> intervalCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "custom" -> customBroadcastCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> false
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): MutableList<String>? {
        return when {
            args.size == 1 -> mutableListOf("reload", "broadcast", "interval", "custom")
            args.size == 2 && args[0].equals("broadcast", ignoreCase = true) -> plugin.forcedBroadcasts.keys.toMutableList()
            args.size == 2 && args[0].equals("interval", ignoreCase = true) -> mutableListOf("get", "set")
            else -> null
        }
    }
}