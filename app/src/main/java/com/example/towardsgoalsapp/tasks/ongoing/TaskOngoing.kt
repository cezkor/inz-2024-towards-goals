package com.example.towardsgoalsapp.tasks.ongoing

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.OwnerType
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.database.DatabaseObjectFactory
import com.example.towardsgoalsapp.database.TGDatabase
import com.example.towardsgoalsapp.etc.DoubleTapOnBack
import com.example.towardsgoalsapp.etc.OneTimeEvent
import com.example.towardsgoalsapp.etc.OneTimeEventWithValue
import com.example.towardsgoalsapp.etc.errors.ErrorHandling
import com.example.towardsgoalsapp.main.App
import com.example.towardsgoalsapp.main.RefreshTypes
import com.example.towardsgoalsapp.main.ShouldRefreshUIBroadcastReceiver
import com.example.towardsgoalsapp.reminders.ReminderService
import com.example.towardsgoalsapp.stats.questions.Question
import com.example.towardsgoalsapp.stats.questions.RangedDoubleQuestion
import com.example.towardsgoalsapp.stats.questions.ViewModelWithDoubleValueQuestionList
import com.example.towardsgoalsapp.tasks.TaskViewModel
import com.example.towardsgoalsapp.tasks.ongoing.TaskDoingContract.Companion.TASK_ID_TO_REFRESH_FOR_REQUESTER
import com.example.towardsgoalsapp.tasks.ongoing.TaskDoingContract.Companion.TASK_ONGOING_BY_NOTIFICATION
import com.example.towardsgoalsapp.tasks.ongoing.TaskOngoingViewModel.Companion.LONG_BREAK_ID
import com.example.towardsgoalsapp.tasks.ongoing.TaskOngoingViewModel.Companion.WORK_TIME_ID
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.lang.NullPointerException
import java.time.Instant
import java.time.format.DateTimeParseException


class TaskOngoingViewModelFactory(private val dbo: TGDatabase,
                                  private val taskId: Long
    ): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TaskOngoingViewModel(dbo, taskId) as T
    }
}

