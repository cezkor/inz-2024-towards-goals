package com.example.towardsgoalsapp.main

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import app.cash.sqldelight.db.SqlDriver
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.database.DatabaseGeneration
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking


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
            Constants.TASK_POMODORO_NOTIFICATION_CHANNEL,
            "TowardsGoals: Pomodoro",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(taskPomodoroNotificationChannel)
    }

    val driver: SqlDriver = DatabaseGeneration.getDriver(this)


}