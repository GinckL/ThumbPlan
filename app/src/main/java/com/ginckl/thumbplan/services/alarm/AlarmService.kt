package com.ginckl.thumbplan.services.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ginckl.thumbplan.di.RepositoryEntryPoint
import com.ginckl.thumbplan.services.Notifier
import com.ginckl.thumbplan.services.ToneSynth
import com.ginckl.thumbplan.services.work.FollowUpWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Rings the alarm the moment a task is due:
 *  1. plays the user's own music (task audio → equipped shop tone → phone alarm tone) + vibration,
 *  2. opens the full-screen alarm page straight away (needs "display over other apps" while the phone is in use;
 *     when the phone is locked/idle the notification's full-screen intent opens it),
 *  3. keeps a silent ongoing notification as a fallback entry point.
 * The system notification itself never plays a sound.
 */
class AlarmService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private var player: AlarmPlayer? = null
    private var job: Job? = null
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId = intent?.getLongExtra(EXTRA_TASK_ID, -1L) ?: -1L
        if (taskId < 0) {
            stopSelf()
            return START_NOT_STICKY
        }
        val kind = intent?.getStringExtra(AlarmSchedulerImpl.EXTRA_KIND) ?: AlarmSchedulerImpl.KIND_ALARM

        // Must go foreground within seconds: show a placeholder now, details once the task is loaded.
        val id = Notifier.notificationId(taskId)
        val placeholder = Notifier.alarmNotification(this, null, taskId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(id, placeholder, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else startForeground(id, placeholder)

        job?.cancel()
        job = scope.launch { ring(taskId, kind, id) }
        return START_NOT_STICKY
    }

    private suspend fun ring(taskId: Long, kind: String, notificationId: Int) {
        val repo = RepositoryEntryPoint.repository(this)
        val settings = RepositoryEntryPoint.settings(this)
        val task = repo.getTask(taskId)
        if (task == null || task.status != "pending") {
            stopSelf()
            return
        }
        // Keep recurring series alive even if the user never completes this one.
        if (kind == AlarmSchedulerImpl.KIND_ALARM) repo.ensureNextOccurrence(task)

        getSystemService(android.app.NotificationManager::class.java)
            .notify(notificationId, Notifier.alarmNotification(this, task, taskId))

        val own = task.audioPath?.let { File(it) }?.takeIf { it.exists() }
            ?: ToneSynth.fileFor(this, settings.tone.value)
        player = AlarmPlayer(this).also { it.start(own?.absolutePath, settings.sound.value, settings.vibrate.value) }

        openAlarmScreen(taskId)

        WorkManager.getInstance(this).enqueueUniqueWork(
            "follow_up_$taskId",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<FollowUpWorker>()
                .setInitialDelay(30, TimeUnit.MINUTES)
                .setInputData(workDataOf("TASK_ID" to taskId))
                .build()
        )

        // A forgotten phone shouldn't ring forever: stop after 2 minutes but leave the notification to tap.
        handler.postDelayed({
            player?.stop()
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }, 120_000)
    }

    /**
     * Background activity starts are blocked on modern Android unless the app shows a window. With the
     * "display over other apps" permission we put a 1px invisible overlay up for a moment, which makes the launch legal.
     */
    private fun openAlarmScreen(taskId: Long) {
        val canOverlay = Settings.canDrawOverlays(this)
        if (canOverlay) {
            runCatching {
                val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val view = View(this)
                val params = WindowManager.LayoutParams(
                    1, 1,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT
                )
                wm.addView(view, params)
                overlayView = view
            }
        }
        runCatching { startActivity(Notifier.alarmIntent(this, taskId)) }
        handler.postDelayed({ removeOverlay() }, 4_000)
    }

    private fun removeOverlay() {
        overlayView?.let { v ->
            runCatching { (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(v) }
        }
        overlayView = null
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        removeOverlay()
        player?.stop()
        player = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_TASK_ID = "TASK_ID"

        fun start(context: Context, taskId: Long, kind: String) {
            val intent = Intent(context, AlarmService::class.java)
                .putExtra(EXTRA_TASK_ID, taskId)
                .putExtra(AlarmSchedulerImpl.EXTRA_KIND, kind)
            context.startForegroundService(intent)
        }

        /** Silences the alarm and removes its notification. */
        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmService::class.java))
        }
    }
}