class TaskOngoingViewModel(private val dbo: TGDatabase,
                           private var taskId: Long
    ):
    TaskViewModel(dbo, taskId, Constants.IGNORE_ID_AS_LONG), ViewModelWithDoubleValueQuestionList {

    companion object {
        const val WORK_TIME_ID = 0
        const val SHORT_BREAK_ID = 1
        const val LONG_BREAK_ID = 2
    }

    // stats mutables
    enum class TaskOngoingStates {
        BEFORE_DOING_TASK, DOING_TASK, MARKING_TASK, FINISHED, NOT_INITIALIZED
    }
    val mutableOfTaskOngoingStates: MutableLiveData<TaskOngoingStates>
        = MutableLiveData(TaskOngoingStates.NOT_INITIALIZED)

    val taskFailed = MutableLiveData<Boolean>(false)

    var reminderId = Constants.IGNORE_ID_AS_LONG
        private set
    private var goalId = Constants.IGNORE_ID_AS_LONG

    var startedDoingTaskOn: Instant? = null
    var endedDoingTaskOn: Instant? = null

    val onFailedToSaveData: MutableLiveData<OneTimeEvent> = MutableLiveData()
    val taskNotPresent: MutableLiveData<OneTimeEvent> = MutableLiveData()

    var pomidoroIsOn: Boolean = false
    val pomidoroIsReady = MutableLiveData(false)
    var pomidoroQuestions: ArrayList<RangedDoubleQuestion?> = arrayListOf(null, null, null)

    val pomidoroSettingsReadyToSave = MutableLiveData<OneTimeEventWithValue<Boolean>>()
    override fun getQuestionsReadyToSave(): MutableLiveData<OneTimeEventWithValue<Boolean>>
            = pomidoroSettingsReadyToSave
    class PomidoroState(
        totalWorkTime: Long = 0,
        currentTime: Long = 0,
        totalTime: Long = 0,
        totalBreakTime: Long = 0,
        breakCount: Long = 0,
        isBreak: Boolean = false
    ) {

        fun determineIndexBasedOnState() : Int {
            if (! isBreak) return WORK_TIME_ID
            return if (isBreakLong && isBreak) LONG_BREAK_ID
            else SHORT_BREAK_ID
        }
        var totalTimeInSeconds: Long = totalTime
            private set

        fun sumTotalAndCurrentTimes() : Long = totalTimeInSeconds + timeOfCurrentStateInSeconds

        var timeOfCurrentStateInSeconds: Long = currentTime
            private set
        var totalTimeOfWorkInSeconds: Long = totalWorkTime
            private set
        var totalTimeOfBreaksInSeconds: Long = totalBreakTime
            private set

        var breakCount: Long = 0
            private set

        var isBreak: Boolean = isBreak
            private set
        var isBreakLong: Boolean = if (breakCount > 0) ((breakCount % 4).toInt() == 0) else false
            private set
        fun switchState() {
            totalTimeInSeconds += timeOfCurrentStateInSeconds
            if (isBreak) {
                isBreak = false
                totalTimeOfBreaksInSeconds += timeOfCurrentStateInSeconds
            }
            else {
                isBreak = true
                breakCount += 1
                isBreakLong = ((breakCount % 4).toInt() == 0)
                totalTimeOfWorkInSeconds += timeOfCurrentStateInSeconds
            }
            timeOfCurrentStateInSeconds = 0
        }

        fun moveTimeBy(seconds: Long) {
            timeOfCurrentStateInSeconds += seconds
        }
    }
    var pomidoroState = PomidoroState()
    class PomidoroSettings(
        var workTimeInMinutes: Int = Constants.DEFAULT_WORK_TIME,
        var shortBreakInMinutes: Int = Constants.DEFAULT_SHORT_BREAK_TIME,
        var longBreakInMinutes: Int = Constants.DEFAULT_LONG_BREAK_TIME
    )
    var pomidoroSettings = PomidoroSettings()
    val pomidoroTimeUpdate: MutableLiveData<OneTimeEventWithValue<Long>> = MutableLiveData(null)
    var pomidoroTimerServiceStartedForTheSameStateAgain = false
    val pomidoroStateChangeRequired: MutableLiveData<OneTimeEvent> = MutableLiveData()
    val pomidoroStateChanged: MutableLiveData<OneTimeEvent> = MutableLiveData()

    override suspend fun getEverything() = getMutex.withLock  {
        val task = taskRepo.getOneById(taskId)
        if (task == null) {
            taskNotPresent.value = OneTimeEvent()
            return@withLock
        }

        arrayOfMutableImpIntDataManager.setUserDataArray(
            impIntRepo.getAllByOwnerTypeAndId(OwnerType.TYPE_TASK, taskId)
        )

        mutableTaskData.value = task

        goalId = mutableTaskData.value!!.goalId

        descriptionOfData.value =
            mutableTaskData.value?.taskDescription ?: descriptionOfData.value

        val reminder = reminderRepo.getOneByOwnerTypeAndId(OwnerType.TYPE_TASK, taskId)
        reminder?.run { reminderId = reminder.remId }
        return@withLock
    }

    override suspend fun saveMainData(): Boolean {
        when (mutableOfTaskOngoingStates.value) {
            TaskOngoingStates.MARKING_TASK -> {
                val taskFailed = taskFailed.value ?: return false
                taskRepo.markTaskCompletion(taskId,taskFailed)
                if (goalId != Constants.IGNORE_ID_AS_LONG) {
                    val now = Instant.now()
                    statsRepo.putNewMarkableTaskStatsData(
                        taskId,
                        goalId,
                        taskFailed,
                        now
                    )
                }
                return true
            }
            TaskOngoingStates.BEFORE_DOING_TASK -> {
                if (pomidoroIsOn) {
                    val wT = pomidoroQuestions[WORK_TIME_ID]?.answer?.toInt()
                    wT?.run { pomidoroSettings.workTimeInMinutes = this }
                    val sB = pomidoroQuestions[SHORT_BREAK_ID]?.answer?.toInt()
                    sB?.run { pomidoroSettings.shortBreakInMinutes = this }
                    val lB = pomidoroQuestions[LONG_BREAK_ID]?.answer?.toInt()
                    lB?.run { pomidoroSettings.longBreakInMinutes = this }
                }
                return true
            }
            else -> return true
        }
    }

    override fun getQuestionList(): ArrayList<Question<Double>?>
        = pomidoroQuestions.map { q -> q }.toCollection(ArrayList())
}

typealias TaskDoingLauncher = ActivityResultLauncher<Long>

class TaskDoingContract: ActivityResultContract<Long, Long>() {

