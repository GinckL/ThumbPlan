package com.ginckl.thumbplan.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ginckl.thumbplan.data.local.dao.CoinDao
import com.ginckl.thumbplan.data.local.dao.DoneTaskDao
import com.ginckl.thumbplan.data.local.dao.PlanTaskDao
import com.ginckl.thumbplan.data.local.dao.ShopDao
import com.ginckl.thumbplan.data.local.entity.FrozenDayEntity
import com.ginckl.thumbplan.data.local.entity.InventoryEntity
import com.ginckl.thumbplan.data.local.dao.UserDao
import com.ginckl.thumbplan.data.local.entity.CoinEntity
import com.ginckl.thumbplan.data.local.entity.DoneTaskEntity
import com.ginckl.thumbplan.data.local.entity.PlanTaskEntity
import com.ginckl.thumbplan.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        PlanTaskEntity::class,
        DoneTaskEntity::class,
        CoinEntity::class,
        InventoryEntity::class,
        FrozenDayEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun planTaskDao(): PlanTaskDao
    abstract fun userDao(): UserDao
    abstract fun doneTaskDao(): DoneTaskDao
    abstract fun coinDao(): CoinDao
    abstract fun shopDao(): ShopDao

    companion object {
        /** v4: coin shop (purchases, streak freezes, labelled coin transactions). */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE coins ADD COLUMN label TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS inventory (itemId TEXT NOT NULL, quantity INTEGER NOT NULL, PRIMARY KEY(itemId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS frozen_days (epochDay INTEGER NOT NULL, PRIMARY KEY(epochDay))")
            }
        }

        /** v3: recurring tasks. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plan_tasks ADD COLUMN repeatMode TEXT NOT NULL DEFAULT 'none'")
                // Old builds mapped the "Weekend" chip to isWeekend; carry that over as a real recurrence.
                db.execSQL("UPDATE plan_tasks SET repeatMode = 'weekends' WHERE isWeekend = 1")
            }
        }
    }
}
