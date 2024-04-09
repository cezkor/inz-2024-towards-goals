package com.example.towardsgoalsapp.tasks.ongoing

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class TaskDoingTimingService : LifecycleService() {

    private var taskId: Long = Constants.IGNORE_ID_AS_LONG
    private var foregroundTxt: String = Constants.EMPTY_STRING
    private var additionalTime: Long = 0L

    inner class OneSecondTicker(private val onTick: () -> Unit,
                                private val onEnd: () -> Unit
        ) {

        private val secondsMutex = Mutex()
        private var secondsTicked: Long = 0
            private set
        suspend fun getSecondsTicked() = secondsMutex.withLock { secondsTicked }

        private val stopped = AtomicBoolean(false)
        private var running = AtomicBoolean(false)

        fun isRunning() : Boolean = running.get()

        private var duration: Long = 0L
        private var durationIsSet: Boolean = false
        private var tickAtSettingDuration : Long = 0L
        private val durationMutex = Mutex()

        suspend fun setDuration(duration: Long) = durationMutex.withLock {
            val tick = getSecondsTicked()
            this.duration = duration
            tickAtSettingDuration = tick
            durationIsSet = true
        }

        fun run(coroutineScope: CoroutineScope) {
            coroutineScope.launch {
                if (stopped.get() || running.get()) return@launch
                running.set(true)

                stopped.set(false)
                var stop = false
                while (! stop) {
                    // let the coroutine sleep 1 second
                    try {
                        delay(MILIS_IN_SECOND)
                    }
                    catch (e: CancellationException) {
                        // unless the coroutine is cancelled
                        // assume it was stopped
                        stopped.set(true)
                        break
                    }

                    secondsMutex.withLock { secondsTicked += 1 }

                    durationMutex.withLock {
                        val secondsTickedSinceDurationWasSet = secondsTicked - tickAtSettingDuration
                        if (secondsTickedSinceDurationWasSet >= duration
                            && durationIsSet) {
                            onEnd.invoke()
                            durationIsSet = false
                        }
                        if (durationIsSet)
                            onTick.invoke()
                    }

                    stop = stopped.get()
                }
                running.set(false)
            }
        }

        fun stop(coroutineScope: CoroutineScope) {
            coroutineScope.launch { stopped.set(true) }
        }

    }

    companion object {
        const val LOG_TAG = "TimingDTS"

        const val DURATION_IN_MINUTES = "TSDUR"
        const val ADDITIONAL_TIME = "TSADD"
        const val TEXT_FOR_BRING_UP_NOTIFICATION = "TSFBGNT"
        const val TEXT_FOR_NOTIFICATION = "TSNT"
        const val VALUE = "TSVAL"
        const val TASK_ID = "TSTID"
        const val FINAL_TICK = "TSFT"
        const val SECONDS_IN_MINUTE = 60L
        const val MILIS_IN_SECOND = 1000L
        const val ID = 1001
        private const val BRING_UP_ID = 1000
        const val PLAY_ALARM_BY_MILIS = 5000L
        const val DEFAULT_VOLUME = 0.5f

        const val TICK_COUNT = "TS_OUT_TC"

        fun createIntent(context: Context,
                         bringUpTxt: String,
                         taskId: Long, additionalTime: Long) : Intent {
            return Intent(context, TaskDoingTimingService::class.java).apply {
                putExtra(TEXT_FOR_NOTIFICATION, bringUpTxt)
                putExtra(TASK_ID, taskId)
                putExtra(ADDITIONAL_TIME, additionalTime * SECONDS_IN_MINUTE)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private val notificationMutex = Mutex()
    private lateinit var ticker: OneSecondTicker
    private var notification: Notification? = null

    private lateinit var lbm: LocalBroadcastManager

    private var emitBringUpNotification = false
    private lateinit var bringUpNotificationBuilder : Notification.Builder
    private lateinit var activityHiddenReceiver: BroadcastReceiver
    private lateinit var startTickingReceiver: BroadcastReceiver
    private lateinit var icon: Icon
    private var mPlayer : MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
        lbm = LocalBroadcastManager.getInstance(this)
    }

    private fun onTick() {
        lifecycleScope.launch {
            val seconds = ticker.getSecondsTicked()
            lbm.sendBroadcast(
                Intent(TaskOngoing.TICK_RCV_INTENT_FILTER).putExtra(TICK_COUNT, seconds)
            )
        }
    }

    private suspend fun setupMediaPlayer() : MediaPlayer? = withContext(Dispatchers.IO) {
        val ringtoneURI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val mp = MediaPlayer()
        try {
            // audio attributes for an alarm (audio stream of alarm, usage of alarm)
            mp.setAudioAttributes(AudioAttributes.Builder()
                .setLegacyStreamType(AudioManager.STREAM_ALARM)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build())
            // ensures CPU will be running; which is crucial for a sound to be played
            // when the device is locked (lock might turn CPU off)
            mp.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            mp.setDataSource(this@TaskDoingTimingService, ringtoneURI)
        }
        catch (e: Exception) {
            Log.e(LOG_TAG, "media player setting exception", e)
            mp.release()
            return@withContext null
        }

        return@withContext mp
    }

    private fun releaseMediaPlayer() {
        val mediaPlayer = mPlayer
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying)
                    mediaPlayer.stop()
            }
        }
        catch (e : IllegalStateException) {
            Log.e(LOG_TAG, "media player state error", e)
        }
        finally {
            mediaPlayer?.release()
        }
    }

    private fun playEndAlarm() {

        val mediaPlayer = mPlayer
        if (mediaPlayer == null) {
            Log.w(LOG_TAG, "no media player; not playing")
            return
        }

        lifecycleScope.launch {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.prepare()
            Log.i(LOG_TAG, "sound start")
            mediaPlayer.start()
            delay(PLAY_ALARM_BY_MILIS)
            mediaPlayer.stop()
            Log.i(LOG_TAG, "sound end")
        }

    }

    @SuppressLint("MissingPermission") // is checked with PermissionHelper
    private fun onEnd() {

        Log.i(LOG_TAG, "time passed")
        if (emitBringUpNotification && PermissionHelper.canPostNotifications(this)) {
            lifecycleScope.launch {
                notificationMutex.withLock {
                    val n = notification ?: return@withLock
                    NotificationManagerCompat.from(this@TaskDoingTimingService)
                        .cancel(BRING_UP_ID)
                    NotificationManagerCompat.from(this@TaskDoingTimingService)
                        .notify(BRING_UP_ID, n)
                }
            }
        }
        lifecycleScope.launch {
            val currentTick = ticker.getSecondsTicked()
            lbm.sendBroadcast(
                Intent(TaskOngoing.END_TIME_MEASUREMENT_INTENT_FILTER)
                    .apply {
                        putExtra(FINAL_TICK, currentTick)
                    }
            )
            ticker.setDuration(additionalTime)
            playEndAlarm()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent == null) return START_NOT_STICKY

        icon = Icon.createWithResource(this, R.drawable.full_oval_green)

        taskId = intent.extras?.getLong(TASK_ID, taskId) ?: taskId
        foregroundTxt = intent.extras?.getString(TEXT_FOR_NOTIFICATION, foregroundTxt) ?: foregroundTxt
        additionalTime = intent.extras?.getLong(ADDITIONAL_TIME, additionalTime) ?: additionalTime

        Log.i(LOG_TAG, "starting for task $taskId, additional time $additionalTime")

        val foregroundNotificationBuilder = Notification
            .Builder(this, Constants.TASK_POMODORO_NOTIFICATION_CHANNEL)
            .setContentTitle(getString(R.string.pomodoro_name))
            .setSmallIcon(icon)
            .setContentText(foregroundTxt)
            .setOngoing(true)

        val bringUpIntent = Intent(this, TaskOngoing::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        bringUpNotificationBuilder = Notification
            .Builder(this, Constants.TASK_POMODORO_NOTIFICATION_CHANNEL)
            .setContentTitle(getString(R.string.pomodoro_name))
            .setOngoing(false)
            .setAutoCancel(true)
            .setSmallIcon(icon)
            .setContentIntent(
                PendingIntent
                    .getActivity(this, 0, bringUpIntent,
                        PendingIntent.FLAG_IMMUTABLE)
            )

        ticker = OneSecondTicker(
            {onTick()},
            {onEnd()}
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

        startTickingReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                if (intent.action != TaskOngoing.START_TICKER_INTENT_FILTER) return
                val duration = intent.extras?.getLong(DURATION_IN_MINUTES) ?: return
                val bringUpTxt = intent.extras?.getString(TEXT_FOR_BRING_UP_NOTIFICATION) ?: return
                Log.i(LOG_TAG, "setting ticker duration until " +
                        "onEnd as ${duration * SECONDS_IN_MINUTE}")
                lifecycleScope.launch {
                    notificationMutex.withLock {
                        bringUpNotificationBuilder.setContentText(bringUpTxt)
                        notification = bringUpNotificationBuilder.build()
                    }
                    if (! ticker.isRunning()) {
                        ticker.setDuration(duration * SECONDS_IN_MINUTE)
                        ticker.run(lifecycleScope)
                    }
                    else
                        ticker.setDuration(duration * SECONDS_IN_MINUTE)
                }
            }

        }

        lifecycleScope.launch {
            mPlayer = setupMediaPlayer()
            if (mPlayer != null)
                mPlayer?.setVolume(DEFAULT_VOLUME, DEFAULT_VOLUME)
            if (mPlayer == null)
                Log.e(LOG_TAG,"unable to setup media player")
        }

        lbm.registerReceiver(activityHiddenReceiver,
                IntentFilter(TaskOngoing.ACTIVITY_VISIBLE_INTENT_FILTER))

        lbm.registerReceiver(startTickingReceiver,
            IntentFilter(TaskOngoing.START_TICKER_INTENT_FILTER))

        NotificationManagerCompat
            .from(this@TaskDoingTimingService)
            .cancel(ID)
        startForeground(ID, foregroundNotificationBuilder.build())

        lbm.sendBroadcast(
            Intent(TaskOngoing.SERVICE_READY_INTENT_FILTER)
        )
        Log.i(LOG_TAG, "service is ready to tick")

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        Log.i(LOG_TAG, "being destroyed;" +
                " task $taskId")
        runBlocking {
            ticker.stop(this)
        }
        releaseMediaPlayer()
        activityHiddenReceiver.run { lbm.unregisterReceiver(this) }
        startTickingReceiver.run { lbm.unregisterReceiver(this) }
        super.onDestroy()
    }
}