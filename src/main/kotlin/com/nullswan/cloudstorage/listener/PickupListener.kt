package com.nullswan.cloudstorage.listener

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

class PickupListener(
    private val storage: PlayerStorage
) : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockDrop(event: BlockDropItemEvent) {
        val player = event.player
        val items = event.items.toList()

        for (drop in items) {
            val itemStack = drop.itemStack
            if (itemStack.type.isAir) continue

            val leftover = player.inventory.addItem(itemStack)
            if (leftover.isEmpty()) {
                showPickup(player, itemStack.type, itemStack.amount, cloud = false)
            } else {
                val added = itemStack.amount - leftover.values.sumOf { it.amount }
                if (added > 0) showPickup(player, itemStack.type, added, cloud = false)

                if (storage.isAutoCloudEnabled(player.uniqueId)) {
                    val overflow = leftover.values.sumOf { it.amount }
                    storage.deposit(player.uniqueId, itemStack.type, overflow)
                    showPickup(player, itemStack.type, overflow, cloud = true)
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
            showPickup(player, itemStack.type, itemStack.amount, cloud = true)
            return
        }

        if (!event.isCancelled) {
            showPickup(player, itemStack.type, itemStack.amount, cloud = false)
        }
    }

    private fun showPickup(player: Player, material: Material, amount: Int, cloud: Boolean) {
        val (prefix, color) = if (cloud)
            "☁ +" to NamedTextColor.AQUA
        else
            "+" to NamedTextColor.GREEN

        val itemName = Component.translatable(material.translationKey())

        player.sendActionBar(
            Component.text("$prefix $amount ", color).append(itemName.color(color))
        )
    }
}
