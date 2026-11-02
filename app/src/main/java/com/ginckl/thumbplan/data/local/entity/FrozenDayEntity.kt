package com.ginckl.thumbplan.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A missed day that a Streak Freeze covered (LocalDate.toEpochDay). */
@Entity(tableName = "frozen_days")
data class FrozenDayEntity(@PrimaryKey val epochDay: Long)
