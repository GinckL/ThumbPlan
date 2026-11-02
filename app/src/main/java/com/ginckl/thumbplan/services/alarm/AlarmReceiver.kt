package com.ginckl.thumbplan.services.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ginckl.thumbplan.di.RepositoryEntryPoint
import com.ginckl.thumbplan.services.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires when a reminder / alarm / snooze is due.
 *  - reminder → a normal heads-up notification ("Starts in 10 minutes")
 *  - alarm / snooze → no notification of its own: [AlarmService] plays the music and opens the full-screen alarm page.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(AlarmSchedulerImpl.EXTRA_TASK_ID, -1L)
        if (taskId < 0) return
        val kind = intent.getStringExtra(AlarmSchedulerImpl.EXTRA_KIND) ?: AlarmSchedulerImpl.KIND_ALARM
        val minutes = intent.getIntExtra(AlarmSchedulerImpl.EXTRA_MINUTES, 0)
        val appContext = context.applicationContext

        if (kind != AlarmSchedulerImpl.KIND_REMINDER) {
            // The service validates the task itself (and stops right away if it is gone or already done).
            AlarmService.start(appContext, taskId, kind)
            return
        }

        val repository = RepositoryEntryPoint.repository(context)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = repository.getTask(taskId) ?: return@launch
                if (task.status == "pending") Notifier.showReminder(appContext, task, minutes)
            } finally {
                pending.finish()
            }
        }
    }
}
