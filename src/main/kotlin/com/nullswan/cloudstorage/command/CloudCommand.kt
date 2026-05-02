package com.nullswan.cloudstorage.command

import com.nullswan.cloudstorage.gui.CloudGUI
import com.nullswan.cloudstorage.storage.PlayerStorage
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

class CloudCommand(
    private val plugin: Plugin,
    private val storage: PlayerStorage,
    private val debug: Boolean
) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players can use this command.")
            return true
        }
        if (debug) plugin.logger.info("[Cloud] /cloud opened by ${sender.name}")
        CloudGUI(sender, storage).open()
        return true
    }
}
