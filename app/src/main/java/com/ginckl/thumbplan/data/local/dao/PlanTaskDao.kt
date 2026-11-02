package com.ginckl.thumbplan.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ginckl.thumbplan.data.local.entity.PlanTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanTaskDao {
    @Query("SELECT * FROM plan_tasks ORDER BY scheduledTime ASC")
    fun getAllTasks(): Flow<List<PlanTaskEntity>>

    @Query("SELECT * FROM plan_tasks WHERE status = :status ORDER BY scheduledTime ASC")
    fun getTasksByStatus(status: String): Flow<List<PlanTaskEntity>>

    @Query("SELECT * FROM plan_tasks WHERE status = 'pending'")
    suspend fun getPendingTasks(): List<PlanTaskEntity>

    @Query("SELECT * FROM plan_tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): PlanTaskEntity?

    @Query("SELECT COUNT(*) FROM plan_tasks WHERE status = 'pending' AND title = :title AND repeatMode = :repeatMode AND scheduledTime > :after")
    suspend fun countPendingSeries(title: String, repeatMode: String, after: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: PlanTaskEntity): Long

    @Update
    suspend fun updateTask(task: PlanTaskEntity)

    @Query("SELECT * FROM plan_tasks ORDER BY id")
    suspend fun getAll(): List<PlanTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<PlanTaskEntity>)

    @Query("DELETE FROM plan_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)
}
