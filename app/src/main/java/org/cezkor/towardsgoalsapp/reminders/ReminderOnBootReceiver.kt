package org.cezkor.towardsgoalsapp.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ReminderOnBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        if (intent.action != "android.intent.action.BOOT_COMPLETED") return
        context?.run {
            startForegroundService(Intent(context, ReminderService::class.java))
            Log.i("ROnBootR", "Reminder service rebooted")
        }
    }
}