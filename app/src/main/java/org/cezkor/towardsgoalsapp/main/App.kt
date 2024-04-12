package org.cezkor.towardsgoalsapp.main

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import app.cash.sqldelight.db.SqlDriver
import org.cezkor.towardsgoalsapp.database.DatabaseGeneration


class App: Application() {

    override fun onCreate() {
        super.onCreate()

        // add task pomodoro notification channel in app context
        val notificationManager =
            getSystemService(NotificationManager::class.java)
        if (notificationManager == null) {
            Log.e("APPCREATE", "no notification manager")
            return
        }
        val taskPomodoroNotificationChannel = NotificationChannel(
            org.cezkor.towardsgoalsapp.Constants.TASK_POMODORO_NOTIFICATION_CHANNEL,
            "TowardsGoals: Pomodoro",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(taskPomodoroNotificationChannel)
    }

    val driver: SqlDriver = DatabaseGeneration.getDriver(this)


}