package com.nullswan.cloudstorage.listener

import com.nullswan.autopickup.InventoryOverflowEvent
import com.nullswan.cloudstorage.storage.PlayerStorage
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class CloudOverflowListener(private val storage: PlayerStorage) : Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    fun onOverflow(event: InventoryOverflowEvent) {
        if (!storage.isAutoCloudEnabled(event.player.uniqueId)) return
        if (event.material.maxStackSize <= 1) return
        storage.deposit(event.player.uniqueId, event.material, event.amount)
        event.isCancelled = true
    }
}
