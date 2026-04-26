package com.nullswan.cloudstorage.storage

import org.bukkit.Material
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID

class PlayerStorage(dataFolder: File) {

    private val connection: Connection

    init {
        dataFolder.mkdirs()
        val dbFile = File(dataFolder, "storage.db")
        connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
        connection.createStatement().execute(
            """
            CREATE TABLE IF NOT EXISTS cloud_items (
                player_uuid TEXT NOT NULL,
                material    TEXT NOT NULL,
                amount      INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (player_uuid, material)
            )
            """.trimIndent()
        )
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

    fun close() {
        if (!connection.isClosed) connection.close()
    }
}
