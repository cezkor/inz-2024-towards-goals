package com.example.towardsgoalsapp.reminders

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import app.cash.sqldelight.db.SqlDriver
import com.example.towardsgoalsapp.BuildConfig
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.OwnerType
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.database.DatabaseGeneration
import com.example.towardsgoalsapp.database.DatabaseObjectFactory
import com.example.towardsgoalsapp.database.ReminderData
import com.example.towardsgoalsapp.database.TGDatabase
import com.example.towardsgoalsapp.database.repositories.HabitRepository
import com.example.towardsgoalsapp.database.repositories.ReminderRepository
import com.example.towardsgoalsapp.database.repositories.TaskRepository
import com.example.towardsgoalsapp.etc.NameFixer
import com.example.towardsgoalsapp.etc.PermissionHelper
import com.example.towardsgoalsapp.habits.questioning.HabitQuestioningContract
import com.example.towardsgoalsapp.habits.questioning.HabitQuestions
import com.example.towardsgoalsapp.tasks.ongoing.TaskDoingContract
import com.example.towardsgoalsapp.tasks.ongoing.TaskDoingTimingService
import com.example.towardsgoalsapp.tasks.ongoing.TaskOngoing
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.PriorityQueue
import java.util.concurrent.atomic.AtomicBoolean
class ReminderService : LifecycleService() {

    inner class ReminderToFire (
        val rData: ReminderData,
        val notification: Notification
    )

    private lateinit var lbm: LocalBroadcastManager

    private lateinit var taskOrHabitBeingOngoing: BroadcastReceiver
    private lateinit var taskOrHabitOngoingLeft: BroadcastReceiver
    private lateinit var editOngoing: BroadcastReceiver
    private lateinit var isServiceAlive: BroadcastReceiver
    private lateinit var alarmFired: BroadcastReceiver
    private lateinit var reminderObsolete: BroadcastReceiver

    private lateinit var serviceDriver: SqlDriver
    private lateinit var databaseObj: TGDatabase
    private lateinit var reminderRepo: ReminderRepository
    private lateinit var habitRepo: HabitRepository
    private lateinit var taskRepo: TaskRepository

    private var remindersRequiringFiringQueue = PriorityQueue<ReminderToFire> { o1, o2 ->
        ReminderLogic.comparator.compare(
            o1?.rData,
            o2?.rData
        )
    }
    private var queueMutex = Mutex()
    private var databaseMutex = Mutex()
    private var remindersMutex = Mutex()

    private var currentReminderToAlarm: ReminderData? = null
    private var isAlarmOngoing: AtomicBoolean = AtomicBoolean(false)
    private val remindersThatShouldHaveBeenFiredUntilNow: HashSet<ReminderData> = HashSet()
    private var canFireNotification: AtomicBoolean = AtomicBoolean(false)
    private var notificationThreadSignalingObj = Object()
    private var alarmThreadSignalingObj = Object()
    private var ntThreadShouldLeave: AtomicBoolean = AtomicBoolean(false)
    private var alThreadShouldLeave: AtomicBoolean = AtomicBoolean(false)
    private var notificationThreadStarted = false

    private lateinit var icon: Icon

    companion object {
        const val LOG_TAG = "RemService"
        const val ID = 1002
        const val NID = 1003

        const val REMINDER_ID = "rsrmid"
        const val HAS_BEEN_MARKED = "rshbm"
        const val BLOCK_NOTIFICATIONS = "rsbn"

        const val TASK_OR_HABIT_BEING_ONGOING_INTENT_FILTER = "taskorhabitongoing"
        const val TASK_OR_HABIT_ONGOING_LEFT_INTENT_FILTER = "taskorhabitongoingleft"
        const val EDIT_ONGOING_INTENT_FILTER = "editongoing"
        const val IS_SERVICE_ALIVE_INTENT_FILTER = "isalive"
        const val SERVICE_ALIVE_INTENT_FILTER = "sialive"
        const val ALARM_FIRED_INTENT_FILTER = "alarmfired"
        const val REMINDER_OBSOLETE_INTENT_FILTER = "reminderobsolete"
    }

