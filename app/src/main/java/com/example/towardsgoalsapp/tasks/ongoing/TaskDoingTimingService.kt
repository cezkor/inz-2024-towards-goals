package com.example.towardsgoalsapp.tasks.ongoing

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.etc.PermissionHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

class TaskDoingTimingService : LifecycleService() {

    private var duration: Long = 0
    private var taskId: Long = Constants.IGNORE_ID_AS_LONG
    private var foregroundText: String = Constants.EMPTY_STRING
    private var bringUpText: String = Constants.EMPTY_STRING
    inner class OneSecondTicker(val duration: Long,
                                private val onTick: () -> Unit,
                                private val onEnd: () -> Unit
        ) {
        var secondsTicked: Long = 0
            private set

        private val stopped = AtomicBoolean(false)

        fun run(coroutineScope: CoroutineScope) {
            coroutineScope.launch {
                if (stopped.get()) return@launch

                stopped.set(false)
                var stop = false
                while (! stop) {
                    // let the coroutine sleep 1 second
                    try {
                        delay(MILIS_IN_SECOND)
                    }
                    catch (e: CancellationException) {
                        // unless the coroutine is cancelled
                        // assume it has ended
                        stopped.set(true)
                        onEnd.invoke()
                        break
                    }

                    secondsTicked += 1
                    if (secondsTicked >= duration) {
                        onEnd.invoke()
                        stopped.set(true)
                        break
                    }

                    onTick.invoke()
                    stop = stopped.get()
                }
            }
        }

        fun stop(coroutineScope: CoroutineScope) {
            coroutineScope.launch { stopped.set(true) }
        }

    }

    companion object {
        const val LOG_TAG = "TimingDTS"

        const val DURATION_IN_SECONDS = "TSDUR"
        const val TEXT_FOR_FOREGROUND_NOTIFICATION = "TSFNT"
        const val TEXT_FOR_NOTIFICATION = "TSNT"
        const val VALUE = "TSVAL"
        const val TASK_ID = "TSTID"
        const val SECONDS_IN_MINUTE = 60L
        const val MILIS_IN_SECOND = 1000L
        const val ID = 1001
        private const val BRING_UP_ID = 1000

        const val TICK_COUNT = "TS_OUT_TC"

        fun createIntent(context: Context, durationInMinutes: Long,
                         foregroundTxt: String, bringUpTxt: String,
                         taskId: Long) : Intent {
            return Intent(context, TaskDoingTimingService::class.java).apply {
                putExtra(DURATION_IN_SECONDS, durationInMinutes * SECONDS_IN_MINUTE)
                putExtra(TEXT_FOR_FOREGROUND_NOTIFICATION, foregroundTxt)
                putExtra(TEXT_FOR_NOTIFICATION, bringUpTxt)
                putExtra(TASK_ID, taskId)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private var ticker: OneSecondTicker? = null
    private lateinit var lbm: LocalBroadcastManager

    private var emitBringUpNotification = false
    private lateinit var bringUpNotificationBuilder : Notification.Builder
    private lateinit var activityHiddenReceiver: BroadcastReceiver
    private lateinit var icon: Icon

    override fun onCreate() {
        super.onCreate()
        lbm = LocalBroadcastManager.getInstance(this)
    }

    private fun onTick() {
        val seconds: Long = ticker?.secondsTicked ?: -1L
        lbm.sendBroadcast(
            Intent(TaskOngoing.TICK_RCV_INTENT_FILTER).putExtra(TICK_COUNT, seconds)
        )
    }

    private suspend fun playEndAlarm() {
        withContext(Dispatchers.IO) {
            var mediaPlayer: MediaPlayer? = null
            try {
                val ringtoneURI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer.create(this@TaskDoingTimingService, ringtoneURI)
                    .apply {
                        // ensures CPU will be running; which is crucial for a sound to be played
                        // when the device is locked (lock might turn CPU off)
                        setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                    }
                Log.i(LOG_TAG, "sound start")
                mediaPlayer.start()
                Log.i(LOG_TAG, "sound end")
            }
            finally {
                mediaPlayer?.release()
                Log.i(LOG_TAG, "player released")
            }
        }
    }

    @SuppressLint("MissingPermission") // is checked with PermissionHelper
    private fun onEnd() {

        Log.i(LOG_TAG, "time passed")

        if (emitBringUpNotification && PermissionHelper.canPostNotifications(this)) {
            NotificationManagerCompat
                .from(this)
                .notify(BRING_UP_ID, bringUpNotificationBuilder.build())
        }
        lbm.sendBroadcast(
            Intent(TaskOngoing.END_TIME_MEASUREMENT_INTENT_FILTER)
        )
        lifecycleScope.launch {
            playEndAlarm()
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent == null) return START_NOT_STICKY

        icon = Icon.createWithResource(this, R.drawable.full_oval_green)

        duration = intent.extras?.getLong(DURATION_IN_SECONDS, duration) ?: duration
        taskId = intent.extras?.getLong(TASK_ID, taskId) ?: taskId
        foregroundText = intent.extras?.getString(TEXT_FOR_FOREGROUND_NOTIFICATION, foregroundText)
            ?: foregroundText
        bringUpText = intent.extras?.getString(TEXT_FOR_NOTIFICATION, bringUpText) ?: bringUpText

        Log.i(LOG_TAG, "starting for task $taskId, duration $duration")

        val foregroundNotificationBuilder = Notification
            .Builder(this, Constants.TASK_POMIDORO_NOTIFICATION_CHANNEL)
            .setContentTitle(getString(R.string.pomidoro_name))
            .setContentText(foregroundText)
            .setSmallIcon(icon)
            .setOngoing(true)

        val bringUpIntent = Intent(this, TaskOngoing::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        bringUpNotificationBuilder = Notification
            .Builder(this, Constants.TASK_POMIDORO_NOTIFICATION_CHANNEL)
            .setContentTitle(getString(R.string.pomidoro_name))
            .setContentText(bringUpText)
            .setOngoing(false)
            .setAutoCancel(true)
            .setSmallIcon(icon)
            .setContentIntent(
                PendingIntent
                    .getActivity(this, 0, bringUpIntent,
                        PendingIntent.FLAG_IMMUTABLE)
            )

        activityHiddenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                if (intent.action != TaskOngoing.ACTIVITY_VISIBLE_INTENT_FILTER) return
                Log.i(LOG_TAG, "task ongoing is hidden: ${intent.extras?.getBoolean(VALUE)}")

                emitBringUpNotification =
                     intent.extras?.getBoolean(VALUE) ?: emitBringUpNotification
            }
        }

        lbm.registerReceiver(activityHiddenReceiver,
                IntentFilter(TaskOngoing.ACTIVITY_VISIBLE_INTENT_FILTER))

        ticker = OneSecondTicker(duration,
            {onTick()},
            {onEnd()}
        )

        ticker!!.run(lifecycleScope)

        NotificationManagerCompat
            .from(this@TaskDoingTimingService)
            .cancel(ID)
        startForeground(ID, foregroundNotificationBuilder.build())


        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        activityHiddenReceiver.run { lbm.unregisterReceiver(this) }
        ticker?.stop(lifecycleScope)
        super.onDestroy()
    }
}