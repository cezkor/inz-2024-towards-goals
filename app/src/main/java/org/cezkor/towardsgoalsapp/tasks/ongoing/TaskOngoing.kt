package org.cezkor.towardsgoalsapp.tasks.ongoing

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import android.view.WindowManager
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
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.database.DatabaseObjectFactory
import org.cezkor.towardsgoalsapp.database.TGDatabase
import org.cezkor.towardsgoalsapp.etc.DoubleTapOnBack
import org.cezkor.towardsgoalsapp.etc.OneTimeEvent
import org.cezkor.towardsgoalsapp.etc.OneTimeEventWithValue
import org.cezkor.towardsgoalsapp.etc.errors.ErrorHandling
import org.cezkor.towardsgoalsapp.main.App
import org.cezkor.towardsgoalsapp.etc.RefreshTypes
import org.cezkor.towardsgoalsapp.etc.ShouldRefreshUIBroadcastReceiver
import org.cezkor.towardsgoalsapp.reminders.ReminderService
import org.cezkor.towardsgoalsapp.stats.questions.Question
import org.cezkor.towardsgoalsapp.stats.questions.RangedDoubleQuestion
import org.cezkor.towardsgoalsapp.stats.questions.ViewModelWithDoubleValueQuestionList
import org.cezkor.towardsgoalsapp.tasks.TaskViewModel
import org.cezkor.towardsgoalsapp.tasks.ongoing.TaskDoingContract.Companion.TASK_ID_TO_REFRESH_FOR_REQUESTER
import org.cezkor.towardsgoalsapp.tasks.ongoing.TaskDoingContract.Companion.TASK_ONGOING_BY_NOTIFICATION
import org.cezkor.towardsgoalsapp.tasks.ongoing.TaskOngoingViewModel.Companion.LONG_BREAK_ID
import org.cezkor.towardsgoalsapp.tasks.ongoing.TaskOngoingViewModel.Companion.WORK_TIME_ID
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import java.lang.NullPointerException
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong


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
    TaskViewModel(dbo, taskId, org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG), ViewModelWithDoubleValueQuestionList {

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

    var reminderId = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG
        private set
    private var goalId = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG

    var startedDoingTaskOn: Instant? = null
    var endedDoingTaskOn: Instant? = null

    val onFailedToSaveData: MutableLiveData<OneTimeEvent> = MutableLiveData()
    val taskNotPresent: MutableLiveData<OneTimeEvent> = MutableLiveData()

    var pomodoroIsOn: Boolean = false
    val pomodoroIsReady = MutableLiveData(false)
    var pomodoroQuestions: ArrayList<RangedDoubleQuestion?> = arrayListOf(null, null, null)

    val pomodoroSettingsReadyToSave = MutableLiveData<OneTimeEventWithValue<Boolean>>()
    override fun getQuestionsReadyToSave(): MutableLiveData<OneTimeEventWithValue<Boolean>>
            = pomodoroSettingsReadyToSave

    var pomodoroState = PomodoroState()
    var pomodoroSettings = PomodoroSettings()
    val pomodoroTimeUpdate: MutableLiveData<OneTimeEventWithValue<Long>> = MutableLiveData(null)
    var pomodoroTimerTickingForTheSameState = AtomicBoolean(false)
    var pomodoroStateChangedByUser = AtomicBoolean(false)
    val pomodoroStateChangeRequired: MutableLiveData<OneTimeEvent> = MutableLiveData()
    val pomodoroStartTicking: MutableLiveData<OneTimeEvent> = MutableLiveData()

    val lastTick = AtomicLong(0L)

    override suspend fun getEverything() = getMutex.withLock  {
        val task = taskRepo.getOneById(taskId)
        if (task == null) {
            taskNotPresent.value = OneTimeEvent()
            return@withLock
        }

        arrayOfMutableImpIntDataManager.setUserDataArray(
            impIntRepo.getAllByOwnerTypeAndId(org.cezkor.towardsgoalsapp.OwnerType.TYPE_TASK, taskId)
        )

        mutableTaskData.value = task

        goalId = mutableTaskData.value!!.goalId

        descriptionOfData.value =
            mutableTaskData.value?.taskDescription ?: descriptionOfData.value

        nameOfData.value = mutableTaskData.value?.taskName ?: nameOfData.value

        val reminder = reminderRepo.getOneByOwnerTypeAndId(org.cezkor.towardsgoalsapp.OwnerType.TYPE_TASK, taskId)
        reminder?.run { reminderId = reminder.remId }
        return@withLock
    }

    override suspend fun saveMainData(): Boolean {
        when (mutableOfTaskOngoingStates.value) {
            TaskOngoingStates.MARKING_TASK -> {
                val taskFailed = taskFailed.value ?: return false
                taskRepo.markTaskCompletion(taskId,taskFailed)
                if (goalId != org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) {
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
                fun getAnswerOrDefault(question: RangedDoubleQuestion?) : Int {
                    val ans = question?.answer
                    val ret = ans?.toInt() ?: question?.defaultValue?.toInt() ?: 1
                    question?.answerWith(ret.toDouble())
                    return ret
                }

                if (pomodoroIsOn) {
                    val wT = getAnswerOrDefault(pomodoroQuestions[WORK_TIME_ID])
                    wT.run { pomodoroSettings.workTimeInMinutes = this }
                    val sB = getAnswerOrDefault(pomodoroQuestions[SHORT_BREAK_ID])
                    sB.run { pomodoroSettings.shortBreakInMinutes = this }
                    val lB = getAnswerOrDefault(pomodoroQuestions[LONG_BREAK_ID])
                    lB.run { pomodoroSettings.longBreakInMinutes = this }
                }
                return true
            }
            else -> return true
        }
    }

    override fun getQuestionList(): ArrayList<Question<Double>?>
        = pomodoroQuestions.map { q -> q }.toCollection(ArrayList())
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
            = intent?.getLongExtra(TASK_ID_TO_REFRESH_FOR_REQUESTER, org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)
        Log.i(LOG_TAG, "got data $idToRefresh, is OK: ${resultCode == Activity.RESULT_OK}")
        return if (resultCode == Activity.RESULT_OK && idToRefresh != null)
            idToRefresh
        else
            org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG
    }
}

