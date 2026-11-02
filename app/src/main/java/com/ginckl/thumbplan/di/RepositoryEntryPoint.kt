package com.ginckl.thumbplan.di

import android.content.Context
import com.ginckl.thumbplan.data.prefs.SettingsStore
import com.ginckl.thumbplan.data.repo.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Lets receivers and services reach the singletons (Kotlin can't call the Hilt-generated super.onReceive). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface RepositoryEntryPoint {
    fun repository(): TaskRepository
    fun settings(): SettingsStore
    fun backup(): com.ginckl.thumbplan.data.backup.BackupManager
    fun syncStore(): com.ginckl.thumbplan.data.backup.SyncStore

    companion object {
        private fun get(context: Context) =
            EntryPointAccessors.fromApplication(context.applicationContext, RepositoryEntryPoint::class.java)

        fun repository(context: Context): TaskRepository = get(context).repository()
        fun settings(context: Context): SettingsStore = get(context).settings()
        fun backup(context: Context) = get(context).backup()
        fun syncStore(context: Context) = get(context).syncStore()
    }
}
