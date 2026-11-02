package com.ginckl.thumbplan.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ginckl.thumbplan.data.local.entity.FrozenDayEntity
import com.ginckl.thumbplan.data.local.entity.InventoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {
    @Query("SELECT * FROM inventory")
    fun observeInventory(): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory")
    suspend fun getInventory(): List<InventoryEntity>

    @Query("SELECT quantity FROM inventory WHERE itemId = :id")
    suspend fun quantity(id: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: InventoryEntity)

    @Query("SELECT epochDay FROM frozen_days")
    suspend fun getFrozenDays(): List<Long>

    @Query("SELECT epochDay FROM frozen_days")
    fun observeFrozenDays(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFrozen(day: FrozenDayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllInventory(items: List<InventoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFrozen(days: List<FrozenDayEntity>)
}
