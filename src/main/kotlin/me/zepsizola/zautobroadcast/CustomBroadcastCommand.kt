package me.zepsizola.zautobroadcast

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class CustomBroadcastCommand(private val plugin: ZAutoBroadcast) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!CommandUtils.hasPermission(sender,ZAutoBroadcast.ADMIN_PERMISSION)) {
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("Please provide the custom broadcast message.")
            return true
        }
        val message = List(1) { args.joinToString(" ")}
        plugin.foliaLib.impl.runNextTick { plugin.broadcastMessage(message) }
        return true
    }
}