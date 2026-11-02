package com.ginckl.thumbplan.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ginckl.thumbplan.data.local.entity.CoinEntity
import kotlinx.coroutines.flow.Flow

/** A coin transaction joined with the task that earned it (title is null if the task was deleted or it was a purchase). */
data class CoinHistoryItem(
    val id: Long,
    val amount: Int,
    val doneTime: Long?,
    val title: String?,
    val comment: String?,
    val label: String?
)

@Dao
interface CoinDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoin(coin: CoinEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(coins: List<CoinEntity>)

    @Query("SELECT * FROM coins ORDER BY id")
    suspend fun getAll(): List<CoinEntity>

    @Query("SELECT * FROM coins ORDER BY id DESC")
    fun getAllCoins(): Flow<List<CoinEntity>>

    /** Everything ever earned (spending doesn't lower level or achievements). */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM coins WHERE amount > 0")
    fun observeLifetimeCoins(): Flow<Int>

    @Query(
        """SELECT c.id AS id, c.amount AS amount, d.doneTime AS doneTime, t.title AS title, d.comment AS comment, c.label AS label
           FROM coins c
           LEFT JOIN done_tasks d ON c.doneTaskId = d.id
           LEFT JOIN plan_tasks t ON d.taskId = t.id
           ORDER BY c.id DESC"""
    )
    fun observeHistory(): Flow<List<CoinHistoryItem>>
}
