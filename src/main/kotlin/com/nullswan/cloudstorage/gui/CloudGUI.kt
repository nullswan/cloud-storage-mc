package com.nullswan.cloudstorage.gui

import com.nullswan.cloudstorage.compactNumber
import com.nullswan.cloudstorage.styledText
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
import org.bukkit.inventory.meta.Damageable
import java.text.NumberFormat
import java.util.UUID

class CloudGUI(
    val player: Player,
    private val storage: PlayerStorage,
    var currentPage: Int = 0,
    var shared: Boolean = true
) : InventoryHolder {

    companion object {
        const val ITEMS_PER_PAGE = 45
        const val SLOT_PREV = 45
        const val SLOT_TOGGLE = 46
        const val SLOT_AUTO_CLOUD = 47
        const val SLOT_DEPOSIT_INV = 48
        const val SLOT_DEPOSIT_HELD = 49
        const val SLOT_INFO = 50
        const val SLOT_SEARCH = 51
        const val SLOT_NEXT = 53
    }

    var searchQuery: String? = null

    private var inv: Inventory = createInventory()

    val storageUuid: UUID
        get() = if (shared) PlayerStorage.SHARED_UUID else player.uniqueId

    private fun createInventory(): Inventory {
        val title = if (shared)
            Component.text("☁ Shared Cloud", NamedTextColor.GOLD)
        else
            Component.text("☁ Cloud Storage", NamedTextColor.LIGHT_PURPLE)
        return Bukkit.createInventory(this, 54, title)
    }

    fun open() {
        refresh()
        player.openInventory(inv)
    }

    fun reopen() {
        inv = createInventory()
        refresh()
        player.openInventory(inv)
    }

    sealed class DisplayEntry {
        data class Stackable(val material: Material, val amount: Long) : DisplayEntry()
        data class Unique(val id: Int, val item: ItemStack) : DisplayEntry()
    }

    var displayEntries: List<DisplayEntry> = emptyList()
        private set

    fun refresh() {
        inv.clear()
        val query = searchQuery?.lowercase()

        val stackable = storage.getAll(storageUuid).entries
            .filter { (mat, _) -> query == null || mat.name.lowercase().replace('_', ' ').contains(query) }
            .map { (mat, amount) -> DisplayEntry.Stackable(mat, amount) }

        val unique = storage.getAllUnique(storageUuid)
            .filter { (_, item) -> query == null || item.type.name.lowercase().replace('_', ' ').contains(query) }
            .map { (id, item) -> DisplayEntry.Unique(id, item) }

        displayEntries = stackable + unique
        val totalPages = ((displayEntries.size - 1) / ITEMS_PER_PAGE).coerceAtLeast(0)
        currentPage = currentPage.coerceIn(0, totalPages)

        val fmt = NumberFormat.getIntegerInstance()
        displayEntries.drop(currentPage * ITEMS_PER_PAGE).take(ITEMS_PER_PAGE)
            .forEachIndexed { index, entry ->
                when (entry) {
                    is DisplayEntry.Stackable -> {
                        val displayCount = entry.amount.coerceIn(1, 99).toInt()
                        inv.setItem(index, ItemStack(entry.material, 1).apply {
                            editMeta { meta -> meta.setMaxStackSize(99) }
                            this.amount = displayCount
                            editMeta { meta ->
                                meta.displayName(
                                    Component.text("x${compactNumber(entry.amount)} ", NamedTextColor.YELLOW)
                                        .decoration(TextDecoration.ITALIC, false)
                                        .append(Component.translatable(entry.material.translationKey())
                                            .color(NamedTextColor.WHITE)
                                            .decoration(TextDecoration.ITALIC, false))
                                )
                                meta.lore(listOf(
                                    styledText("Amount: ${fmt.format(entry.amount)}", NamedTextColor.AQUA),
                                    Component.empty(),
                                    styledText("Left-click: withdraw stack", NamedTextColor.GRAY),
                                    styledText("Shift-click: withdraw all", NamedTextColor.GRAY)
                                ))
                            }
                        })
                    }
                    is DisplayEntry.Unique -> {
                        inv.setItem(index, entry.item.clone().apply {
                            editMeta { meta ->
                                val existing = meta.lore() ?: mutableListOf()
                                val lore = existing.toMutableList()
                                lore.add(Component.empty())
                                lore.add(styledText("☁ Cloud-stored", NamedTextColor.LIGHT_PURPLE))
                                lore.add(styledText("Click to withdraw", NamedTextColor.GRAY))
                                meta.lore(lore)
                            }
                        })
                    }
                }
            }

        fillNavBar(currentPage, totalPages, displayEntries.size)
    }

    private fun fillNavBar(page: Int, totalPages: Int, totalItems: Int) {
        val filler = ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1).apply {
            editMeta { it.displayName(Component.text(" ")) }
        }
        for (slot in 45..53) inv.setItem(slot, filler)

        if (page > 0) inv.setItem(SLOT_PREV, navItem(Material.ARROW, "◀ Previous Page", NamedTextColor.YELLOW))
        if (page < totalPages) inv.setItem(SLOT_NEXT, navItem(Material.ARROW, "Next Page ▶", NamedTextColor.YELLOW))

        val (toggleName, toggleColor, toggleMat) = if (shared)
            Triple("Switch to Personal", NamedTextColor.LIGHT_PURPLE, Material.ENDER_CHEST)
        else
            Triple("Switch to Shared", NamedTextColor.GOLD, Material.ENDER_EYE)
        inv.setItem(SLOT_TOGGLE, navItem(toggleMat, toggleName, toggleColor))

        val autoCloud = storage.isAutoCloudEnabled(player.uniqueId)
        val (autoName, autoColor, autoMat) = if (autoCloud)
            Triple("Auto-Cloud: ON", NamedTextColor.GREEN, Material.LIME_DYE)
        else
            Triple("Auto-Cloud: OFF", NamedTextColor.RED, Material.GRAY_DYE)
        inv.setItem(SLOT_AUTO_CLOUD, navItem(autoMat, autoName, autoColor))

        inv.setItem(SLOT_DEPOSIT_INV, navItem(Material.HOPPER, "Deposit Inventory", NamedTextColor.GREEN))
        inv.setItem(SLOT_DEPOSIT_HELD, navItem(Material.CHEST, "Deposit Held Item", NamedTextColor.GREEN))

        if (searchQuery != null) {
            inv.setItem(SLOT_SEARCH, ItemStack(Material.BARRIER, 1).apply {
                editMeta { it.displayName(styledText("Clear Search: \"$searchQuery\"", NamedTextColor.RED)) }
            })
        } else {
            inv.setItem(SLOT_SEARCH, navItem(Material.SPYGLASS, "Search Items", NamedTextColor.AQUA))
        }

        val modeLabel = if (shared) "Shared" else "Personal"
        inv.setItem(SLOT_INFO, ItemStack(Material.BOOK, 1).apply {
            editMeta { meta ->
                meta.displayName(styledText("$modeLabel — Page ${page + 1}/${totalPages + 1}", NamedTextColor.WHITE))
                meta.lore(listOf(styledText("$totalItems item types stored", NamedTextColor.GRAY)))
            }
        })
    }

    private fun navItem(material: Material, name: String, color: NamedTextColor): ItemStack =
        ItemStack(material, 1).apply {
            editMeta { it.displayName(styledText(name, color)) }
        }

    override fun getInventory(): Inventory = inv
}
