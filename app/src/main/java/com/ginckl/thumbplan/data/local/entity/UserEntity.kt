package com.ginckl.thumbplan.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "users",
    indices = [Index(value = ["phone"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val username: String? = null,
    val dateOfBirth: Long? = null,
    val googleId: Long? = null,
    val appleId: Long? = null,
    val avatarPath: String? = null,
    val phone: String? = null,
    val lastSeen: Long? = null,
    val referralCode: String = UUID.randomUUID().toString().take(8),
    val streakCount: Int = 0,
    val totalCoins: Int = 0
)