    companion object {
        const val TASK_ID_FROM_REQUESTER = "tdfr"
        const val TASK_ID_TO_REFRESH_FOR_REQUESTER = "tdtidtorfr"
        const val TASK_ONGOING_BY_NOTIFICATION = "tdn"
        const val LOG_TAG = "TDC"
    }
    override fun createIntent(context: Context, input: Long): Intent {
        Log.i(LOG_TAG, "creating intent for $context, input $input")

        return Intent(context, TaskOngoing::class.java).apply {
            action = Intent.ACTION_SEND
            putExtra(TASK_ID_FROM_REQUESTER, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Long {
        val idToRefresh
            = intent?.getLongExtra(TASK_ID_TO_REFRESH_FOR_REQUESTER, Constants.IGNORE_ID_AS_LONG)
        Log.i(LOG_TAG, "got data $idToRefresh, is OK: ${resultCode == Activity.RESULT_OK}")
        return if (resultCode == Activity.RESULT_OK && idToRefresh != null)
            idToRefresh
        else
            Constants.IGNORE_ID_AS_LONG
    }
}

class TaskOngoing : AppCompatActivity() {

    companion object {
        const val LOG_TAG = "TaskOngoing"
        const val TASK_ID = "togtid"
        const val VIEW_STATE_ORDINAL = "togso"
        const val POMIDORO_IS_ON = "togpom"
        const val TOTAL_TIME = "togtot"
        const val TOTAL_WORK_TIME = "togtow"
        const val TOTAL_BREAK_TIME = "togtob"
        const val CURRENT_TIME = "togct"
        const val IS_BREAK = "togib"
        const val BREAK_COUNT = "togbc"
        const val STARTED_ON = "togso"
        const val ENDED_ON = "togeo"

        const val TICK_RCV_INTENT_FILTER = "pomidorotasktick"
        const val END_TIME_MEASUREMENT_INTENT_FILTER = "pomidoropauseorworkend"
        const val ACTIVITY_VISIBLE_INTENT_FILTER = "activityvisible"
    }

    private lateinit var lbm: LocalBroadcastManager

    private lateinit var viewModel: TaskOngoingViewModel

    private lateinit var databaseObject: TGDatabase

    private var taskId: Long = Constants.IGNORE_ID_AS_LONG
    private var lastState: TaskOngoingViewModel.TaskOngoingStates
        = TaskOngoingViewModel.TaskOngoingStates.NOT_INITIALIZED

    private var recoveredPomidoroState : TaskOngoingViewModel.PomidoroState? = null
    private var isPomidoro = false

    private lateinit var tickBroadcastReceiver: BroadcastReceiver
    private lateinit var currentTimePassedReceiver: BroadcastReceiver
    private var serviceRun: Boolean = false

    private var informedServiceOnLeaving = false
    private var openedByNotification = false

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.title_only_menu, menu)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            putLong(TASK_ID, taskId)
            val ordinal = viewModel.mutableOfTaskOngoingStates.value?.ordinal ?:
                TaskOngoingViewModel.TaskOngoingStates.NOT_INITIALIZED.ordinal
            putInt(VIEW_STATE_ORDINAL, ordinal)

            putBoolean(POMIDORO_IS_ON, viewModel.pomidoroIsOn)
            viewModel.startedDoingTaskOn?.run { putString(STARTED_ON, this.toString()) }
            viewModel.endedDoingTaskOn?.run { putString(ENDED_ON, this.toString()) }

            val pState = viewModel.pomidoroState
            putLong(TOTAL_TIME, pState.totalTimeInSeconds)
            putLong(TOTAL_WORK_TIME, pState.totalTimeOfWorkInSeconds)
            putLong(TOTAL_BREAK_TIME, pState.totalTimeOfBreaksInSeconds)
            putLong(CURRENT_TIME, pState.timeOfCurrentStateInSeconds)
            putBoolean(IS_BREAK, pState.isBreak)
            putLong(BREAK_COUNT, pState.breakCount)
        }

        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        unregisterReceivers()
        if (isFinishing && viewModel.mutableOfTaskOngoingStates.value
                == TaskOngoingViewModel.TaskOngoingStates.MARKING_TASK
        ) {
            doAfterFullyDoingTask()
        }
        else informReminderServiceAboutLeaving(false)
        super.onDestroy()
    }

