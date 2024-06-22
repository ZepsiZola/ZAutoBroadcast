package me.zepsizola.zautobroadcast

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class CustomBroadcastCommand(private val plugin: ZAutoBroadcast) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!CommandUtils.hasPermission(sender,"zautobroadcast.custom")) {
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("Please provide the custom broadcast message.")
            return true
        }
        val message = args.joinToString(" ")
        plugin.foliaLib.impl.runNextTick  { Bukkit.getServer().sendMessage(MiniMessage.miniMessage().deserialize(message)) }
        return true
    }
}