package com.ginckl.thumbplan.util

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/** Snapshot of everything the alarm needs in order to be reliable. */
data class PermissionHealth(
    val notifications: Boolean,
    val exactAlarms: Boolean,
    val fullScreen: Boolean,
    /** "Display over other apps": lets the alarm screen pop up while the phone is in use. */
    val overlay: Boolean
) {
    val allGood get() = notifications && exactAlarms && fullScreen && overlay
}

object Permissions {
    fun check(context: Context): PermissionHealth {
        val notifications = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        val fullScreen = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        return PermissionHealth(notifications, exact, fullScreen, Settings.canDrawOverlays(context))
    }

    fun notificationSettings(context: Context) =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun exactAlarmSettings(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        } else appDetails(context)

    fun fullScreenSettings(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
        } else appDetails(context)

    fun overlaySettings(context: Context) =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))

    fun appDetails(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
}
