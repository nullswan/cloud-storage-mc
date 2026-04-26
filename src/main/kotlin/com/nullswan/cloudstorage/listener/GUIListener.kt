package com.nullswan.cloudstorage.listener

import com.nullswan.cloudstorage.Constants
import com.nullswan.cloudstorage.displayName
import com.nullswan.cloudstorage.gui.CloudGUI
import com.nullswan.cloudstorage.storage.PlayerStorage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.ItemStack
import java.util.UUID

class GUIListener(private val storage: PlayerStorage) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val gui = event.inventory.holder as? CloudGUI ?: return
        event.isCancelled = true

        val player = event.whoClicked as? Player ?: return
        val slot = event.rawSlot
        if (slot < 0 || slot >= Constants.INVENTORY_SIZE) return

        when (slot) {
            in 0 until CloudGUI.ITEMS_PER_PAGE -> handleItemClick(player, gui, slot, event.isShiftClick)
            CloudGUI.SLOT_PREV -> { gui.currentPage--; gui.refresh() }
            CloudGUI.SLOT_TOGGLE -> { gui.shared = !gui.shared; gui.currentPage = 0; gui.reopen() }
            CloudGUI.SLOT_AUTO_CLOUD -> toggleAutoCloud(player, gui)
            CloudGUI.SLOT_DEPOSIT_INV -> depositInventory(player, gui)
            CloudGUI.SLOT_DEPOSIT_HELD -> depositHeld(player, gui)
            CloudGUI.SLOT_NEXT -> { gui.currentPage++; gui.refresh() }
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        if (event.inventory.holder is CloudGUI) event.isCancelled = true
    }

    private fun handleItemClick(player: Player, gui: CloudGUI, slot: Int, shiftClick: Boolean) {
        val displayItem = gui.inventory.getItem(slot) ?: return
        val material = displayItem.type
        val uuid = gui.storageUuid

        if (storage.getAmount(uuid, material) <= 0) return

        if (shiftClick) withdrawAll(player, uuid, material) else withdrawStack(player, uuid, material)
        gui.refresh()
    }

    private fun withdrawStack(player: Player, uuid: UUID, material: Material) {
        val stored = storage.getAmount(uuid, material)
        val stackSize = material.maxStackSize.toLong().coerceAtMost(stored).toInt()
        val actual = storage.withdraw(uuid, material, stackSize)
        if (actual <= 0) return

        val leftover = player.inventory.addItem(ItemStack(material, actual))
        if (leftover.isNotEmpty()) {
            storage.deposit(uuid, material, leftover.values.sumOf { it.amount })
        }
    }

    private fun withdrawAll(player: Player, uuid: UUID, material: Material) {
        var total = 0
        while (player.inventory.firstEmpty() != -1) {
            val remaining = storage.getAmount(uuid, material)
            if (remaining <= 0) break
            val actual = storage.withdraw(uuid, material, material.maxStackSize.toLong().coerceAtMost(remaining).toInt())
            if (actual <= 0) break
            player.inventory.setItem(player.inventory.firstEmpty(), ItemStack(material, actual))
            total += actual
        }
        if (total > 0) {
            player.sendActionBar(Component.text("Withdrew $total ${material.displayName()}", NamedTextColor.AQUA))
        }
    }

    private fun toggleAutoCloud(player: Player, gui: CloudGUI) {
        val current = storage.isAutoCloudEnabled(player.uniqueId)
        storage.setAutoCloud(player.uniqueId, !current)
        val state = if (!current) "ON" else "OFF"
        player.sendActionBar(Component.text("Auto-Cloud: $state", NamedTextColor.YELLOW))
        gui.refresh()
    }

    private fun depositInventory(player: Player, gui: CloudGUI) {
        val uuid = gui.storageUuid
        var deposited = 0
        for (i in Constants.INVENTORY_SLOTS_START until Constants.INVENTORY_SLOTS_END) {
            val item = player.inventory.getItem(i) ?: continue
            if (!item.type.isItem || item.type.isAir) continue
            storage.deposit(uuid, item.type, item.amount)
            deposited += item.amount
            player.inventory.setItem(i, null)
        }
        if (deposited > 0) {
            player.sendActionBar(Component.text("Deposited $deposited items", NamedTextColor.GREEN))
        }
        gui.refresh()
    }

    private fun depositHeld(player: Player, gui: CloudGUI) {
        val item = player.inventory.itemInMainHand
        if (item.type.isAir) return
        val uuid = gui.storageUuid
        storage.deposit(uuid, item.type, item.amount)
        player.sendActionBar(
            Component.text("Deposited ${item.amount} ${item.type.displayName()}", NamedTextColor.GREEN)
        )
        player.inventory.setItemInMainHand(null)
        gui.refresh()
    }
}
