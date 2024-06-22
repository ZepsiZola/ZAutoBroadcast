package me.zepsizola.zautobroadcast.command

import org.bukkit.command.CommandSender

object CommandUtils {
    fun hasPermission(sender: CommandSender, permission: String): Boolean {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage("You do not have permission to use this command.")
            return false
        }
        return true
    }
}