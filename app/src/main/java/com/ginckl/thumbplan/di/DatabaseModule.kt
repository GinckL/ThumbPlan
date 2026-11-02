package com.ginckl.thumbplan.di

import android.content.Context
import androidx.room.Room
import com.ginckl.thumbplan.data.local.AppDatabase
import com.ginckl.thumbplan.data.local.dao.CoinDao
import com.ginckl.thumbplan.data.local.dao.DoneTaskDao
import com.ginckl.thumbplan.data.local.dao.PlanTaskDao
import com.ginckl.thumbplan.data.local.dao.ShopDao
import com.ginckl.thumbplan.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "thumbplan_db")
            .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .build()

    @Provides fun providePlanTaskDao(database: AppDatabase): PlanTaskDao = database.planTaskDao()
    @Provides fun provideUserDao(database: AppDatabase): UserDao = database.userDao()
    @Provides fun provideDoneTaskDao(database: AppDatabase): DoneTaskDao = database.doneTaskDao()
    @Provides fun provideCoinDao(database: AppDatabase): CoinDao = database.coinDao()
    @Provides fun provideShopDao(database: AppDatabase): ShopDao = database.shopDao()
}
