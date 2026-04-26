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
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PickupListener(
    private val plugin: Plugin,
    private val storage: PlayerStorage
) : Listener {

    private class PickupBatch {
        val items = mutableMapOf<Material, Int>()
        val cloudItems = mutableMapOf<Material, Int>()
        var lastUpdate = System.currentTimeMillis()
    }

    private val batches = ConcurrentHashMap<UUID, PickupBatch>()

    init {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { flush() },
            Constants.PICKUP_BATCH_INTERVAL_TICKS, Constants.PICKUP_BATCH_INTERVAL_TICKS)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockDrop(event: BlockDropItemEvent) {
        val player = event.player
        val items = event.items.toList()

        for (drop in items) {
            val itemStack = drop.itemStack
            if (itemStack.type.isAir) continue

            val leftover = player.inventory.addItem(itemStack)
            if (leftover.isEmpty()) {
                record(player, itemStack.type, itemStack.amount, cloud = false)
            } else {
                val added = itemStack.amount - leftover.values.sumOf { it.amount }
                if (added > 0) record(player, itemStack.type, added, cloud = false)

                if (storage.isAutoCloudEnabled(player.uniqueId)) {
                    val overflow = leftover.values.sumOf { it.amount }
                    storage.deposit(player.uniqueId, itemStack.type, overflow)
                    record(player, itemStack.type, overflow, cloud = true)
                } else {
                    continue
                }
            }
            event.items.remove(drop)
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onEntityPickup(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        val itemStack = event.item.itemStack
        if (itemStack.type.isAir) return

        val canFit = player.inventory.firstEmpty() != -1 ||
            player.inventory.contents.any { it != null && it.type == itemStack.type && it.amount < it.maxStackSize }

        if (!canFit && storage.isAutoCloudEnabled(player.uniqueId)) {
            event.isCancelled = true
            storage.deposit(player.uniqueId, itemStack.type, itemStack.amount)
            event.item.remove()
            record(player, itemStack.type, itemStack.amount, cloud = true)
            return
        }

        if (!event.isCancelled) {
            record(player, itemStack.type, itemStack.amount, cloud = false)
        }
    }

    private fun record(player: Player, material: Material, amount: Int, cloud: Boolean) {
        val batch = batches.computeIfAbsent(player.uniqueId) { PickupBatch() }
        val target = if (cloud) batch.cloudItems else batch.items
        target.merge(material, amount, Int::plus)
        batch.lastUpdate = System.currentTimeMillis()
    }

    private fun flush() {
        val now = System.currentTimeMillis()
        val iter = batches.entries.iterator()
        while (iter.hasNext()) {
            val (uuid, batch) = iter.next()
            if (now - batch.lastUpdate < Constants.PICKUP_BATCH_DELAY_MS) continue
            iter.remove()

            val player = plugin.server.getPlayer(uuid) ?: continue

            val allItems = batch.items.toMutableMap()
            batch.cloudItems.forEach { (mat, amt) -> allItems.merge(mat, amt, Int::plus) }
            val top = allItems.maxByOrNull { it.value } ?: continue

            val cloudTotal = batch.cloudItems.values.sum()
            val extra = allItems.size - 1
            val suffix = if (extra > 0) " (+$extra more)" else ""

            val (prefix, color) = if (cloudTotal > 0)
                "☁ +" to NamedTextColor.AQUA
            else
                "+" to NamedTextColor.GREEN

            player.sendActionBar(
                Component.text("$prefix ${top.value} ${top.key.displayName()}$suffix", color)
            )
        }
    }
}
