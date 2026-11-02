package com.ginckl.thumbplan.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.ColumnInfo

@Entity(
    tableName = "plan_tasks",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId"),
        Index("status")
    ]
)
data class PlanTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val colorHex: String? = null,
    val imagePath: String? = null,
    val audioPath: String? = null,
    val scheduledTime: Long,
    val rewardCoins: Int = 50,
    val isWeekend: Boolean = false,
    val reminderIntervals: String = "", // Comma separated e.g. "15,10,5,1"
    val status: String = "pending", // pending | completed
    @ColumnInfo(defaultValue = "none")
    val repeatMode: String = "none" // none | daily | weekdays | weekends | weekly
)