    override fun onStop() {
        signalWhetherActivityHidden(true)
        super.onStop()
    }
    override fun onResume() {
        super.onResume()
        informReminderServiceAboutOngoing()
        signalWhetherActivityHidden(false)
    }

    private fun doAfterFullyDoingTask() {
        informReminderServiceAboutLeaving(true)
        if (openedByNotification) {
            val goalId = viewModel.mutableTaskData.value?.goalId ?: Constants.IGNORE_ID_AS_LONG
            val ownerTask = viewModel.mutableTaskData.value?.taskOwnerId
            lbm.sendBroadcast(
                ShouldRefreshUIBroadcastReceiver.createIntent(
                    goalId, RefreshTypes.GOAL
                )
            )
            lbm.sendBroadcast(
                ShouldRefreshUIBroadcastReceiver.createIntent(
                    taskId, RefreshTypes.TASK
                )
            )
            if (ownerTask != null)
                lbm.sendBroadcast(
                    ShouldRefreshUIBroadcastReceiver.createIntent(
                        ownerTask, RefreshTypes.TASK
                    )
                )
        }
        setResult(Activity.RESULT_OK, intent.apply {
            putExtra(TASK_ID_TO_REFRESH_FOR_REQUESTER, taskId)
        })
    }

    private fun createAndRunService(addDuration: Boolean = false) {
        Log.i(LOG_TAG, "service creation and run")
        serviceRun = true
        val idx = viewModel.pomidoroState.determineIndexBasedOnState()
        val textToDisplay : String
        val duration: Long
        when (idx) {
            WORK_TIME_ID -> {
                textToDisplay = getString(R.string.tasks_pomidoro_work_time)
                duration = if (addDuration) Constants.ADDITIONAL_TIME.toLong() else
                    viewModel.pomidoroSettings.workTimeInMinutes.toLong()
            }
            LONG_BREAK_ID -> {
                textToDisplay = getString(R.string.tasks_pomidoro_long_break)
                duration =
                    if (addDuration) Constants.ADDITIONAL_TIME.toLong() else
                        viewModel.pomidoroSettings.longBreakInMinutes.toLong()
            }
            else -> {
                textToDisplay = getString(R.string.tasks_pomidoro_short_break)
                duration =
                    if (addDuration) Constants.ADDITIONAL_TIME.toLong() else
                        viewModel.pomidoroSettings.shortBreakInMinutes.toLong()
            }
        }
        Log.i(LOG_TAG, "text: $textToDisplay, duration $duration ")
        val serviceIntent = TaskDoingTimingService.createIntent(
            this,
            duration,
            textToDisplay,
            getString(R.string.tasks_time_is_up),
            taskId
        )
        startForegroundService(serviceIntent)
    }

    private fun registerReceivers() {
        lbm.registerReceiver(
            tickBroadcastReceiver,
            IntentFilter(TICK_RCV_INTENT_FILTER)
        )
        lbm.registerReceiver(
            currentTimePassedReceiver,
            IntentFilter(END_TIME_MEASUREMENT_INTENT_FILTER)
        )
    }

    private fun unregisterReceivers() {
        lbm.unregisterReceiver(tickBroadcastReceiver)
        lbm.unregisterReceiver(currentTimePassedReceiver)
    }

    private fun signalWhetherActivityHidden(hidden: Boolean){
        val intent = Intent(ACTIVITY_VISIBLE_INTENT_FILTER).apply {
            putExtra(TaskDoingTimingService.VALUE, hidden)
        }
        lbm.sendBroadcast(intent)
    }