    private fun startAlarmThread(reminderId: Long, timeInMilis: Long) = Thread {

        val threadLBM = LocalBroadcastManager.getInstance(this@ReminderService)

        fun broadcastAlarm(reminderId: Long) {
            Log.e(LOG_TAG, "alarming thread broadcasts ALARM_FIRED for $reminderId")
            threadLBM.sendBroadcast(
                Intent(ALARM_FIRED_INTENT_FILTER).apply {
                putExtra(REMINDER_ID, reminderId)
                    }
            )
        }

        if (isAlarmOngoing.get()) {
            Log.e(LOG_TAG, "alarming thread started when there is already an alarm; leaving")
            return@Thread
        }
        isAlarmOngoing.set(true)

        val current = Instant.now().toEpochMilli()
        if (current > timeInMilis) {
            Log.e(LOG_TAG, "alarming thread alarms now reminder $reminderId")
            broadcastAlarm(reminderId)
        }
        else {
            alThreadShouldLeave.set(false)
            val waitBy = timeInMilis - current
            var leave = false
            while (! leave) {
                try {
                    // it won't be exact; although I was unable to make AlarmManager work for scheduling
                    // it is a known issue
                    // https://stackoverflow.com/
                    // questions/52455317/alarmmanager-is-not-executing-a-task-at-a-particular-time?rq=3
                    Log.i(LOG_TAG, "alarming thread waits for $reminderId")
                    synchronized(alarmThreadSignalingObj) {
                        alarmThreadSignalingObj.wait(waitBy)
                    }
                    Log.i(LOG_TAG, "alarming thread stopped waiting for $reminderId")
                    val sLeave = alThreadShouldLeave.get()
                    if (! sLeave) {
                        broadcastAlarm(reminderId)
                    }
                    leave = true

                }
                catch (e: InterruptedException) {
                    if (alThreadShouldLeave.get())
                        leave = true
                }
            }
        }
        isAlarmOngoing.set(false)
        Log.i(LOG_TAG, "alarming thread leaves")
    }.start()

    private fun stopAlarmThread() {
        alThreadShouldLeave.set(true)
        try {
            synchronized(alarmThreadSignalingObj) {
                alarmThreadSignalingObj.notify()
            }
        }
        catch (e: IllegalMonitorStateException) {
            Log.e(LOG_TAG, "exception", e)
        }
    }

    @SuppressLint("MissingPermission") // because it is checked by ReminderPermissionHelper
    private fun startNotificationThread() {
        Log.i(LOG_TAG, "notification thread started: $notificationThreadStarted")
        if (notificationThreadStarted) return
        Thread {
            Log.i(LOG_TAG, "notification thread starting")
            notificationThreadStarted = true
            var leave = ntThreadShouldLeave.get()
            while (! leave) {

                if (canFireNotification.get()) { runBlocking {
                    Log.i(LOG_TAG, "notification thread polls reminder to fire")
                    val reminderToFire : ReminderToFire? = queueMutex.withLock {
                        remindersRequiringFiringQueue.poll()
                    }
                    reminderToFire?.run {
                        if (PermissionHelper.canPostNotifications(this@ReminderService)) {
                            Log.i(LOG_TAG, "notification thread " +
                                    "notifies reminder ${this.rData.remId}")
                            NotificationManagerCompat.from(this@ReminderService)
                                .notify(NID, reminderToFire.notification)
                            canFireNotification.set(false)
                        }
                    }
                    if (reminderToFire == null)
                        Log.i(LOG_TAG, "notification thread didn't poll any reminder")

                } }

                Log.i(LOG_TAG, "notification thread after polling")

                synchronized(notificationThreadSignalingObj) {
                    try {
                        notificationThreadSignalingObj.wait()
                    }catch (e: InterruptedException) {
                        Log.i(LOG_TAG, "notification thread interrupted; assume leaving")
                        ntThreadShouldLeave.set(false)
                        leave = false
                    }
                }
                leave = ntThreadShouldLeave.get()
                Log.i(LOG_TAG, "notification thread should leave $leave")
            }
        }.start()
    }

    private fun stopNotificationThread() {
        ntThreadShouldLeave.set(true)
        notifyNotificationThread()
    }

    private fun notifyNotificationThread() {
        try {
            synchronized(notificationThreadSignalingObj) {
                notificationThreadSignalingObj.notify()
            }
        }
        catch (e: IllegalMonitorStateException) {
            Log.e(LOG_TAG, "exception", e)
        }
    }

