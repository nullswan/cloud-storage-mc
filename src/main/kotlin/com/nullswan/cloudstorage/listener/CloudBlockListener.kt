package com.nullswan.cloudstorage.listener

import com.nullswan.cloudstorage.block.CloudBlock
import com.nullswan.cloudstorage.gui.CloudGUI
import com.nullswan.cloudstorage.storage.PlayerStorage
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.Plugin

class CloudBlockListener(
    private val plugin: Plugin,
    private val cloudBlock: CloudBlock,
    private val storage: PlayerStorage
) : Listener {

    init {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { spawnAmbientParticles() }, 20L, 20L)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlace(event: BlockPlaceEvent) {
        val item = event.itemInHand
        if (!cloudBlock.isCloudItem(item)) return
        cloudBlock.markBlock(event.blockPlaced)

        val loc = event.blockPlaced.location.add(0.5, 1.0, 0.5)
        loc.world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.5f)
        loc.world.spawnParticle(Particle.END_ROD, loc, 20, 0.3, 0.3, 0.3, 0.02)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        if (block.type != Material.BARREL) return
        if (!cloudBlock.isCloudBlock(block)) return

        event.isCancelled = true

        val loc = block.location.add(0.5, 0.5, 0.5)
        loc.world.playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, 0.6f, 1.2f)
        loc.world.spawnParticle(Particle.PORTAL, loc, 15, 0.3, 0.3, 0.3, 0.5)

        CloudGUI(event.player, storage).open()
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onBreak(event: BlockBreakEvent) {
        val block = event.block
        if (block.type != Material.BARREL) return
        if (!cloudBlock.isCloudBlock(block)) return

        event.isDropItems = false
        val loc = block.location.add(0.5, 0.5, 0.5)
        loc.world.playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 1.5f)
        loc.world.spawnParticle(Particle.SMOKE, loc, 15, 0.3, 0.3, 0.3, 0.02)

        block.type = Material.AIR
        if (event.player.gameMode != GameMode.CREATIVE) {
            block.world.dropItemNaturally(block.location, cloudBlock.createItem())
        }
    }

    private fun spawnAmbientParticles() {
        for (player in plugin.server.onlinePlayers) {
            val nearby = player.location.getNearbyEntities(16.0, 16.0, 16.0)
            for (chunk in player.location.chunk.let { listOf(it) }) {
                for (state in chunk.tileEntities) {
                    if (state.block.type != Material.BARREL) continue
                    if (!cloudBlock.isCloudBlock(state.block)) continue
                    val loc = state.block.location.add(0.5, 1.1, 0.5)
                    loc.world.spawnParticle(Particle.END_ROD, loc, 3, 0.2, 0.1, 0.2, 0.01)
                }
            }
        }
    }
}
