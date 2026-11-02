package com.ginckl.thumbplan.services.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ginckl.thumbplan.data.local.dao.PlanTaskDao
import com.ginckl.thumbplan.services.Notifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Runs ~30 min after an alarm fired: if the task is still pending, nudge the user once. */
@HiltWorker
class FollowUpWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val planTaskDao: PlanTaskDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong("TASK_ID", -1L)
        if (taskId == -1L) return Result.failure()
        val task = planTaskDao.getTaskById(taskId)
        if (task != null && task.status == "pending") {
            Notifier.showFollowUp(context, task)
        }
        return Result.success()
    }
}
