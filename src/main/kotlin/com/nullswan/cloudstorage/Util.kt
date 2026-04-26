package com.nullswan.cloudstorage

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object Constants {
    const val INVENTORY_SIZE = 54
    const val INVENTORY_SLOTS_START = 9
    const val INVENTORY_SLOTS_END = 36
}

fun styledText(text: String, color: NamedTextColor): Component =
    Component.text(text, color).decoration(TextDecoration.ITALIC, false)

fun Material.displayName(): String =
    name.lowercase().replace('_', ' ')

fun isStackableCloudItem(item: ItemStack): Boolean =
    item.type.maxStackSize > 1 && item.enchantments.isEmpty()

fun compactNumber(n: Long): String = when {
    n < 1_000 -> n.toString()
    n < 1_000_000 -> "${n / 1_000}.${(n % 1_000) / 100}k"
    n < 1_000_000_000 -> "${n / 1_000_000}.${(n % 1_000_000) / 100_000}M"
    else -> "${n / 1_000_000_000}.${(n % 1_000_000_000) / 100_000_000}B"
}
