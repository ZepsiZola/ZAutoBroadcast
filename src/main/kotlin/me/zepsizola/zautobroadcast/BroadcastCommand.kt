package me.zepsizola.zautobroadcast

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class BroadcastCommand(private val plugin: ZAutoBroadcast) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!CommandUtils.hasPermission(sender,"zautobroadcast.admin")) {
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("Please provide a key for a broadcast message.")
            return true
        }
        val key = args[0]
        val broadcast = plugin.getBroadcastMessage(key)
        if (broadcast == null) {
            sender.sendMessage("Invalid key for broadcast message.")
            return true
        }
        plugin.foliaLib.impl.runNextTick { broadcast.forEach { Bukkit.getServer().sendMessage(it) } }
        return true
    }
}