class TaskOngoing : AppCompatActivity() {

    companion object {
        const val LOG_TAG = "TaskOngoing"
        const val TASK_ID = "togtid"
        const val VIEW_STATE_ORDINAL = "togso"
        const val POMODORO_IS_ON = "togpom"
        const val TOTAL_TIME = "togtot"
        const val TOTAL_WORK_TIME = "togtow"
        const val TOTAL_BREAK_TIME = "togtob"
        const val CURRENT_TIME = "togct"
        const val IS_BREAK = "togib"
        const val BREAK_COUNT = "togbc"
        const val STARTED_ON = "togso"
        const val ENDED_ON = "togeo"

        const val TICK_RCV_INTENT_FILTER = "pomodorotasktick"
        const val END_TIME_MEASUREMENT_INTENT_FILTER = "pomodoropauseorworkend"
        const val ACTIVITY_VISIBLE_INTENT_FILTER = "activityvisible"
        const val START_TICKER_INTENT_FILTER = "starttimerintentfilter"
        const val SERVICE_READY_INTENT_FILTER = "timingserviceready"
    }

    private lateinit var lbm: LocalBroadcastManager

    private lateinit var viewModel: TaskOngoingViewModel

    private lateinit var databaseObject: TGDatabase

    private var taskId: Long = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG
    private var lastState: TaskOngoingViewModel.TaskOngoingStates
        = TaskOngoingViewModel.TaskOngoingStates.NOT_INITIALIZED

    private var recoveredPomodoroState : PomodoroState? = null
    private var isPomodoro = false

    private lateinit var tickBroadcastReceiver: BroadcastReceiver
    private lateinit var currentTimePassedReceiver: BroadcastReceiver
    private lateinit var timingServiceReadyReceiver: BroadcastReceiver
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

            putBoolean(POMODORO_IS_ON, viewModel.pomodoroIsOn)
            viewModel.startedDoingTaskOn?.run { putString(STARTED_ON, this.toString()) }
            viewModel.endedDoingTaskOn?.run { putString(ENDED_ON, this.toString()) }

