package com.ginckl.thumbplan.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Something the user bought: a one-off unlock (quantity 1) or a consumable stack (freezes, boosts). */
@Entity(tableName = "inventory")
data class InventoryEntity(
    @PrimaryKey val itemId: String,
    val quantity: Int
)
