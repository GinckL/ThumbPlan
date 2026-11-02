package com.ginckl.thumbplan.services.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ginckl.thumbplan.di.RepositoryEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Alarms do not survive a reboot / app update / clock change — re-arm them all. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val repository = RepositoryEntryPoint.repository(context)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
