package com.example.towardsgoalsapp.etc

import android.Manifest
import android.app.Activity
import com.example.towardsgoalsapp.R
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat

class PermissionHelper {
    companion object {
        fun canScheduleExactAlarms(context: Context) : Boolean {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                am.canScheduleExactAlarms()
            } else true
        }

        fun canPostNotifications(context: Context) : Boolean {
            val canEmit = NotificationManagerCompat.from(context).areNotificationsEnabled()
            val canEmitOnTiramisu = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
            return canEmit && canEmitOnTiramisu
        }

    }
}