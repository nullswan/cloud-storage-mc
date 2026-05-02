package com.nullswan.cloudstorage.listener

import com.nullswan.autopickup.InventoryOverflowEvent
import com.nullswan.cloudstorage.storage.PlayerStorage
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

class CloudOverflowListener(
    private val plugin: Plugin,
    private val storage: PlayerStorage,
    private val debug: Boolean
) : Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    fun onOverflow(event: InventoryOverflowEvent) {
        if (!storage.isAutoCloudEnabled(event.player.uniqueId)) {
            if (debug) plugin.logger.info("[Cloud] overflow ${event.player.name} ${event.material}x${event.amount} → skipped (auto-cloud disabled)")
            return
        }
        if (event.material.maxStackSize <= 1) {
            if (debug) plugin.logger.info("[Cloud] overflow ${event.player.name} ${event.material}x${event.amount} → skipped (non-stackable)")
            return
        }
        storage.deposit(event.player.uniqueId, event.material, event.amount)
        event.isCancelled = true
        if (debug) plugin.logger.info("[Cloud] overflow ${event.player.name} ${event.material}x${event.amount} → deposited")
    }
}