    override fun onRestoreInstanceState(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?
    ) {
        savedInstanceState?.run {
            taskId = this.getLong(TASK_ID, Constants.IGNORE_ID_AS_LONG)
            val stateOrd = this.getInt(
                VIEW_STATE_ORDINAL,
                TaskOngoingViewModel.TaskOngoingStates.NOT_INITIALIZED.ordinal)
            lastState =
                TaskOngoingViewModel.TaskOngoingStates.entries[stateOrd]

            isPomidoro = getBoolean(POMIDORO_IS_ON, isPomidoro)
            try {
                val endInstant = Instant.parse(getString(ENDED_ON))
                viewModel.endedDoingTaskOn?.run { viewModel.endedDoingTaskOn = endInstant }
            }
            catch (_: NullPointerException) { /* ignore */ }
            catch (_: DateTimeParseException) { /* ignore */ }
            try {
                val startInstant = Instant.parse(getString(STARTED_ON))
                viewModel.startedDoingTaskOn?.run { viewModel.startedDoingTaskOn = startInstant }
            }
            catch (_: NullPointerException) { /* ignore */ }
            catch (_: DateTimeParseException) { /* ignore */ }

            if (isPomidoro) {
                val totalTime = getLong(TOTAL_TIME, -1L)
                if (totalTime != -1L && viewModel.pomidoroState.totalTimeInSeconds < totalTime) {
                    val totalBreakTime = getLong(TOTAL_BREAK_TIME, 0)
                    val totalWorkTime = getLong(TOTAL_WORK_TIME, 0)
                    val curTime = getLong(CURRENT_TIME, 0)
                    val breakCount = getLong(BREAK_COUNT, 0)
                    val isBreak = getBoolean(IS_BREAK, false)
                    recoveredPomidoroState = TaskOngoingViewModel.PomidoroState(
                        totalWorkTime,
                        curTime,
                        totalTime,
                        totalBreakTime,
                        breakCount,
                        isBreak)
                    viewModel.pomidoroState = recoveredPomidoroState!!
                }
            }
        }

        super.onRestoreInstanceState(savedInstanceState, persistentState)
    }

    private fun informReminderServiceAboutLeaving(marked: Boolean) {
        if (informedServiceOnLeaving) return
        lbm.sendBroadcast(
            Intent(ReminderService.TASK_OR_HABIT_ONGOING_LEFT_INTENT_FILTER)
                .putExtra(ReminderService.REMINDER_ID, viewModel.reminderId)
                .putExtra(ReminderService.HAS_BEEN_MARKED, marked)
        )
        informedServiceOnLeaving = true
    }

