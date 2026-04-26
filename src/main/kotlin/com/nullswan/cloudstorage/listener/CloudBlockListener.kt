package com.nullswan.cloudstorage.listener

import com.nullswan.cloudstorage.block.CloudBlock
import com.nullswan.cloudstorage.gui.CloudGUI
import com.nullswan.cloudstorage.storage.PlayerStorage
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent

class CloudBlockListener(
    private val cloudBlock: CloudBlock,
    private val storage: PlayerStorage
) : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlace(event: BlockPlaceEvent) {
        val item = event.itemInHand
        if (!cloudBlock.isCloudItem(item)) return
        cloudBlock.markBlock(event.blockPlaced)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        if (block.type != Material.BARREL) return
        if (!cloudBlock.isCloudBlock(block)) return

        event.isCancelled = true
        CloudGUI(event.player, storage).open()
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onBreak(event: BlockBreakEvent) {
        val block = event.block
        if (block.type != Material.BARREL) return
        if (!cloudBlock.isCloudBlock(block)) return

        event.isDropItems = false
        block.type = Material.AIR
        if (event.player.gameMode != GameMode.CREATIVE) {
            block.world.dropItemNaturally(block.location, cloudBlock.createItem())
        }
    }
}
