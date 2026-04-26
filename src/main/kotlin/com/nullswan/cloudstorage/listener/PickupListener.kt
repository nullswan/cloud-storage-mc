package com.nullswan.cloudstorage.listener

import com.nullswan.cloudstorage.Constants
import com.nullswan.cloudstorage.displayName
import com.nullswan.cloudstorage.storage.PlayerStorage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PickupListener(
    private val plugin: Plugin,
    private val storage: PlayerStorage
) : Listener {

    private class PickupBatch {
        val items = mutableMapOf<Material, Int>()
        var cloudItems = mutableMapOf<Material, Int>()
        var lastUpdate = System.currentTimeMillis()
    }

    private val batches = ConcurrentHashMap<UUID, PickupBatch>()

    init {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { flush() },
            Constants.PICKUP_BATCH_INTERVAL_TICKS, Constants.PICKUP_BATCH_INTERVAL_TICKS)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPickup(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        val itemStack = event.item.itemStack
        if (itemStack.type.isAir) return

        val canFit = player.inventory.firstEmpty() != -1 ||
            player.inventory.contents.any { it != null && it.type == itemStack.type && it.amount < it.maxStackSize }

        if (!canFit && storage.isAutoCloudEnabled(player.uniqueId)) {
            event.isCancelled = true
            storage.deposit(player.uniqueId, itemStack.type, itemStack.amount)
            event.item.remove()

            val batch = batches.computeIfAbsent(player.uniqueId) { PickupBatch() }
            batch.cloudItems.merge(itemStack.type, itemStack.amount, Int::plus)
            batch.lastUpdate = System.currentTimeMillis()
            return
        }

        if (!event.isCancelled) {
            val batch = batches.computeIfAbsent(player.uniqueId) { PickupBatch() }
            batch.items.merge(itemStack.type, itemStack.amount, Int::plus)
            batch.lastUpdate = System.currentTimeMillis()
        }
    }

    private fun flush() {
        val now = System.currentTimeMillis()
        val iter = batches.entries.iterator()
        while (iter.hasNext()) {
            val (uuid, batch) = iter.next()
            if (now - batch.lastUpdate < Constants.PICKUP_BATCH_DELAY_MS) continue
            iter.remove()

            val player = plugin.server.getPlayer(uuid) ?: continue

            val cloudTop = batch.cloudItems.maxByOrNull { it.value }
            if (cloudTop != null) {
                val extra = batch.cloudItems.size - 1
                val suffix = if (extra > 0) " (+$extra more)" else ""
                player.sendActionBar(
                    Component.text("☁ + ${cloudTop.value} ${cloudTop.key.displayName()}$suffix", NamedTextColor.AQUA)
                )
                return
            }

            val top = batch.items.maxByOrNull { it.value } ?: continue
            val extra = batch.items.size - 1
            val suffix = if (extra > 0) " (+$extra more)" else ""
            player.sendActionBar(
                Component.text("+ ${top.value} ${top.key.displayName()}$suffix", NamedTextColor.GREEN)
            )
        }
    }
}
