package com.ginckl.thumbplan.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A coin transaction: positive = earned, negative = spent in the shop (then [label] names the purchase). */
@Entity(
    tableName = "coins",
    foreignKeys = [
        ForeignKey(
            entity = DoneTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["doneTaskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("doneTaskId")]
)
data class CoinEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Int = 0,
    val doneTaskId: Long? = null,
    val inviteUserId: Long? = null,
    val daily: Boolean = false,
    val label: String? = null
)
