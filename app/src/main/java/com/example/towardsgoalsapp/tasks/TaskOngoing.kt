package com.example.towardsgoalsapp.tasks

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.DoubleTapOnBack
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.tasks.TaskDoingContract.Companion.REFRESH_FOR_REQUESTER

class TaskOngoingViewModel(private val taskId: Long): TaskViewModel(taskId) {

    // stats mutables
    enum class TaskOnGoingStates {
        FIRST_QUESTIONS, DOING_TASK, SECOND_QUESTIONS, NOT_INITIALIZED
    }
    val mutableTaskOnGoingStates: MutableLiveData<TaskOnGoingStates>
        = MutableLiveData(TaskOnGoingStates.NOT_INITIALIZED)
    override fun updateOneTask() {}

    override fun updateTaskAsUnfinished() {}

    override fun getEverything(fromUnfinished: Boolean) {
        // should be able to put results of queries and doing task
        // i won't need to fetch subtasks so here i should avoid it
        // to do when database code is ready
    }
}

class TaskDoingContract: ActivityResultContract<Long, Boolean>() {

    companion object {
        const val TASK_ID_FROM_REQUESTER = "tdfr"
        const val REFRESH_FOR_REQUESTER = "rfr"
        const val LOG_TAG = "TDC"
    }
    override fun createIntent(context: Context, input: Long): Intent {
        Log.i(LOG_TAG, "creating intent for $context, input $input")

        return Intent(context, TaskDetails::class.java).apply {
            action = Intent.ACTION_SEND
            putExtra(TASK_ID_FROM_REQUESTER, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        val shouldRefresh: Boolean?
            = intent?.getBooleanExtra(REFRESH_FOR_REQUESTER, false)
        Log.i(LOG_TAG, "got data $shouldRefresh, is OK: ${resultCode == Activity.RESULT_OK}")
        return if (resultCode == Activity.RESULT_OK && shouldRefresh != null)
            shouldRefresh
        else
            false
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

    private var taskId: Long = Constants.IGNORE_ID_AS_LONG
    private var lastState: TaskOngoingViewModel.TaskOnGoingStates
        = TaskOngoingViewModel.TaskOnGoingStates.NOT_INITIALIZED

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.title_only_menu, menu)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            putLong(TASK_ID, taskId)
            val ordinal = viewModel.mutableTaskOnGoingStates.value?.ordinal ?:
                TaskOngoingViewModel.TaskOnGoingStates.NOT_INITIALIZED.ordinal
            putInt(STATE_ORDINAL, ordinal)
        }

        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        if (viewModel.mutableTaskOnGoingStates.value
            == TaskOngoingViewModel.TaskOnGoingStates.SECOND_QUESTIONS) {
            doAfterFullyDoingTask()
        }
        super.onStop()
    }

    private fun doAfterFullyDoingTask() {
        setResult(Activity.RESULT_OK, intent.apply {
            putExtra(REFRESH_FOR_REQUESTER, true)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_ongoing)

        fun recoverSavedState() {
            savedInstanceState?.run {
                taskId = this.getLong(TASK_ID, Constants.IGNORE_ID_AS_LONG)
                val stateOrd = this.getInt(STATE_ORDINAL,
                    TaskOngoingViewModel.TaskOnGoingStates.NOT_INITIALIZED.ordinal)
                lastState =
                    TaskOngoingViewModel.TaskOnGoingStates.entries[stateOrd]
            }
        }

        fun getArgs() {
            intent?.run {
                taskId = getLongExtra(TaskDoingContract.TASK_ID_FROM_REQUESTER,
                    Constants.IGNORE_ID_AS_LONG)
            }
        }

        fun processArgsAndSavedState() {
            viewModel = TaskOngoingViewModel(taskId)
            viewModel.mutableTaskOnGoingStates.value = lastState
        }


        fun prepareUI() {
            // title setting
            val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.taskToolbar)
            val taskName = viewModel.mutableTaskData.value?.taskName
                ?: R.string.tasks_name.toString()
            toolbar.title = getString(R.string.tasks_doing_task, taskName)
            setSupportActionBar(toolbar)
            viewModel.mutableTaskData.observe(this) {
                toolbar.title = getString(R.string.tasks_doing_task, it.taskName)
            }

            // orchestrating fragments
            val firstQFragment = TaskQuestionFirst()
            val firstQBackCallback
            = DoubleTapOnBack(this, getString(R.string.tasks_abandon_task)) {
                this@TaskOngoing.setResult(Activity.RESULT_CANCELED)
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

            val stateObserver = Observer<TaskOngoingViewModel.TaskOnGoingStates> {
                val fm = supportFragmentManager

                when (it) {
                    TaskOngoingViewModel.TaskOnGoingStates.FIRST_QUESTIONS -> {
                        fm.beginTransaction().apply {
                            setCustomAnimations(R.anim.layout_accelerator, R.anim.layout_deccelerator)
                            replace(R.id.taskStagesFrameLayout, firstQFragment)
                        }.commit()

                        firstQBackCallback.isEnabled = true
                        taskDoingBackCallback.isEnabled = false
                        secondQBackCallback.isEnabled = false
                        onBackPressedDispatcher.addCallback(firstQBackCallback)
                    }
                    TaskOngoingViewModel.TaskOnGoingStates.DOING_TASK -> {
                        fm.beginTransaction().apply {
                            setCustomAnimations(R.anim.layout_accelerator, R.anim.layout_deccelerator)
                            replace(R.id.taskStagesFrameLayout, taskDoingFragment)
                        }.commit()

                        firstQBackCallback.isEnabled = false
                        taskDoingBackCallback.isEnabled = true
                        secondQBackCallback.isEnabled = false
                        onBackPressedDispatcher.addCallback(taskDoingBackCallback)
                    }
                    TaskOngoingViewModel.TaskOnGoingStates.SECOND_QUESTIONS -> {
                        fm.beginTransaction().apply {
                            setCustomAnimations(R.anim.layout_accelerator, R.anim.layout_deccelerator)
                            replace(R.id.taskStagesFrameLayout, secondQFragment)
                        }.commit()

                        firstQBackCallback.isEnabled = false
                        taskDoingBackCallback.isEnabled = false
                        secondQBackCallback.isEnabled = true
                        onBackPressedDispatcher.addCallback(secondQBackCallback)
                    }
                    else -> {}
                }
            }
            viewModel.mutableTaskOnGoingStates.observe(this, stateObserver)
        }

        fun doInThisOrder() {

            recoverSavedState()

            getArgs()

            processArgsAndSavedState()

            prepareUI()

            viewModel.getEverything()

        }; doInThisOrder()

    }


}