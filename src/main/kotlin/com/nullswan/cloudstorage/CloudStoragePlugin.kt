package com.nullswan.cloudstorage

import com.nullswan.cloudstorage.block.CloudBlock
import com.nullswan.cloudstorage.command.CloudCommand
import com.nullswan.cloudstorage.listener.CloudBlockListener
import com.nullswan.cloudstorage.listener.CloudOverflowListener
import com.nullswan.cloudstorage.listener.GUIListener
import com.nullswan.cloudstorage.storage.PlayerStorage
import org.bukkit.plugin.java.JavaPlugin

class CloudStoragePlugin : JavaPlugin() {

    private lateinit var storage: PlayerStorage

    override fun onEnable() {
        storage = PlayerStorage(dataFolder)

        val cloudBlock = CloudBlock(this)
        server.addRecipe(cloudBlock.createRecipe())

        getCommand("cloud")?.setExecutor(CloudCommand(storage))

        val pm = server.pluginManager
        pm.registerEvents(GUIListener(storage, this), this)
        pm.registerEvents(CloudBlockListener(this, cloudBlock, storage), this)

        if (pm.getPlugin("AutoPickup") != null) {
            pm.registerEvents(CloudOverflowListener(storage), this)
            logger.info("CloudStorage enabled — listening for AutoPickup overflow events")
        } else {
            logger.info("CloudStorage enabled — AutoPickup not found, overflow disabled")
        }
    }

    override fun onDisable() {
        if (::storage.isInitialized) storage.close()
    }
}
