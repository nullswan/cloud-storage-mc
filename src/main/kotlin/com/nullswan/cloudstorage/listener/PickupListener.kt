package com.nullswan.cloudstorage.listener

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PickupListener(plugin: Plugin) : Listener {

    private data class PickupBatch(
        val items: MutableMap<Material, Int> = mutableMapOf(),
        var lastUpdate: Long = System.currentTimeMillis()
    )

    private val batches = ConcurrentHashMap<UUID, PickupBatch>()

    init {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            val now = System.currentTimeMillis()
            val iterator = batches.entries.iterator()
            while (iterator.hasNext()) {
                val (uuid, batch) = iterator.next()
                if (now - batch.lastUpdate < 500) continue
                iterator.remove()
                val player = plugin.server.getPlayer(uuid) ?: continue
                val (material, amount) = batch.items.maxByOrNull { it.value } ?: continue
                val extra = batch.items.size - 1
                val suffix = if (extra > 0) " (+$extra more)" else ""
                player.sendActionBar(
                    Component.text("+ $amount ${formatMaterial(material)}$suffix", NamedTextColor.GREEN)
                )
            }
        }, 10L, 10L) // every 10 ticks = 500ms
    }

    @EventHandler
    fun onPickup(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        val item = event.item.itemStack
        if (item.type.isAir) return

        val batch = batches.computeIfAbsent(player.uniqueId) { PickupBatch() }
        batch.items.merge(item.type, item.amount, Int::plus)
        batch.lastUpdate = System.currentTimeMillis()
    }

    private fun formatMaterial(material: Material): String {
        return material.name.lowercase().replace('_', ' ')
    }
}
