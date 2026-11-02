package com.ginckl.thumbplan.services.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ginckl.thumbplan.data.backup.SyncManager
import com.ginckl.thumbplan.data.backup.SyncOutcome
import com.ginckl.thumbplan.data.backup.SyncStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/** Background sync. Never resolves a conflict on its own - it only reports it. */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sync: SyncManager,
    private val store: SyncStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!store.state.value.enabled || store.state.value.folderUri == null) return Result.success()
        return when (sync.sync()) {
            is SyncOutcome.Error -> Result.retry()
            else -> Result.success()
        }
    }

    companion object {
        private const val PERIODIC = "sync_periodic"
        private const val SOON = "sync_soon"

        fun schedulePeriodic(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC, ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<SyncWorker>(12, TimeUnit.HOURS).build()
            )
        }

        fun cancelPeriodic(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
        }

        /** Push local changes shortly after the app is left. */
        fun syncSoon(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                SOON, ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<SyncWorker>().setInitialDelay(3, TimeUnit.SECONDS).build()
            )
        }
    }
}