    private fun informReminderServiceAboutOngoing() {
        lbm.sendBroadcast(
            Intent(ReminderService.TASK_OR_HABIT_BEING_ONGOING_INTENT_FILTER)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_ongoing)

        lbm = LocalBroadcastManager.getInstance(this)

        var endInstant: Instant? = null
        var startInstant: Instant? = null

        fun recoverSavedState() {
            savedInstanceState?.run {
                taskId = this.getLong(TASK_ID, Constants.IGNORE_ID_AS_LONG)
                val stateOrd = this.getInt(
                    VIEW_STATE_ORDINAL,
                    TaskOngoingViewModel.TaskOngoingStates.NOT_INITIALIZED.ordinal)
                lastState =
                    TaskOngoingViewModel.TaskOngoingStates.entries[stateOrd]

                isPomidoro = getBoolean(POMIDORO_IS_ON, isPomidoro)
                try {
                    endInstant = Instant.parse(getString(ENDED_ON))
                }
                catch (_: NullPointerException) { /* ignore */ }
                catch (_: DateTimeParseException) { /* ignore */ }
                try {
                    startInstant = Instant.parse(getString(STARTED_ON))
                }
                catch (_: NullPointerException) { /* ignore */ }
                catch (_: DateTimeParseException) { /* ignore */ }

                if (isPomidoro) {
                    val totalTime = getLong(TOTAL_TIME, -1L)
                    if (totalTime != -1L) {
                        val totalBreakTime = getLong(TOTAL_BREAK_TIME, 0)
                        val totalWorkTime = getLong(TOTAL_WORK_TIME, 0)
                        val curTime = getLong(CURRENT_TIME, 0)
                        val breakCount = getLong(BREAK_COUNT, 0)
                        val isBreak = getBoolean(IS_BREAK, false)
                        recoveredPomidoroState = TaskOngoingViewModel.PomidoroState(
                            totalWorkTime,
                            curTime,
                            totalTime,
                            totalBreakTime,
                            breakCount,
                            isBreak)
                    }
                }
            }
        }

        fun getArgs() {
            taskId = intent.getLongExtra(
                TaskDoingContract.TASK_ID_FROM_REQUESTER,
                Constants.IGNORE_ID_AS_LONG)
            openedByNotification = intent.getBooleanExtra(TASK_ONGOING_BY_NOTIFICATION,
                false)
        }

        fun processArgsAndSavedState() {

            (application as App? )?.run {
                databaseObject = DatabaseObjectFactory.newDatabaseObject(this.driver)
            }
            viewModel = ViewModelProvider(this,
                TaskOngoingViewModelFactory(databaseObject, taskId)
            )[TaskOngoingViewModel::class.java]
            viewModel.mutableOfTaskOngoingStates.value = lastState
            startInstant?.run { viewModel.startedDoingTaskOn = this }
            endInstant?.run { viewModel.endedDoingTaskOn = this }

            if (isPomidoro) {
                viewModel.pomidoroIsOn = true
                viewModel.pomidoroState = recoveredPomidoroState!!
            }
        }

        fun prepareUI() {
            viewModel.taskNotPresent.observe(this) {
                it?.handleIfNotHandledWith {
                    this@TaskOngoing.setResult(RESULT_CANCELED)
                    finish()
                }
            }

            viewModel.exceptionMutable.observe(this) {
                ErrorHandling.showExceptionDialog(this, it)
            }

            // title setting
            val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.taskToolbar)
            setSupportActionBar(toolbar)

            viewModel.pomidoroQuestions[TaskOngoingViewModel.WORK_TIME_ID]= RangedDoubleQuestion(
                getString(R.string.tasks_work_time_in_minutes),
                1.0,
                Constants.MAX_WORK_TIME.toDouble(),
                Constants.DEFAULT_WORK_TIME.toDouble()
            )
            viewModel.pomidoroQuestions[TaskOngoingViewModel.SHORT_BREAK_ID]= RangedDoubleQuestion(
                getString(R.string.tasks_short_break_in_minutes),
                1.0,
                Constants.MAX_SHORT_BREAK_TIME.toDouble(),
                Constants.DEFAULT_SHORT_BREAK_TIME.toDouble()
            )
            viewModel.pomidoroQuestions[TaskOngoingViewModel.LONG_BREAK_ID]= RangedDoubleQuestion(
                getString(R.string.tasks_long_break_in_minutes),
                1.0,
                Constants.MAX_LONG_BREAK_TIME.toDouble(),
                Constants.DEFAULT_LONG_BREAK_TIME.toDouble()
            )

            viewModel.mutableTaskData.observe(this) {
                val taskName = viewModel.mutableTaskData.value?.taskName
                    ?: getString(R.string.tasks_name)
                toolbar.title = getString(R.string.tasks_doing_task, taskName)
            }

            // orchestrating fragments
            val beforeDoingFragment = TaskBeforeDoing()
            val beforeDoingBackCallback
            = DoubleTapOnBack(this, getString(R.string.tasks_abandon_task)) {
                this@TaskOngoing.setResult(RESULT_CANCELED)
                finish()
            }
            val taskDoingFragment = TaskDoing()
            val taskDoingBackCallback = object: OnBackPressedCallback(false) {
                override fun handleOnBackPressed() {
                    if (isEnabled) {
                        Toast.
                        makeText(
                            this@TaskOngoing,
                            this@TaskOngoing.getString(R.string.tasks_keep_doing_task),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            val afterDoingFragment = TaskMarking()
            val afterDoingBackCallback = object: OnBackPressedCallback(false) {
                override fun handleOnBackPressed() {
                    if (isEnabled) {
                        Toast.
                        makeText(
                            this@TaskOngoing,
                            this@TaskOngoing.getString(R.string.tasks_please_mark_task),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            beforeDoingBackCallback.isEnabled = false
            afterDoingBackCallback.isEnabled = false
            taskDoingBackCallback.isEnabled = false

            viewModel.onFailedToSaveData.observe(this) {
                it.handleIfNotHandledWith {
                    // so to allow for leaving if not able to save data
                    afterDoingBackCallback.isEnabled = false
                }
            }

            val stateObserver = Observer<TaskOngoingViewModel.TaskOngoingStates> {
                val fm = supportFragmentManager

                if (it != null) lastState = it

                when (it) {
                    TaskOngoingViewModel.TaskOngoingStates.BEFORE_DOING_TASK -> {
                        fm.beginTransaction().apply {
                            setReorderingAllowed(true)
                            setCustomAnimations(R.anim.layout_accelerator, R.anim.layout_deccelerator)
                            replace(R.id.taskStagesFrameLayout, beforeDoingFragment)
                        }.commit()

                        beforeDoingBackCallback.isEnabled = true
                        taskDoingBackCallback.isEnabled = false
                        afterDoingBackCallback.isEnabled = false
                        onBackPressedDispatcher.addCallback(beforeDoingBackCallback)
                    }
                    TaskOngoingViewModel.TaskOngoingStates.DOING_TASK -> {

                        Log.i(LOG_TAG, "task pomidoro on: ${viewModel.pomidoroIsOn};" +
                                "times (work, short, long): " +
                                "${viewModel.pomidoroQuestions[WORK_TIME_ID]?.answer}, " +
                        "${viewModel.pomidoroQuestions[TaskOngoingViewModel.SHORT_BREAK_ID]?.answer}," +
                                "${viewModel.pomidoroQuestions[LONG_BREAK_ID]?.answer}")

                        if (viewModel.startedDoingTaskOn == null)
                            viewModel.startedDoingTaskOn = Instant.now()

                        if (! serviceRun && viewModel.pomidoroIsOn) createAndRunService()

                        fm.beginTransaction().apply {
                            setReorderingAllowed(true)
                            setCustomAnimations(R.anim.layout_accelerator, R.anim.layout_deccelerator)
                            replace(R.id.taskStagesFrameLayout, taskDoingFragment)
                        }.commit()

                        beforeDoingBackCallback.isEnabled = false
                        taskDoingBackCallback.isEnabled = true
                        afterDoingBackCallback.isEnabled = false
                        onBackPressedDispatcher.addCallback(taskDoingBackCallback)
                    }
                    TaskOngoingViewModel.TaskOngoingStates.MARKING_TASK -> {
                        if (viewModel.endedDoingTaskOn == null)
                            viewModel.endedDoingTaskOn = Instant.now()

                        if (serviceRun) {
                            Log.i(LOG_TAG, "stopping service")
                            serviceRun = false
                            unregisterReceivers()
                            stopService(
                                Intent(this@TaskOngoing, TaskDoingTimingService::class.java)
                            )
                            NotificationManagerCompat.from(this)
                                .cancel(TaskDoingTimingService.ID)
                        }

                        fm.beginTransaction().apply {
                            setReorderingAllowed(true)
                            setCustomAnimations(R.anim.layout_accelerator, R.anim.layout_deccelerator)
                            replace(R.id.taskStagesFrameLayout, afterDoingFragment)
                        }.commit()

                        beforeDoingBackCallback.isEnabled = false
                        taskDoingBackCallback.isEnabled = false
                        afterDoingBackCallback.isEnabled = true
                        onBackPressedDispatcher.addCallback(afterDoingBackCallback)
                    }
                    TaskOngoingViewModel.TaskOngoingStates.FINISHED -> {
                        doAfterFullyDoingTask()
                        finish()
                    }
                    else -> {}
                }
            }
            viewModel.mutableOfTaskOngoingStates.observe(this, stateObserver)
        }

        fun prepareReceiversAndObjectsRelatedToThem() {

            tickBroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    // by "ticks" it is implied they're seconds - OneSecondTicker
                    if (intent == null) return
                    if (intent.action != TICK_RCV_INTENT_FILTER) return
                    // one tick - roughly one second
                    val tickCount = intent.extras?.getLong(TaskDoingTimingService.TICK_COUNT) ?: 0
                    val currentSeconds = viewModel.pomidoroState.timeOfCurrentStateInSeconds
                    val shift = if (tickCount > currentSeconds) tickCount - currentSeconds else 0
                    viewModel.pomidoroTimeUpdate.value = OneTimeEventWithValue<Long>(shift)
                }
            }

            currentTimePassedReceiver =  object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent == null) return
                    if (intent.action != END_TIME_MEASUREMENT_INTENT_FILTER) return
                    viewModel.pomidoroTimerServiceStartedForTheSameStateAgain = true
                    createAndRunService()
                    viewModel.pomidoroStateChangeRequired.value = OneTimeEvent()
                }
            }
            registerReceivers()

            viewModel.pomidoroStateChanged.observe(this) {
                it?.handleIfNotHandledWith {
                    stopService(Intent(this, TaskDoingTimingService::class.java))
                    viewModel.pomidoroTimerServiceStartedForTheSameStateAgain = false
                    createAndRunService()
            } }
        }

        fun doInThisOrder() {

            recoverSavedState()

            getArgs()

            processArgsAndSavedState()

            prepareUI()

            prepareReceiversAndObjectsRelatedToThem()

            lifecycleScope.launch(viewModel.exceptionHandler)
            { repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.getEverything()

                viewModel.mutableOfTaskOngoingStates.value =
                    TaskOngoingViewModel.TaskOngoingStates.BEFORE_DOING_TASK
            } }

        }; doInThisOrder()

    }


}