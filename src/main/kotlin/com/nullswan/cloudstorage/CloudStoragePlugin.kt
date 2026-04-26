package com.nullswan.cloudstorage

import com.nullswan.cloudstorage.block.CloudBlock
import com.nullswan.cloudstorage.command.CloudCommand
import com.nullswan.cloudstorage.listener.CloudBlockListener
import com.nullswan.cloudstorage.listener.GUIListener
import com.nullswan.cloudstorage.listener.PickupListener
import com.nullswan.cloudstorage.storage.PlayerStorage
import org.bukkit.plugin.java.JavaPlugin

class CloudStoragePlugin : JavaPlugin() {

    private lateinit var storage: PlayerStorage

    override fun onEnable() {
        storage = PlayerStorage(dataFolder)

        val cloudBlock = CloudBlock(this)
        server.addRecipe(cloudBlock.createRecipe())

        getCommand("cloud")?.setExecutor(CloudCommand(storage))

        server.pluginManager.registerEvents(GUIListener(storage), this)
        server.pluginManager.registerEvents(CloudBlockListener(cloudBlock, storage), this)
        server.pluginManager.registerEvents(PickupListener(this), this)

        logger.info("CloudStorage enabled — /cloud or craft a Cloud Block (chest + diamond)")
    }

    override fun onDisable() {
        if (::storage.isInitialized) storage.close()
        logger.info("CloudStorage disabled")
    }
}
