package com.example.towardsgoalsapp.tasks.ongoing

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.etc.DoubleTapOnBack
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.database.DatabaseObjectFactory
import com.example.towardsgoalsapp.database.TGDatabase
import com.example.towardsgoalsapp.main.App
import com.example.towardsgoalsapp.tasks.TaskViewModel
import com.example.towardsgoalsapp.tasks.ongoing.TaskDoingContract.Companion.TASK_ID_TO_REFRESH_FOR_REQUESTER
import kotlinx.coroutines.launch

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
    TaskViewModel(dbo, taskId, Constants.IGNORE_ID_AS_LONG) {

    // stats mutables
    enum class TaskOngoingStates {
        FIRST_QUESTIONS, DOING_TASK, SECOND_QUESTIONS, FINISHED, NOT_INITIALIZED
    }
    val mutableOfTaskOngoingStates: MutableLiveData<TaskOngoingStates>
        = MutableLiveData(TaskOngoingStates.NOT_INITIALIZED)

    val taskFailed = MutableLiveData<Boolean>(false)

    override suspend fun getEverything() {
        // should be able to put results of queries and doing task
        // i won't need to fetch subtasks so here i should avoid it
    }

    override suspend fun saveMainData(): Boolean {
        when (mutableOfTaskOngoingStates.value) {
            TaskOngoingStates.FIRST_QUESTIONS -> {
                // put first questions in database
                return true
            }
            TaskOngoingStates.DOING_TASK -> {
                taskRepo.markTaskCompletion(taskId,taskFailed.value ?: false)
                return true
            }
            TaskOngoingStates.SECOND_QUESTIONS -> {
                // put last questions in database
                return true
            }
            else -> return true
        }
    }
}

typealias TaskDoingLauncher = ActivityResultLauncher<Long>

class TaskDoingContract: ActivityResultContract<Long, Long>() {

    companion object {
        const val TASK_ID_FROM_REQUESTER = "tdfr"
        const val TASK_ID_TO_REFRESH_FOR_REQUESTER = "tdtidtorfr"
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
        const val STATE_ORDINAL = "togso"
    }

    private lateinit var viewModel: TaskOngoingViewModel
    private val classNumber = Constants.viewModelClassToNumber[TaskOngoingViewModel::class.java]

    private lateinit var databaseObject: TGDatabase

