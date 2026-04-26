package com.nullswan.cloudstorage.storage

import org.bukkit.Material
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerStorageTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var storage: PlayerStorage
    private val playerA = UUID.randomUUID()
    private val playerB = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        storage = PlayerStorage(tempDir)
    }

    @AfterEach
    fun tearDown() {
        storage.close()
    }

    @Test
    fun `deposit and retrieve single material`() {
        storage.deposit(playerA, Material.DIAMOND, 10)
        assertEquals(10L, storage.getAmount(playerA, Material.DIAMOND))
    }

    @Test
    fun `deposit accumulates amounts`() {
        storage.deposit(playerA, Material.IRON_INGOT, 5)
        storage.deposit(playerA, Material.IRON_INGOT, 15)
        assertEquals(20L, storage.getAmount(playerA, Material.IRON_INGOT))
    }

    @Test
    fun `withdraw reduces amount`() {
        storage.deposit(playerA, Material.GOLD_INGOT, 100)
        val withdrawn = storage.withdraw(playerA, Material.GOLD_INGOT, 30)
        assertEquals(30, withdrawn)
        assertEquals(70L, storage.getAmount(playerA, Material.GOLD_INGOT))
    }

    @Test
    fun `withdraw all removes row`() {
        storage.deposit(playerA, Material.COBBLESTONE, 64)
        val withdrawn = storage.withdraw(playerA, Material.COBBLESTONE, 64)
        assertEquals(64, withdrawn)
        assertEquals(0L, storage.getAmount(playerA, Material.COBBLESTONE))
        assertTrue(storage.getAll(playerA).isEmpty())
    }

    @Test
    fun `withdraw more than available returns only available`() {
        storage.deposit(playerA, Material.EMERALD, 10)
        val withdrawn = storage.withdraw(playerA, Material.EMERALD, 999)
        assertEquals(10, withdrawn)
        assertEquals(0L, storage.getAmount(playerA, Material.EMERALD))
    }

    @Test
    fun `withdraw from empty returns zero`() {
        val withdrawn = storage.withdraw(playerA, Material.DIAMOND, 5)
        assertEquals(0, withdrawn)
    }

    @Test
    fun `getAll returns all materials sorted`() {
        storage.deposit(playerA, Material.DIAMOND, 5)
        storage.deposit(playerA, Material.IRON_INGOT, 100)
        storage.deposit(playerA, Material.COBBLESTONE, 1000)

        val all = storage.getAll(playerA)
        assertEquals(3, all.size)
        assertEquals(1000L, all[Material.COBBLESTONE])
        assertEquals(5L, all[Material.DIAMOND])
        assertEquals(100L, all[Material.IRON_INGOT])
    }

    @Test
    fun `players have isolated storage`() {
        storage.deposit(playerA, Material.DIAMOND, 10)
        storage.deposit(playerB, Material.DIAMOND, 50)

        assertEquals(10L, storage.getAmount(playerA, Material.DIAMOND))
        assertEquals(50L, storage.getAmount(playerB, Material.DIAMOND))
    }

    @Test
    fun `shared storage uses sentinel UUID`() {
        storage.deposit(PlayerStorage.SHARED_UUID, Material.DIAMOND, 100)
        assertEquals(100L, storage.getAmount(PlayerStorage.SHARED_UUID, Material.DIAMOND))
        assertEquals(0L, storage.getAmount(playerA, Material.DIAMOND))
    }

    @Test
    fun `getAmount for unknown material returns zero`() {
        assertEquals(0L, storage.getAmount(playerA, Material.NETHERITE_INGOT))
    }

    @Test
    fun `large deposits use long range`() {
        storage.deposit(playerA, Material.DIRT, Int.MAX_VALUE)
        storage.deposit(playerA, Material.DIRT, Int.MAX_VALUE)
        val expected = Int.MAX_VALUE.toLong() * 2
        assertEquals(expected, storage.getAmount(playerA, Material.DIRT))
    }

    @Test
    fun `auto-cloud defaults to enabled`() {
        assertTrue(storage.isAutoCloudEnabled(playerA))
    }

    @Test
    fun `toggle auto-cloud off and on`() {
        storage.setAutoCloud(playerA, false)
        assertEquals(false, storage.isAutoCloudEnabled(playerA))
        storage.setAutoCloud(playerA, true)
        assertEquals(true, storage.isAutoCloudEnabled(playerA))
    }

    @Test
    fun `auto-cloud is per-player`() {
        storage.setAutoCloud(playerA, false)
        assertTrue(storage.isAutoCloudEnabled(playerB))
        assertEquals(false, storage.isAutoCloudEnabled(playerA))
    }
}
