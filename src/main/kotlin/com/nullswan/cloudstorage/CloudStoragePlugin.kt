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

        val pm = server.pluginManager
        pm.registerEvents(GUIListener(storage), this)
        pm.registerEvents(CloudBlockListener(this, cloudBlock, storage), this)
        pm.registerEvents(PickupListener(storage), this)

        logger.info("CloudStorage enabled — /cloud or craft a Cloud Block (chest + diamond)")
    }

    override fun onDisable() {
        if (::storage.isInitialized) storage.close()
    }
}