    private fun registerReceivers() {
        lbm.registerReceiver(reminderObsolete, IntentFilter(REMINDER_OBSOLETE_INTENT_FILTER))
        lbm.registerReceiver(editOngoing, IntentFilter(EDIT_ONGOING_INTENT_FILTER))
        lbm.registerReceiver(taskOrHabitBeingOngoing,
            IntentFilter(TASK_OR_HABIT_BEING_ONGOING_INTENT_FILTER)
        )
        lbm.registerReceiver(taskOrHabitOngoingLeft,
            IntentFilter(TASK_OR_HABIT_ONGOING_LEFT_INTENT_FILTER)
        )
        lbm.registerReceiver(isServiceAlive, IntentFilter(IS_SERVICE_ALIVE_INTENT_FILTER))
        lbm.registerReceiver(alarmFired, IntentFilter(ALARM_FIRED_INTENT_FILTER))
    }

    private fun unregisterReceivers() {
        lbm.unregisterReceiver(taskOrHabitOngoingLeft)
        lbm.unregisterReceiver(editOngoing)
        lbm.unregisterReceiver(taskOrHabitBeingOngoing)
        lbm.unregisterReceiver(isServiceAlive)
        lbm.unregisterReceiver(alarmFired)
        lbm.unregisterReceiver(reminderObsolete)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onCreate() {
        super.onCreate()
        lbm = LocalBroadcastManager.getInstance(this)
        // driver has to be created independently of application
        // as this service may be started when the app is not running
        serviceDriver =
            DatabaseGeneration.getDriver(this@ReminderService)
        databaseObj = DatabaseObjectFactory.newDatabaseObject(serviceDriver)
        reminderRepo = ReminderRepository(databaseObj)
        habitRepo = HabitRepository(databaseObj)
        taskRepo = TaskRepository(databaseObj)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent == null) {
            Log.e(LOG_TAG, "start intent is null")
            return START_NOT_STICKY
        }

        icon = Icon.createWithResource(this, R.drawable.full_oval)

        Log.i(LOG_TAG, "service starting")

        val reminderNotificationChannel = NotificationChannel(
            Constants.REMINDER_NOTIFICATION_CHANNEL,
            "TowardsGoals: " + getString(R.string.reminders_reminder_plural),
            NotificationManager.IMPORTANCE_HIGH
        )
        NotificationManagerCompat.from(this)
            .createNotificationChannel(reminderNotificationChannel)

        reminderObsolete = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                if (intent.action != REMINDER_OBSOLETE_INTENT_FILTER) return
                val remId = intent.extras?.getLong(REMINDER_ID) ?: return
                lifecycleScope.launch {
                    val reminder = databaseMutex.withLock { reminderRepo.getOneById(remId) }
                    Log.i(LOG_TAG, "reminder obsolete: $remId; exists: ${reminder != null}")
                    reminder?.run {
                        handleObsoleteReminder(reminder)

                        val currentReminder = remindersMutex.withLock { currentReminderToAlarm }
                        if (currentReminder != null && currentReminder.remId == this.remId) {
                            Log.i(LOG_TAG, "reminder was the reminder to be alarmed; " +
                                    "recalculating what reminder to alarm now")
                            recalcCurrentReminderToAlarm()
                            setAlarmForNow()
                        }
                    }
                }
            }
        }
        alarmFired = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                if (intent.action != ALARM_FIRED_INTENT_FILTER) return
                val remId = intent.extras?.getLong(REMINDER_ID) ?: return
                Log.i(LOG_TAG, "alarm fired for reminder: $remId")
                handleAlarmConcurrently(remId)
            }
        }
        taskOrHabitBeingOngoing = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                if (intent.action != TASK_OR_HABIT_BEING_ONGOING_INTENT_FILTER) return
                Log.i(LOG_TAG, "task or habit is ongoing")
                // don't trigger alarms if task/habit are ongoing
                lifecycleScope.launch {
                    removeCurrentAlarm()
                    canFireNotification.set(false)
                }
            }
        }
        taskOrHabitOngoingLeft = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                if (intent.action != TASK_OR_HABIT_ONGOING_LEFT_INTENT_FILTER) return
                val remId = intent.extras?.getLong(REMINDER_ID)
                val marked = intent.extras?.getBoolean(HAS_BEEN_MARKED) ?: return
                Log.i(LOG_TAG, "task or habit are not ongoing; " +
                        "reminder of it $remId " +
                        "did user mark it: $marked")
                lifecycleScope.launch {
                    if (remId != null && remId != Constants.IGNORE_ID_AS_LONG) {
                        handleConcurrentlyMarking(remId, marked)
                    }
                    else {
                        setAlarmForNow()
                    }
                    canFireNotification.set(true)
                }
            }
        }
        editOngoing = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                if (intent.action != EDIT_ONGOING_INTENT_FILTER) return

                val blockNotification = intent.extras?.getBoolean(BLOCK_NOTIFICATIONS) ?: return
                Log.i(LOG_TAG, "edit of goal/task/habit details ongoing;" +
                        "should block sending notifications: $blockNotification")
                lifecycleScope.launch {
                    if (blockNotification)
                        canFireNotification.set(false)
                    else {
                        // reminders might have been changed
                        recalcCurrentReminderToAlarm()
                        setAlarmForNow()
                        canFireNotification.set(true)
                        notifyNotificationThread()
                    }
                }
            }
        }
        isServiceAlive = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                if (intent.action != IS_SERVICE_ALIVE_INTENT_FILTER) return
                Log.i(LOG_TAG, "got isServiceAlive")
                lbm.sendBroadcastSync(Intent(SERVICE_ALIVE_INTENT_FILTER))
            }
        }
        registerReceivers()

        startNotificationThread()

        val foregroundNotificationBuilder = Notification
            .Builder(this, Constants.REMINDER_NOTIFICATION_CHANNEL)
            .setContentTitle(getString(R.string.reminders_reminder_plural))
            .setContentText(getString(R.string.reminders_notification_reasoning))
            .setSmallIcon(icon)
            .setOngoing(true)
        NotificationManagerCompat
            .from(this)
            .cancel(ID)
        startForeground(ID, foregroundNotificationBuilder.build())

        if (BuildConfig.SHOULD_USE_TEST_DATA) { lifecycleScope.launch {
            if (DatabaseGeneration.assureDatabaseHasTestData(databaseObj)){
                Log.i(LOG_TAG, "recalculating reminders because of filled out test data")
                recalcCurrentReminderToAlarm()
                setAlarmForNow()
            }
        } }

        // set reminder
        lifecycleScope.launch {
            recalcCurrentReminderToAlarm()
            setAlarmForNow()
            canFireNotification.set(true)
        }

        Log.i(LOG_TAG, "service started")

        return START_REDELIVER_INTENT
    }

    // if task/habit is not marked before its reminder had to fire, don't remove the reminder
    // otherwise, remove the reminder ( <==> the user have been reminded or didn't have to be reminded)

    private suspend fun handleObsoleteReminder(reminder: ReminderData) = databaseMutex.withLock {
        if (reminder.ownerType == OwnerType.TYPE_TASK) {
            Log.i(LOG_TAG, "obsolete reminder ${reminder.remId} for task ${reminder.ownerId}")
            // task once done does not need to be reminded of
            reminderRepo.deleteById(reminder.remId)
        }
        else {
            Log.i(LOG_TAG, "obsolete reminder ${reminder.remId} for habit ${reminder.ownerId}")
            // habit reminders, if user later doesn't change that,
            // have to be rescheduled on next day in the same time of day
            reminderRepo.updateLastReminded(reminder.remId, Instant.now())
            reminderRepo.updateRemindOn(
                reminder.remId,
                reminder.remindOn.plus(1, ChronoUnit.DAYS)
            )
            recalcCurrentReminderToAlarm()
            setAlarmForNow()
        }
    }
    private fun handleConcurrentlyMarking(remId: Long, marked: Boolean) = lifecycleScope.launch {
        val reminder = reminderRepo.getOneById(remId)
        if (reminder == null) {
            Log.w(LOG_TAG, "reminder $remId didn't exist")
            setAlarmForNow()
            return@launch
        }
        val now = Instant.now()
        Log.i(LOG_TAG, "owner: ${reminder.ownerType.typeString}," +
                "ownerId: ${reminder.ownerId}" +
                "; reminder $remId;" +
                " is marked: $marked")
        if (! marked && now > reminder.remindOn) {
            Log.i(LOG_TAG, "reminder $remId still valid")
            setAlarmForNow()
            return@launch
        }
        handleObsoleteReminder(reminder)
        val cRemToAl = remindersMutex.withLock { currentReminderToAlarm }
        val wasCurrentAlarm = (cRemToAl != null && cRemToAl.remId == remId)
        Log.i(LOG_TAG, "reminder $remId was current alarm: $wasCurrentAlarm")
        if (wasCurrentAlarm) {
            lifecycleScope.launch { recalcCurrentReminderToAlarm() }
        }
        setAlarmForNow()
    }

    private suspend fun recalcCurrentReminderToAlarm() {
        removeCurrentAlarm()
        val ordered = databaseMutex.withLock {
             ReminderLogic
                .getOrderedListOfReminder(reminderRepo.getAll())
        }
        ordered?.run {
            val now = Instant.now()
            val toBeQueuedToFireNow = this.filter { it.remindOn <= now }
            remindersMutex.withLock {
                if (toBeQueuedToFireNow.isNotEmpty())
                    remindersThatShouldHaveBeenFiredUntilNow.addAll(toBeQueuedToFireNow)
                currentReminderToAlarm = this.firstOrNull{ it.remindOn > now }
                Log.i(LOG_TAG, "current reminder ${currentReminderToAlarm?.remId};" +
                        "has reminders to fire now: " +
                        "${remindersThatShouldHaveBeenFiredUntilNow.isNotEmpty()}")
                if (remindersThatShouldHaveBeenFiredUntilNow.isNotEmpty()) {
                    // fire them
                    lifecycleScope.launch {
                        Log.i(LOG_TAG, "count of reminders that should have fired until now: " +
                                "${remindersThatShouldHaveBeenFiredUntilNow.size}")
                        val asyncs = remindersThatShouldHaveBeenFiredUntilNow.map {
                            async { handleReminderToFire(it) }
                        }
                        remindersThatShouldHaveBeenFiredUntilNow.clear()
                        asyncs.awaitAll()
                        Log.i(LOG_TAG, "fired all reminders queued to fire now")
                    }
                }
            }
        }

    }

    private suspend fun removeCurrentAlarm() = remindersMutex.withLock {
        if (isAlarmOngoing.get()) {
            Log.i(LOG_TAG, "canceling alarm")
            // cancel
            alThreadShouldLeave.set(true)
            stopAlarmThread()
        }
    }

    private fun setAlarmForNow() {
        lifecycleScope.launch {

            removeCurrentAlarm()

            Log.i(LOG_TAG, "setting alarm")

            val reminder : ReminderData?
            remindersMutex.withLock{ reminder = currentReminderToAlarm }
            if (reminder == null) {
                Log.i(LOG_TAG, "no alarm to set")
                return@launch
            }
            Log.i(LOG_TAG, "alarm for reminder ${reminder.remId}")

            val instantToMilis = reminder.remindOn.toEpochMilli()

            startAlarmThread(reminder.remId, instantToMilis)

            Log.i(LOG_TAG, "alarm for reminder ${reminder.remId} has been set")

        }
    }

    private suspend fun handleReminderToFire(reminder: ReminderData) {
        Log.i(LOG_TAG, "handling reminder ${reminder.remId}")
        val name: String
        val type: String
        val doType: String
        val intent: Intent
        when (reminder.ownerType) {
            OwnerType.TYPE_TASK -> {
                type = getString(R.string.tasks_name).lowercase()
                doType = getString(R.string.tasks_do)
                val task = taskRepo.getOneById(reminder.ownerId)
                name = if (task == null) {
                    getString(R.string.tasks_name)
                } else {
                    NameFixer.fix(task.taskName)
                }
                intent = Intent(this@ReminderService, TaskOngoing::class.java)
                    .apply {
                        action = Intent.ACTION_SEND
                        putExtra(TaskDoingContract.TASK_ID_FROM_REQUESTER, reminder.ownerId)
                        putExtra(TaskDoingContract.TASK_ONGOING_BY_NOTIFICATION, true)
                    }
            }

            OwnerType.TYPE_HABIT -> {
                type = getString(R.string.habits_name).lowercase()
                doType = getString(R.string.habits_mark)
                val habit = habitRepo.getOneById(reminder.ownerId)
                name = if (habit == null) {
                    getString(R.string.habits_name)
                } else {
                    NameFixer.fix(habit.habitName)
                }
                intent = Intent(this@ReminderService, HabitQuestions::class.java)
                    .apply {
                        action = Intent.ACTION_SEND
                        putExtra(HabitQuestioningContract.HABIT_ID_FROM_REQUESTER, reminder.ownerId)
                        putExtra(HabitQuestioningContract.HABIT_MARKING_BY_NOTIFICATION, true)
                    }
            }

            else -> return
        }
        val reminderText = getString(
            R.string.reminders_reminder_about,
            doType,
            type,
            name
        )

        val notificationBuilder = Notification
            .Builder(this@ReminderService, Constants.REMINDER_NOTIFICATION_CHANNEL)
            .setContentTitle(getString(R.string.reminders_reminder))
            .setContentText(reminderText)
            .setOngoing(false)
            .setAutoCancel(true)
            .setSmallIcon(icon)
            .setContentIntent(
                PendingIntent
                    .getActivity(
                        this@ReminderService, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
            )
            .setDeleteIntent(
                PendingIntent.getBroadcast(
                    this@ReminderService, 0,
                    Intent(REMINDER_OBSOLETE_INTENT_FILTER)
                        .putExtra(REMINDER_ID, reminder.remId),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )

        if (!PermissionHelper.canPostNotifications(this@ReminderService)) {
            Log.i(
                LOG_TAG, "unable to notify about reminder ${reminder.remId}" +
                        " ; considering it obsolete"
            )
            handleObsoleteReminder(reminder)
        } else
            queueMutex.withLock {
                remindersRequiringFiringQueue.add(
                    ReminderToFire(
                        reminder,
                        notificationBuilder.build()
                    )
                )
                Log.i(LOG_TAG, "queued to fire reminder ${reminder.remId}")
                notifyNotificationThread()
            }
    }

    @SuppressLint("MissingPermission") // is checked with permission helper
    private fun handleAlarmConcurrently(remId: Long) {

        lifecycleScope.launch {

            var reminder: ReminderData? = null
            val currentReminder = remindersMutex.withLock { currentReminderToAlarm }
            if (currentReminder != null && currentReminder.remId == remId) {
                reminder = currentReminder
            }
            if (reminder == null)
                reminder = databaseMutex.withLock { reminderRepo.getOneById(remId) }

            // check if should fire reminders ought to be fired until now
            var currentReminderIsAmongThoseQueuedToBeFiredUntilNow = false
            remindersMutex.withLock {
                if (remindersThatShouldHaveBeenFiredUntilNow.isNotEmpty()) {

                    Log.i(LOG_TAG, "count of reminders that should have fired until now: " +
                    "${remindersThatShouldHaveBeenFiredUntilNow.size}")
                    val asyncs = remindersThatShouldHaveBeenFiredUntilNow.map {
                        if (it.remId == remId)
                            currentReminderIsAmongThoseQueuedToBeFiredUntilNow = true
                        async { handleReminderToFire(it) }
                    }
                    remindersThatShouldHaveBeenFiredUntilNow.clear()
                    asyncs.awaitAll()
                    Log.i(LOG_TAG, "service handled all reminders that should " +
                            "have fired until now; proceeding to given reminder")
                }
            }
            Log.i(LOG_TAG, "does service have reminder $remId: " +
                    "${reminder != null}; was it among those to be fired: " +
                    "$currentReminderIsAmongThoseQueuedToBeFiredUntilNow")
            if (reminder == null) return@launch

            if (! currentReminderIsAmongThoseQueuedToBeFiredUntilNow) {
                handleReminderToFire(reminder)
                recalcCurrentReminderToAlarm()
                setAlarmForNow()
            }

        }
    }

    override fun onDestroy() {
        unregisterReceivers()
        stopNotificationThread()
        stopAlarmThread()
        super.onDestroy()
    }

}