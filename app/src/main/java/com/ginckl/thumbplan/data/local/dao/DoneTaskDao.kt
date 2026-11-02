package com.ginckl.thumbplan.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ginckl.thumbplan.data.local.entity.DoneTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DoneTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoneTask(doneTask: DoneTaskEntity): Long

    @Query("SELECT * FROM done_tasks ORDER BY doneTime DESC")
    fun getAllDoneTasks(): Flow<List<DoneTaskEntity>>

    @Query("SELECT * FROM done_tasks ORDER BY id")
    suspend fun getAll(): List<DoneTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DoneTaskEntity>)

    @Query("SELECT doneTime FROM done_tasks")
    fun observeDoneTimes(): Flow<List<Long>>

    @Query("SELECT doneTime FROM done_tasks")
    suspend fun getDoneTimes(): List<Long>

    @Query("SELECT * FROM done_tasks WHERE taskId = :taskId ORDER BY doneTime DESC LIMIT 1")
    suspend fun getForTask(taskId: Long): DoneTaskEntity?

    @Query("UPDATE done_tasks SET comment = :comment WHERE id = :id")
    suspend fun updateComment(id: Long, comment: String?)
}
