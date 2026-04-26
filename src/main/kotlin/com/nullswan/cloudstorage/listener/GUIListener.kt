package com.nullswan.cloudstorage.listener

import com.nullswan.cloudstorage.gui.CloudGUI
import com.nullswan.cloudstorage.storage.PlayerStorage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.ItemStack

class GUIListener(private val storage: PlayerStorage) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder
        if (holder !is CloudGUI) return
        event.isCancelled = true

        val player = event.whoClicked as? Player ?: return
        val slot = event.rawSlot

        if (slot < 0 || slot >= 54) return

        when {
            slot < 45 -> handleItemClick(player, holder, slot, event.isShiftClick)
            slot == 45 -> {
                holder.currentPage--
                holder.refresh()
            }
            slot == 53 -> {
                holder.currentPage++
                holder.refresh()
            }
            slot == 48 -> depositInventory(player, holder)
            slot == 49 -> depositHeld(player, holder)
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        if (event.inventory.holder is CloudGUI) {
            event.isCancelled = true
        }
    }

    private fun handleItemClick(player: Player, gui: CloudGUI, slot: Int, shiftClick: Boolean) {
        val displayItem = gui.inventory.getItem(slot) ?: return
        val material = displayItem.type
        val stored = storage.getAmount(player.uniqueId, material)
        if (stored <= 0) return

        if (shiftClick) {
            var totalWithdrawn = 0
            while (true) {
                val emptySlot = player.inventory.firstEmpty()
                if (emptySlot == -1) break
                val remaining = storage.getAmount(player.uniqueId, material)
                if (remaining <= 0) break
                val stackSize = material.maxStackSize.toLong().coerceAtMost(remaining).toInt()
                val actual = storage.withdraw(player.uniqueId, material, stackSize)
                if (actual <= 0) break
                player.inventory.setItem(emptySlot, ItemStack(material, actual))
                totalWithdrawn += actual
            }
            if (totalWithdrawn > 0) {
                player.sendActionBar(
                    Component.text("Withdrew $totalWithdrawn ${formatMaterial(material)}", NamedTextColor.AQUA)
                )
            }
        } else {
            val stackSize = material.maxStackSize.toLong().coerceAtMost(stored).toInt()
            val actual = storage.withdraw(player.uniqueId, material, stackSize)
            if (actual > 0) {
                val leftover = player.inventory.addItem(ItemStack(material, actual))
                if (leftover.isNotEmpty()) {
                    val returned = leftover.values.sumOf { it.amount }
                    storage.deposit(player.uniqueId, material, returned)
                }
            }
        }
        gui.refresh()
    }

    private fun depositInventory(player: Player, gui: CloudGUI) {
        var deposited = 0
        for (i in 9 until 36) {
            val item = player.inventory.getItem(i) ?: continue
            if (!item.type.isItem || item.type.isAir) continue
            storage.deposit(player.uniqueId, item.type, item.amount)
            deposited += item.amount
            player.inventory.setItem(i, null)
        }
        if (deposited > 0) {
            player.sendActionBar(
                Component.text("Deposited $deposited items", NamedTextColor.GREEN)
            )
        }
        gui.refresh()
    }

    private fun depositHeld(player: Player, gui: CloudGUI) {
        val item = player.inventory.itemInMainHand
        if (item.type.isAir) return
        storage.deposit(player.uniqueId, item.type, item.amount)
        player.sendActionBar(
            Component.text(
                "Deposited ${item.amount} ${formatMaterial(item.type)}",
                NamedTextColor.GREEN
            )
        )
        player.inventory.setItemInMainHand(null)
        gui.refresh()
    }

    private fun formatMaterial(material: org.bukkit.Material): String {
        return material.name.lowercase().replace('_', ' ')
    }
}
