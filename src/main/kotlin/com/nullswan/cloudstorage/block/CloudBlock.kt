package com.nullswan.cloudstorage.block

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Barrel
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class CloudBlock(private val plugin: Plugin) {

    val pdcKey = NamespacedKey(plugin, "cloud_block")

    fun createItem(amount: Int = 1): ItemStack {
        return ItemStack(Material.BARREL, amount).apply {
            editMeta { meta ->
                meta.displayName(
                    Component.text("☁ Cloud Block", NamedTextColor.LIGHT_PURPLE)
                        .decoration(TextDecoration.ITALIC, false)
                        .decoration(TextDecoration.BOLD, true)
                )
                meta.lore(
                    listOf(
                        Component.text("Right-click to access cloud storage", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                        Component.text("Craft: Chest + Diamond", NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                )
                meta.persistentDataContainer.set(pdcKey, PersistentDataType.BYTE, 1.toByte())
            }
        }
    }

    fun createRecipe(): ShapelessRecipe {
        val key = NamespacedKey(plugin, "cloud_block_recipe")
        val recipe = ShapelessRecipe(key, createItem())
        recipe.addIngredient(Material.CHEST)
        recipe.addIngredient(Material.DIAMOND)
        return recipe
    }

    fun isCloudItem(item: ItemStack?): Boolean {
        if (item == null || item.type != Material.BARREL) return false
        val meta = item.itemMeta ?: return false
        return meta.persistentDataContainer.has(pdcKey, PersistentDataType.BYTE)
    }

    fun isCloudBlock(block: Block): Boolean {
        val state = block.state as? Barrel ?: return false
        return state.persistentDataContainer.has(pdcKey, PersistentDataType.BYTE)
    }

    fun markBlock(block: Block) {
        val state = block.state as? Barrel ?: return
        state.persistentDataContainer.set(pdcKey, PersistentDataType.BYTE, 1.toByte())
        state.update()
    }
}