            val pState = viewModel.pomodoroState
            runBlocking {
                putLong(TOTAL_TIME, pState.getTotalTime())
                putLong(TOTAL_WORK_TIME, pState.getTotalTimeOfWork())
                putLong(TOTAL_BREAK_TIME, pState.getTotalTimeOfBreaks())
                putLong(CURRENT_TIME, pState.getTimeOfCurrentState())
                putBoolean(IS_BREAK, pState.isBreak())
                putLong(BREAK_COUNT, pState.getCountOfBreaks())
            }
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
            val goalId = viewModel.mutableTaskData.value?.goalId ?: org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG
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
        val serviceIntent = TaskDoingTimingService.createIntent(
            this@TaskOngoing,
            getString(R.string.tasks_time_is_up),
            taskId,
            org.cezkor.towardsgoalsapp.Constants.ADDITIONAL_TIME.toLong()
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
        lbm.registerReceiver(
            timingServiceReadyReceiver,
            IntentFilter(SERVICE_READY_INTENT_FILTER)
        )
    }

    private fun unregisterReceivers() {
        lbm.unregisterReceiver(timingServiceReadyReceiver)
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
            taskId = this.getLong(TASK_ID, org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)
            val stateOrd = this.getInt(
                VIEW_STATE_ORDINAL,
                TaskOngoingViewModel.TaskOngoingStates.NOT_INITIALIZED.ordinal)
            lastState =
                TaskOngoingViewModel.TaskOngoingStates.entries[stateOrd]

            isPomodoro = getBoolean(POMODORO_IS_ON, isPomodoro)
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

            if (isPomodoro) {
                val totalTime = getLong(TOTAL_TIME, -1L)
                runBlocking {
                    if (totalTime != -1L && viewModel.pomodoroState.getTotalTime() < totalTime) {
                        val totalBreakTime = getLong(TOTAL_BREAK_TIME, 0)
                        val totalWorkTime = getLong(TOTAL_WORK_TIME, 0)
                        val curTime = getLong(CURRENT_TIME, 0)
                        val breakCount = getLong(BREAK_COUNT, 0)
                        val isBreak = getBoolean(IS_BREAK, false)
                        recoveredPomodoroState = PomodoroState(
                            totalWorkTime,
                            curTime,
                            totalTime,
                            totalBreakTime,
                            breakCount,
                            isBreak)
                        viewModel.pomodoroState = recoveredPomodoroState!!
                    }
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

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        lbm = LocalBroadcastManager.getInstance(this)

        var endInstant: Instant? = null
        var startInstant: Instant? = null

        fun recoverSavedState() {
            savedInstanceState?.run {
                taskId = this.getLong(TASK_ID, org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)
                val stateOrd = this.getInt(
                    VIEW_STATE_ORDINAL,
                    TaskOngoingViewModel.TaskOngoingStates.NOT_INITIALIZED.ordinal)
                lastState =
                    TaskOngoingViewModel.TaskOngoingStates.entries[stateOrd]

                isPomodoro = getBoolean(POMODORO_IS_ON, isPomodoro)
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

                if (isPomodoro) {
                    val totalTime = getLong(TOTAL_TIME, -1L)
                    if (totalTime != -1L) {
                        val totalBreakTime = getLong(TOTAL_BREAK_TIME, 0)
                        val totalWorkTime = getLong(TOTAL_WORK_TIME, 0)
                        val curTime = getLong(CURRENT_TIME, 0)
                        val breakCount = getLong(BREAK_COUNT, 0)
                        val isBreak = getBoolean(IS_BREAK, false)
                        recoveredPomodoroState = PomodoroState(
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
                org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)
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

            if (isPomodoro) {
                viewModel.pomodoroIsOn = true
                viewModel.pomodoroState = recoveredPomodoroState!!
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

            viewModel.pomodoroQuestions[TaskOngoingViewModel.WORK_TIME_ID]= RangedDoubleQuestion(
                getString(R.string.tasks_work_time_in_minutes),
                1.0,
                org.cezkor.towardsgoalsapp.Constants.MAX_WORK_TIME.toDouble(),
                org.cezkor.towardsgoalsapp.Constants.DEFAULT_WORK_TIME.toDouble()
            )
            viewModel.pomodoroQuestions[TaskOngoingViewModel.SHORT_BREAK_ID]= RangedDoubleQuestion(
                getString(R.string.tasks_short_break_in_minutes),
                1.0,
                org.cezkor.towardsgoalsapp.Constants.MAX_SHORT_BREAK_TIME.toDouble(),
                org.cezkor.towardsgoalsapp.Constants.DEFAULT_SHORT_BREAK_TIME.toDouble()
            )
            viewModel.pomodoroQuestions[TaskOngoingViewModel.LONG_BREAK_ID]= RangedDoubleQuestion(
                getString(R.string.tasks_long_break_in_minutes),
                1.0,
                org.cezkor.towardsgoalsapp.Constants.MAX_LONG_BREAK_TIME.toDouble(),
                org.cezkor.towardsgoalsapp.Constants.DEFAULT_LONG_BREAK_TIME.toDouble()
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

                        Log.i(LOG_TAG, "task pomodoro on: ${viewModel.pomodoroIsOn};" +
                                "times (work, short, long): " +
                                "${viewModel.pomodoroSettings.workTimeInMinutes}, " +
                        "${viewModel.pomodoroSettings.shortBreakInMinutes}," +
                                "${viewModel.pomodoroSettings.longBreakInMinutes}")

                        if (viewModel.startedDoingTaskOn == null)
                            viewModel.startedDoingTaskOn = Instant.now()

                        if (! serviceRun && viewModel.pomodoroIsOn)
                            createAndRunService()

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

            fun calculateShiftFor(tickCount: Long): Long {
                val lastTick = viewModel.lastTick.get()
                if (lastTick > tickCount) return 0L
                val shift = if (lastTick < tickCount) tickCount - lastTick else 0L
                viewModel.lastTick.set(tickCount)
                return shift
            }

            tickBroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    // by "ticks" it is implied they're seconds - OneSecondTicker
                    if (intent == null) return
                    if (intent.action != TICK_RCV_INTENT_FILTER) return
                    // one tick - roughly one second
                    val tickCount = intent.extras?.getLong(TaskDoingTimingService.TICK_COUNT) ?: 0

                    viewModel.pomodoroTimeUpdate.value =
                        OneTimeEventWithValue(calculateShiftFor(tickCount))
                }
            }

            currentTimePassedReceiver =  object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent == null) return
                    if (intent.action != END_TIME_MEASUREMENT_INTENT_FILTER) return
                    val finalTicks =
                        intent.extras?.getLong(TaskDoingTimingService.FINAL_TICK) ?: return
                    viewModel.pomodoroTimeUpdate.value =
                        OneTimeEventWithValue(calculateShiftFor(finalTicks))
                    viewModel.pomodoroStateChangeRequired.value = OneTimeEvent()
                }
            }

            timingServiceReadyReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent == null) return
                    if (intent.action != SERVICE_READY_INTENT_FILTER) return
                    viewModel.pomodoroIsReady.value = (viewModel.pomodoroIsOn)
                }
            }

            viewModel.pomodoroStartTicking.observe(this) {
                it?.handleIfNotHandledWith { lifecycleScope.launch {
                    val addDuration = viewModel.pomodoroTimerTickingForTheSameState.get()
                    val idx = viewModel.pomodoroState.determineIndexBasedOnState()
                    val textToDisplay : String
                    val duration: Long
                    when (idx) {
                        WORK_TIME_ID -> {
                            textToDisplay = getString(R.string.tasks_pomodoro_work_time)
                            duration = if (addDuration) org.cezkor.towardsgoalsapp.Constants.ADDITIONAL_TIME.toLong() else
                                viewModel.pomodoroSettings.workTimeInMinutes.toLong()
                        }
                        LONG_BREAK_ID -> {
                            textToDisplay = getString(R.string.tasks_pomodoro_long_break)
                            duration =
                                if (addDuration) org.cezkor.towardsgoalsapp.Constants.ADDITIONAL_TIME.toLong() else
                                    viewModel.pomodoroSettings.longBreakInMinutes.toLong()
                        }
                        else -> {
                            textToDisplay = getString(R.string.tasks_pomodoro_short_break)
                            duration =
                                if (addDuration) org.cezkor.towardsgoalsapp.Constants.ADDITIONAL_TIME.toLong() else
                                    viewModel.pomodoroSettings.shortBreakInMinutes.toLong()
                        }
                    }
                    Log.i(LOG_TAG, "text: $textToDisplay, duration $duration ")
                    lbm.sendBroadcast(
                        Intent(START_TICKER_INTENT_FILTER).apply {
                            putExtra(TaskDoingTimingService.DURATION_IN_MINUTES, duration)
                            putExtra(TaskDoingTimingService.TEXT_FOR_BRING_UP_NOTIFICATION,
                                textToDisplay)
                        }
                    )
                } }
            }
            registerReceivers()

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