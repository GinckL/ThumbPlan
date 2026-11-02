package com.ginckl.thumbplan.services.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ginckl.thumbplan.data.local.entity.PlanTaskEntity
import com.ginckl.thumbplan.domain.parseReminderMinutes

interface AlarmScheduler {
    /** Arms the main alarm and the reminders of [task] (past moments are skipped). Replaces earlier ones. */
    fun schedule(task: PlanTaskEntity)

    /** Re-rings [task] at [atMillis] without touching its stored schedule. */
    fun snooze(task: PlanTaskEntity, atMillis: Long)

    /** Cancels every alarm, reminder and snooze of [task]. */
    fun cancel(task: PlanTaskEntity)
}

class AlarmSchedulerImpl(private val context: Context) : AlarmScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(task: PlanTaskEntity) {
        cancel(task)
        val now = System.currentTimeMillis()
        if (task.scheduledTime > now) arm(task, KIND_ALARM, 0, task.scheduledTime, 0)
        parseReminderMinutes(task.reminderIntervals).take(MAX_REMINDERS).forEachIndexed { index, minutes ->
            val at = task.scheduledTime - minutes * 60_000L
            if (at > now) arm(task, KIND_REMINDER, index + 1, at, minutes)
        }
    }

    override fun snooze(task: PlanTaskEntity, atMillis: Long) {
        arm(task, KIND_SNOOZE, SNOOZE_SLOT, atMillis, 0)
    }

    override fun cancel(task: PlanTaskEntity) {
        for (slot in 0..SNOOZE_SLOT) {
            val pi = pendingIntent(task.id, slot, KIND_ALARM, 0, PendingIntent.FLAG_NO_CREATE) ?: continue
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    private fun arm(task: PlanTaskEntity, kind: String, slot: Int, at: Long, minutes: Int) {
        val pi = pendingIntent(task.id, slot, kind, minutes, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (exactAllowed) alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call: degrade to an inexact alarm.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun pendingIntent(taskId: Long, slot: Int, kind: String, minutes: Int, flag: Int): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_KIND, kind)
            putExtra(EXTRA_MINUTES, minutes)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(taskId, slot),
            intent,
            flag or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val EXTRA_TASK_ID = "TASK_ID"
        const val EXTRA_KIND = "KIND"
        const val EXTRA_MINUTES = "MINUTES"
        const val KIND_ALARM = "alarm"
        const val KIND_REMINDER = "reminder"
        const val KIND_SNOOZE = "snooze"
        const val MAX_REMINDERS = 8
        private const val SNOOZE_SLOT = 9

        /** 10 request codes per task: 0 = main, 1..8 = reminders, 9 = snooze. */
        fun requestCode(taskId: Long, slot: Int): Int = ((taskId % 100_000L).toInt() * 10) + slot
    }
}
