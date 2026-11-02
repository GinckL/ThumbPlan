package com.ginckl.thumbplan.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ginckl.thumbplan.MainActivity
import com.ginckl.thumbplan.R
import com.ginckl.thumbplan.data.local.entity.PlanTaskEntity
import com.ginckl.thumbplan.ui.features.alarm.AlarmActivity

object Notifier {
    /** Silent on purpose: the alarm *music* is played by [com.ginckl.thumbplan.services.alarm.AlarmService], never by the system. */
    const val CHANNEL_ALARM = "alarm_silent_v3"
    const val CHANNEL_REMINDER = "reminder_channel"
    const val CHANNEL_FOLLOW_UP = "follow_up_channel"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        // Older builds used a channel with the phone's alarm ringtone, which played instead of the user's music.
        nm.deleteNotificationChannel("alarm_channel")
        nm.deleteNotificationChannel("alarm_channel_v2")
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALARM, "Task alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Shown while a task alarm is ringing (the music is played by the app)"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDER, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Heads-up before a task starts (30 / 15 / 10 / 5 / 1 min)"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_FOLLOW_UP, "Follow-ups", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Did you finish it? Gentle nudges for unfinished tasks"
            }
        )
    }

    fun openTaskIntent(context: Context, taskId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_TASK_ID, taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, (taskId % 100_000L).toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun alarmIntent(context: Context, taskId: Long, preview: Boolean = false): Intent =
        Intent(context, AlarmActivity::class.java).apply {
            putExtra(AlarmActivity.EXTRA_TASK_ID, taskId)
            putExtra(AlarmActivity.EXTRA_PREVIEW, preview)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

    fun notificationId(taskId: Long, offset: Int = 0) = (taskId % 100_000L).toInt() * 3 + offset

    /** The ongoing "alarm is ringing" notification. Its full-screen intent opens the alarm page when the phone is locked. */
    fun alarmNotification(context: Context, task: PlanTaskEntity?, taskId: Long): Notification {
        ensureChannels(context)
        val pi = PendingIntent.getActivity(
            context, notificationId(taskId), alarmIntent(context, taskId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_stat_task)
            .setContentTitle(task?.title ?: "Alarm")
            .setContentText(if (task != null) "Time's up! Tap to swipe it done (+${task.rewardCoins} coins)." else "Task alarm")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun showReminder(context: Context, task: PlanTaskEntity, minutes: Int) {
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_stat_task)
            .setContentTitle(task.title)
            .setContentText(if (minutes == 1) "Starts in 1 minute" else "Starts in $minutes minutes")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openTaskIntent(context, task.id))
            .setAutoCancel(true)
            .build()
        notify(context, notificationId(task.id, 1), notification)
    }

    fun showFollowUp(context: Context, task: PlanTaskEntity) {
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_FOLLOW_UP)
            .setSmallIcon(R.drawable.ic_stat_task)
            .setContentTitle("Did you finish “${task.title}”?")
            .setContentText("Open ThumbPlan to mark it done and earn ${task.rewardCoins} coins.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openTaskIntent(context, task.id))
            .setAutoCancel(true)
            .build()
        notify(context, notificationId(task.id, 2), notification)
    }

    fun cancelAlarm(context: Context, taskId: Long) {
        context.getSystemService(NotificationManager::class.java).cancel(notificationId(taskId))
    }

    fun cancelReminder(context: Context, taskId: Long) {
        context.getSystemService(NotificationManager::class.java).cancel(notificationId(taskId, 1))
    }

    private fun notify(context: Context, id: Int, notification: Notification) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (!nm.areNotificationsEnabled()) return
        nm.notify(id, notification)
    }
}
