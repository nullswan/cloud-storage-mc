package com.nullswan.cloudstorage

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material

object Constants {
    const val INVENTORY_SIZE = 54
    const val INVENTORY_SLOTS_START = 9
    const val INVENTORY_SLOTS_END = 36
}

fun styledText(text: String, color: NamedTextColor): Component =
    Component.text(text, color).decoration(TextDecoration.ITALIC, false)

fun Material.displayName(): String =
    name.lowercase().replace('_', ' ')
