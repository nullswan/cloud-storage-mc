package com.nullswan.cloudstorage.storage

import org.bukkit.Material
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID

class PlayerStorage(dataFolder: File) {

    companion object {
        val SHARED_UUID: UUID = UUID(0L, 0L)
    }

    private val connection: Connection

    init {
        dataFolder.mkdirs()
        val dbFile = File(dataFolder, "storage.db")
        connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
        connection.createStatement().use { stmt ->
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS cloud_items (
                    player_uuid TEXT NOT NULL,
                    material    TEXT NOT NULL,
                    amount      INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (player_uuid, material)
                )
                """.trimIndent()
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS player_settings (
                    player_uuid  TEXT NOT NULL PRIMARY KEY,
                    auto_cloud   INTEGER NOT NULL DEFAULT 1
                )
                """.trimIndent()
            )
        }
    }

    fun deposit(playerUuid: UUID, material: Material, amount: Int) {
        require(amount > 0)
        connection.prepareStatement(
            """
            INSERT INTO cloud_items (player_uuid, material, amount)
            VALUES (?, ?, ?)
            ON CONFLICT(player_uuid, material) DO UPDATE SET amount = amount + excluded.amount
            """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, playerUuid.toString())
            stmt.setString(2, material.name)
            stmt.setLong(3, amount.toLong())
            stmt.executeUpdate()
        }
    }

    fun withdraw(playerUuid: UUID, material: Material, amount: Int): Int {
        require(amount > 0)
        val current = getAmount(playerUuid, material)
        if (current <= 0) return 0
        val actual = amount.toLong().coerceAtMost(current).toInt()

        if (current - actual <= 0) {
            connection.prepareStatement(
                "DELETE FROM cloud_items WHERE player_uuid = ? AND material = ?"
            ).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, material.name)
                stmt.executeUpdate()
            }
        } else {
            connection.prepareStatement(
                "UPDATE cloud_items SET amount = amount - ? WHERE player_uuid = ? AND material = ?"
            ).use { stmt ->
                stmt.setLong(1, actual.toLong())
                stmt.setString(2, playerUuid.toString())
                stmt.setString(3, material.name)
                stmt.executeUpdate()
            }
        }
        return actual
    }

    fun getAmount(playerUuid: UUID, material: Material): Long {
        connection.prepareStatement(
            "SELECT amount FROM cloud_items WHERE player_uuid = ? AND material = ?"
        ).use { stmt ->
            stmt.setString(1, playerUuid.toString())
            stmt.setString(2, material.name)
            val rs = stmt.executeQuery()
            return if (rs.next()) rs.getLong("amount") else 0L
        }
    }

    fun getAll(playerUuid: UUID): Map<Material, Long> {
        val items = mutableMapOf<Material, Long>()
        connection.prepareStatement(
            "SELECT material, amount FROM cloud_items WHERE player_uuid = ? AND amount > 0 ORDER BY material"
        ).use { stmt ->
            stmt.setString(1, playerUuid.toString())
            val rs = stmt.executeQuery()
            while (rs.next()) {
                val mat = Material.matchMaterial(rs.getString("material")) ?: continue
                items[mat] = rs.getLong("amount")
            }
        }
        return items
    }

    fun isAutoCloudEnabled(playerUuid: UUID): Boolean {
        connection.prepareStatement(
            "SELECT auto_cloud FROM player_settings WHERE player_uuid = ?"
        ).use { stmt ->
            stmt.setString(1, playerUuid.toString())
            val rs = stmt.executeQuery()
            return if (rs.next()) rs.getInt("auto_cloud") == 1 else true
        }
    }

    fun setAutoCloud(playerUuid: UUID, enabled: Boolean) {
        connection.prepareStatement(
            """
            INSERT INTO player_settings (player_uuid, auto_cloud) VALUES (?, ?)
            ON CONFLICT(player_uuid) DO UPDATE SET auto_cloud = excluded.auto_cloud
            """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, playerUuid.toString())
            stmt.setInt(2, if (enabled) 1 else 0)
            stmt.executeUpdate()
        }
    }

    fun close() {
        if (!connection.isClosed) connection.close()
    }
}