    private var taskId: Long = Constants.IGNORE_ID_AS_LONG
    private var lastState: TaskOngoingViewModel.TaskOngoingStates
        = TaskOngoingViewModel.TaskOngoingStates.NOT_INITIALIZED

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.title_only_menu, menu)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            putLong(TASK_ID, taskId)
            val ordinal = viewModel.mutableOfTaskOngoingStates.value?.ordinal ?:
                TaskOngoingViewModel.TaskOngoingStates.NOT_INITIALIZED.ordinal
            putInt(STATE_ORDINAL, ordinal)
        }

        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (isFinishing && viewModel.mutableOfTaskOngoingStates.value
                == TaskOngoingViewModel.TaskOngoingStates.SECOND_QUESTIONS
        ) {
                doAfterFullyDoingTask()
            }
        super.onDestroy()
    }

    private fun doAfterFullyDoingTask() {
        setResult(Activity.RESULT_OK, intent.apply {
            putExtra(TASK_ID_TO_REFRESH_FOR_REQUESTER, taskId)
        })
    }

    override fun onRestoreInstanceState(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?
    ) {
        savedInstanceState?.run {
            taskId = this.getLong(TASK_ID, Constants.IGNORE_ID_AS_LONG)
            val stateOrd = this.getInt(
                STATE_ORDINAL,
                TaskOngoingViewModel.TaskOngoingStates.NOT_INITIALIZED.ordinal)
            lastState =
                TaskOngoingViewModel.TaskOngoingStates.entries[stateOrd]
        }

        super.onRestoreInstanceState(savedInstanceState, persistentState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_ongoing)

        fun recoverSavedState() {
            savedInstanceState?.run {
                taskId = this.getLong(TASK_ID, Constants.IGNORE_ID_AS_LONG)
                val stateOrd = this.getInt(
                    STATE_ORDINAL,
                    TaskOngoingViewModel.TaskOngoingStates.NOT_INITIALIZED.ordinal)
                lastState =
                    TaskOngoingViewModel.TaskOngoingStates.entries[stateOrd]
            }
        }

        fun getArgs() {
            intent?.run {
                taskId = getLongExtra(
                    TaskDoingContract.TASK_ID_FROM_REQUESTER,
                    Constants.IGNORE_ID_AS_LONG)
            }
        }

        fun processArgsAndSavedState() {

            (application as App? )?.run {
                databaseObject = DatabaseObjectFactory.newDatabaseObject(this.driver)
            }
            viewModel = ViewModelProvider(this,
                TaskOngoingViewModelFactory(databaseObject, taskId)
            )[TaskOngoingViewModel::class.java]
            viewModel.mutableOfTaskOngoingStates.value = lastState
        }


        fun prepareUI() {
            // title setting
            val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.taskToolbar)
            setSupportActionBar(toolbar)
            viewModel.mutableTaskData.observe(this) {
                val taskName = viewModel.mutableTaskData.value?.taskName
                    ?: getString(R.string.tasks_name)
                toolbar.title = getString(R.string.tasks_doing_task, taskName)
            }

            // orchestrating fragments
            val firstQFragment = TaskQuestionFirst()
            val firstQBackCallback
            = DoubleTapOnBack(this, getString(R.string.tasks_abandon_task)) {
                this@TaskOngoing.setResult(RESULT_CANCELED)
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
            val secondQFragment = TaskQuestionSecond()
            val secondQBackCallback =
                DoubleTapOnBack(this, getString(R.string.tasks_abandon_second_query)) {
                    this@TaskOngoing.doAfterFullyDoingTask()
                }
            firstQBackCallback.isEnabled = false
            secondQBackCallback.isEnabled = false
            taskDoingBackCallback.isEnabled = false

            val stateObserver = Observer<TaskOngoingViewModel.TaskOngoingStates> {
                val fm = supportFragmentManager

                when (it) {
                    TaskOngoingViewModel.TaskOngoingStates.FIRST_QUESTIONS -> {
                        fm.beginTransaction().apply {
                            setCustomAnimations(R.anim.layout_accelerator, R.anim.layout_deccelerator)
                            replace(R.id.taskStagesFrameLayout, firstQFragment)
                            setReorderingAllowed(true)
                        }.commit()

                        firstQBackCallback.isEnabled = true
                        taskDoingBackCallback.isEnabled = false
                        secondQBackCallback.isEnabled = false
                        onBackPressedDispatcher.addCallback(firstQBackCallback)
                    }
                    TaskOngoingViewModel.TaskOngoingStates.DOING_TASK -> {
                        fm.beginTransaction().apply {
                            setCustomAnimations(R.anim.layout_accelerator, R.anim.layout_deccelerator)
                            replace(R.id.taskStagesFrameLayout, taskDoingFragment)
                            setReorderingAllowed(true)
                        }.commit()

                        firstQBackCallback.isEnabled = false
                        taskDoingBackCallback.isEnabled = true
                        secondQBackCallback.isEnabled = false
                        onBackPressedDispatcher.addCallback(taskDoingBackCallback)
                    }
                    TaskOngoingViewModel.TaskOngoingStates.SECOND_QUESTIONS -> {
                        fm.beginTransaction().apply {
                            setCustomAnimations(R.anim.layout_accelerator, R.anim.layout_deccelerator)
                            replace(R.id.taskStagesFrameLayout, secondQFragment)
                            setReorderingAllowed(true)
                        }.commit()

                        firstQBackCallback.isEnabled = false
                        taskDoingBackCallback.isEnabled = false
                        secondQBackCallback.isEnabled = true
                        onBackPressedDispatcher.addCallback(secondQBackCallback)
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

        fun doInThisOrder() {

            recoverSavedState()

            getArgs()

            processArgsAndSavedState()

            prepareUI()

            lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.getEverything()

                viewModel.mutableOfTaskOngoingStates.value =
                    TaskOngoingViewModel.TaskOngoingStates.FIRST_QUESTIONS
            } }

        }; doInThisOrder()

    }


}