package me.zepsizola.zautobroadcast.command

import me.zepsizola.zautobroadcast.ZAutoBroadcast
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class ReloadCommand(private val plugin: ZAutoBroadcast) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!CommandUtils.hasPermission(sender, ZAutoBroadcast.ADMIN_PERMISSION)) {
            return true
        }
        plugin.reloadConfigs(false)
        sender.sendMessage("Configuration reloaded.")
        return true
    }

}