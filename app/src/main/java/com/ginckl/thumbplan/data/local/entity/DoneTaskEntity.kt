package com.ginckl.thumbplan.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "done_tasks",
    foreignKeys = [
        ForeignKey(
            entity = PlanTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")]
)
data class DoneTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val doneTime: Long,
    val comment: String? = null,
    val locationLabel: String? = null,
    val lat: Double? = null,
    val lng: Double? = null
)
