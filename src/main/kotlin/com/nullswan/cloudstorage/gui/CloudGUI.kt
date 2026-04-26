package com.nullswan.cloudstorage.gui

import com.nullswan.cloudstorage.storage.PlayerStorage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import java.text.NumberFormat

class CloudGUI(
    private val player: Player,
    private val storage: PlayerStorage,
    var currentPage: Int = 0
) : InventoryHolder {

    private val inv: Inventory = Bukkit.createInventory(
        this, 54,
        Component.text("☁ Cloud Storage", NamedTextColor.LIGHT_PURPLE)
    )

    val itemsPerPage = 45

    fun open() {
        refresh()
        player.openInventory(inv)
    }

    fun refresh() {
        inv.clear()
        val allItems = storage.getAll(player.uniqueId).entries.toList()
        val totalPages = ((allItems.size - 1) / itemsPerPage).coerceAtLeast(0)
        currentPage = currentPage.coerceIn(0, totalPages)

        val pageItems = allItems.drop(currentPage * itemsPerPage).take(itemsPerPage)
        val fmt = NumberFormat.getIntegerInstance()

        for ((index, entry) in pageItems.withIndex()) {
            val (material, amount) = entry
            val item = ItemStack(material, 1)
            val meta = item.itemMeta ?: continue
            meta.lore(
                listOf(
                    Component.text("Amount: ${fmt.format(amount)}", NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Left-click: withdraw stack", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                    Component.text("Shift-click: withdraw all", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                )
            )
            item.itemMeta = meta
            inv.setItem(index, item)
        }

        fillNavBar(currentPage, totalPages, allItems.size)
    }

    private fun fillNavBar(page: Int, totalPages: Int, totalItems: Int) {
        val filler = ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1).apply {
            editMeta { it.displayName(Component.text(" ")) }
        }
        for (slot in 45..53) inv.setItem(slot, filler)

        if (page > 0) {
            inv.setItem(45, navItem(Material.ARROW, "◀ Previous Page", NamedTextColor.YELLOW))
        }
        if (page < totalPages) {
            inv.setItem(53, navItem(Material.ARROW, "Next Page ▶", NamedTextColor.YELLOW))
        }

        inv.setItem(48, navItem(Material.HOPPER, "Deposit Inventory", NamedTextColor.GREEN))
        inv.setItem(49, navItem(Material.CHEST, "Deposit Held Item", NamedTextColor.GREEN))

        val infoItem = ItemStack(Material.BOOK, 1).apply {
            editMeta { meta ->
                meta.displayName(
                    Component.text("Page ${page + 1}/${totalPages + 1}", NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false)
                )
                meta.lore(
                    listOf(
                        Component.text("$totalItems item types stored", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                )
            }
        }
        inv.setItem(50, infoItem)
    }

    private fun navItem(material: Material, name: String, color: NamedTextColor): ItemStack {
        return ItemStack(material, 1).apply {
            editMeta { meta ->
                meta.displayName(
                    Component.text(name, color).decoration(TextDecoration.ITALIC, false)
                )
            }
        }
    }

    override fun getInventory(): Inventory = inv
}
