package com.nullswan.cloudstorage.listener

import com.nullswan.cloudstorage.block.CloudBlock
import com.nullswan.cloudstorage.gui.CloudGUI
import com.nullswan.cloudstorage.storage.PlayerStorage
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Barrel
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
    private val storage: PlayerStorage,
    private val debug: Boolean
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

        if (debug) plugin.logger.info("[Cloud] block placed by ${event.player.name} @${event.blockPlaced.location.toVector()}")
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

        if (debug) plugin.logger.info("[Cloud] block opened by ${event.player.name} @${block.location.toVector()}")
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

        if (debug) plugin.logger.info("[Cloud] block broken by ${event.player.name} @${block.location.toVector()} (creative=${event.player.gameMode == GameMode.CREATIVE})")
    }

    private fun spawnAmbientParticles() {
        for (player in plugin.server.onlinePlayers) {
            val cx = player.location.blockX shr 4
            val cz = player.location.blockZ shr 4
            val world = player.world
            for (dx in -1..1) {
                for (dz in -1..1) {
                    if (!world.isChunkLoaded(cx + dx, cz + dz)) continue
                    val chunk = world.getChunkAt(cx + dx, cz + dz)
                    for (state in chunk.tileEntities) {
                        if (state !is Barrel) continue
                        if (!state.persistentDataContainer.has(cloudBlock.pdcKey)) continue
                        val loc = state.block.location.add(0.5, 1.2, 0.5)
                        world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 4, 0.25, 0.15, 0.25, 0.005)
                        world.spawnParticle(Particle.ENCHANT, loc, 6, 0.3, 0.3, 0.3, 0.5)
                    }
                }
            }
        }
    }
}
