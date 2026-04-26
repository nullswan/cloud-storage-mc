package com.nullswan.cloudstorage.listener

import com.nullswan.cloudstorage.Constants
import com.nullswan.cloudstorage.displayName
import com.nullswan.cloudstorage.gui.CloudGUI
import com.nullswan.cloudstorage.storage.PlayerStorage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
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

        if (slot < 0) return

        if (slot >= Constants.INVENTORY_SIZE) {
            depositClickedItem(player, gui, event)
            return
        }

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

    private fun depositClickedItem(player: Player, gui: CloudGUI, event: InventoryClickEvent) {
        val clicked = event.currentItem ?: return
        if (clicked.type.isAir) return

        val uuid = gui.storageUuid
        storage.deposit(uuid, clicked.type, clicked.amount)
        player.sendActionBar(
            Component.text("Deposited ${clicked.amount} ${clicked.type.displayName()}", NamedTextColor.GREEN)
        )
        event.currentItem = null
        if (gui.shared) notifySharedCloud(player, clicked.type, clicked.amount, deposited = true)
        gui.refresh()
    }

    private fun handleItemClick(player: Player, gui: CloudGUI, slot: Int, shiftClick: Boolean) {
        val displayItem = gui.inventory.getItem(slot) ?: return
        val material = displayItem.type
        val uuid = gui.storageUuid

        if (storage.getAmount(uuid, material) <= 0) return

        val withdrawn = if (shiftClick) withdrawAll(player, uuid, material) else withdrawStack(player, uuid, material)
        if (gui.shared && withdrawn > 0) notifySharedCloud(player, material, withdrawn, deposited = false)
        gui.refresh()
    }

    private fun withdrawStack(player: Player, uuid: UUID, material: Material): Int {
        val stored = storage.getAmount(uuid, material)
        val stackSize = material.maxStackSize.toLong().coerceAtMost(stored).toInt()
        val actual = storage.withdraw(uuid, material, stackSize)
        if (actual <= 0) return 0

        val leftover = player.inventory.addItem(ItemStack(material, actual))
        if (leftover.isNotEmpty()) {
            val returned = leftover.values.sumOf { it.amount }
            storage.deposit(uuid, material, returned)
            return actual - returned
        }
        return actual
    }

    private fun withdrawAll(player: Player, uuid: UUID, material: Material): Int {
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
        return total
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
            if (gui.shared) notifySharedCloud(player, null, deposited, deposited = true)
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
        if (gui.shared) notifySharedCloud(player, item.type, item.amount, deposited = true)
        player.inventory.setItemInMainHand(null)
        gui.refresh()
    }

    private fun notifySharedCloud(actor: Player, material: Material?, amount: Int, deposited: Boolean) {
        val action = if (deposited) "deposited" else "withdrew"
        val itemDesc = if (material != null) "$amount ${material.displayName()}" else "$amount items"
        val msg = Component.text("[Shared Cloud] ", NamedTextColor.GOLD)
            .append(Component.text("${actor.name} $action $itemDesc", NamedTextColor.GRAY))

        for (player in Bukkit.getOnlinePlayers()) {
            if (player.uniqueId != actor.uniqueId) {
                player.sendMessage(msg)
            }
        }
    }
}
