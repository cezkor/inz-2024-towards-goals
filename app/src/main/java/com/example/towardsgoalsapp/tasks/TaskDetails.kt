package com.example.towardsgoalsapp.tasks

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.etc.DoubleTapOnBack
import com.example.towardsgoalsapp.OwnerType
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.database.DatabaseObjectFactory
import com.example.towardsgoalsapp.database.TGDatabase
import com.example.towardsgoalsapp.database.userdata.MutablesArrayContentState
import com.example.towardsgoalsapp.impints.ImpIntItemList
import com.example.towardsgoalsapp.etc.OneTextFragment
import com.example.towardsgoalsapp.etc.SaveSuccessToastLauncher
import com.example.towardsgoalsapp.etc.TextsFragment
import com.example.towardsgoalsapp.etc.TupleOfFour
import com.example.towardsgoalsapp.main.App
import com.example.towardsgoalsapp.reminders.ReminderSetting
import com.example.towardsgoalsapp.tasks.TaskInfoContract.Companion.GOAL_ID_IF_FOR_ADDING
import com.example.towardsgoalsapp.tasks.TaskInfoContract.Companion.TASK_ID_FROM_REQUESTER
import com.example.towardsgoalsapp.tasks.TaskInfoContract.Companion.TASK_ID_IF_REFRESH_FOR_REQUESTER
import com.example.towardsgoalsapp.tasks.TaskInfoContract.Companion.TASK_OWNER_ID_FROM_REQUESTER
import com.example.towardsgoalsapp.tasks.ongoing.TaskDoingContract
import com.example.towardsgoalsapp.tasks.ongoing.TaskDoingLauncher
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

typealias TaskInfoLauncher = ActivityResultLauncher<TupleOfFour<Long, Long, Long, Boolean>>

class TaskInfoContract: ActivityResultContract<TupleOfFour<Long, Long, Long, Boolean>, Long>() {

    companion object {
        const val TASK_ID_FROM_REQUESTER = "tifr"
        const val TASK_OWNER_ID_FROM_REQUESTER = "toifr"
        const val GOAL_ID_IF_FOR_ADDING = "tigi"
        const val FOR_ADDING = "tiisfa"
        const val TASK_ID_IF_REFRESH_FOR_REQUESTER = "rfr"
        const val LOG_TAG = "TIC"
    }
    override fun createIntent(context: Context, input: TupleOfFour<Long, Long, Long, Boolean>): Intent {
        Log.i(LOG_TAG, "creating intent for $context, input $input")

        return Intent(context, TaskDetails::class.java).apply {
            action = Intent.ACTION_SEND
            putExtra(TASK_ID_FROM_REQUESTER, input.first)
            putExtra(GOAL_ID_IF_FOR_ADDING, input.second)
            putExtra(TASK_OWNER_ID_FROM_REQUESTER, input.third)
            putExtra(FOR_ADDING, input.fourth)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Long {
        val data = intent?.getLongExtra(TASK_ID_IF_REFRESH_FOR_REQUESTER, Constants.IGNORE_ID_AS_LONG)
        Log.i(LOG_TAG, "got data $data, is OK: ${resultCode == Activity.RESULT_OK}")

        return if (resultCode == Activity.RESULT_OK && data != null)
            data
        else
            Constants.IGNORE_ID_AS_LONG
    }
}


class TaskDetails : AppCompatActivity() {

    companion object{
        const val LOG_TAG = "TaskDetails"

        const val LAST_TAB_ID = "tdlt"
        const val TASK_ID = "tdtid"
        const val TASK_OWNER_ID = "tdtoid"
        const val GOAL_ID = "tgid"
        const val UNFINISHED_EDITING = "tdue"
        const val FOR_ADDING = TaskInfoContract.FOR_ADDING
        const val CAN_DO_TASK = "tdsndt"
        const val CAN_ADD_SUBTASK = "tdcas"

        private const val TASKS_TAB_ID = 1
        private const val TEXTS_TAB_ID = 0
        private const val IMP_INTS_TAB_ID = 3
        private const val REMINDER_TAB_ID = 2
        private const val TAB_COUNT = 4
    }

    private val classNumber = Constants.viewModelClassToNumber[TaskDetailsViewModel::class.java]

    private lateinit var viewModel: TaskDetailsViewModel

    private lateinit var databaseObject: TGDatabase

    private var taskId = Constants.IGNORE_ID_AS_LONG
    private var goalId = Constants.IGNORE_ID_AS_LONG
    private var taskOwnerId = Constants.IGNORE_ID_AS_LONG

    private var isEdit: Boolean = false
    private var forAdding: Boolean = false
    private var isUnfinished: Boolean = false
    private var canDoThisTask: Boolean = false
    private var canAddTask: Boolean = false

    private var lastTabId: Int = TEXTS_TAB_ID
    private lateinit var tabsPager: ViewPager2

    private lateinit var subtaskAddOrEditLauncher: TaskInfoLauncher
    private lateinit var taskDoingLauncher: TaskDoingLauncher

    private lateinit var doubleTapCallback: OnBackPressedCallback
    private lateinit var refreshRequesterCallback: OnBackPressedCallback

    private var userAddedOrDeletedSubtaskAtLeastOnce = false

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.task_detail_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (forAdding) {
            menu?.findItem(R.id.editTaskItem)?.title = getString(R.string.add_end_name)
            menu?.findItem(R.id.deleteTaskItem)?.title = getString(R.string.cancel)
        }
        else {
            if (! isEdit)
                menu?.findItem(R.id.editTaskItem)?.title = getString(R.string.enable_edit)
            else
                menu?.findItem(R.id.editTaskItem)?.title = getString(R.string.edit_end_name)
        }
        if (! canDoThisTask) menu?.removeItem(R.id.doTaskItem)
        if (! canAddTask) menu?.removeItem(R.id.addSubtaskItem)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        fun carefullyAddThing(launchUnit: () -> Unit) {
            if (taskId == Constants.IGNORE_ID_AS_LONG) { lifecycleScope.launch {
                // can't add task/impint of goal if there is no goal -> add is as unfinished
                val saved = viewModel.saveMainDataAsUnfinished()
                if (saved && viewModel.mutableTaskData.value != null) {
                    taskId = viewModel.mutableTaskData.value!!.taskId
                    launchUnit()
                } }
            } else launchUnit()
        }
        fun determineResultAndFinish() {
            determineResultOnPlannedFinish()
            finish()
        }

        return when (item.itemId) {
            R.id.addImpIntItem -> {
                if (! isEdit) {
                    isEdit = true
                    doubleTapCallback.isEnabled = true
                    refreshRequesterCallback.isEnabled = false
                    recreatePagerAdapter()
                    invalidateMenu()
                }
                tabsPager.currentItem = IMP_INTS_TAB_ID
                fun launchAdding() {
                    lifecycleScope.launch {
                        val wasEmpty = viewModel.getImpIntsSharer().isArrayConsideredEmpty()
                        viewModel.addOneNewEmptyImpInt()
                        if (wasEmpty) runOnUiThread {recreatePagerAdapter()}
                    }
                }
                carefullyAddThing { launchAdding() }
                true
            }
            R.id.doTaskItem -> {
                if (isEdit) {
                    Toast.makeText(this@TaskDetails,
                        getString(R.string.tasks_dont_do_while_edit),
                        Toast.LENGTH_SHORT).show()
                }
                else {
                    lifecycleScope.launch {
                        if (viewModel.getCurrentSubtasksCount() > 0) {
                            carefullyAddThing { taskDoingLauncher.launch(taskId) }
                        }
                        else Toast.makeText(this@TaskDetails,
                            getString(R.string.tasks_dont_do_because_has_subtasks),
                            Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
            R.id.addSubtaskItem -> {
                if (viewModel.getTaskDepth() + 1 < Constants.MAX_TASK_DEPTH ) {
                    fun launchAdding() {
                        subtaskAddOrEditLauncher.launch(
                            TupleOfFour(Constants.IGNORE_ID_AS_LONG, goalId,
                                taskId,true)
                            // for adding with a task owner
                        )
                    }
                    carefullyAddThing {
                        val wasEmpty
                            = viewModel.getTasksSharer()?.isArrayConsideredEmpty() ?: false
                        launchAdding()
                        if (wasEmpty) recreatePagerAdapter()
                    }
                }
                else Toast.makeText(this@TaskDetails,
                        getString(R.string.tasks_subtasks_not_allowed) ,
                    Toast.LENGTH_SHORT).show()
                true
            }
            R.id.editTaskItem -> {
                if (! forAdding) {
                    val curTab = tabsPager.currentItem
                    if (isEdit) {
                        isEdit = false
                        doubleTapCallback.isEnabled = false
                        refreshRequesterCallback.isEnabled = true
                        recreatePagerAdapter()
                        lifecycleScope.launch {
                            SaveSuccessToastLauncher
                                .launchToast(this@TaskDetails, viewModel.saveMainData())
                        }
                    } else {
                        isEdit = true
                        doubleTapCallback.isEnabled = true
                        refreshRequesterCallback.isEnabled = false
                        recreatePagerAdapter()
                    }
                    tabsPager.currentItem = curTab
                }
                else lifecycleScope.launch {
                    isEdit = false
                    runOnUiThread { recreatePagerAdapter() }
                    if (viewModel.saveMainData())
                        runOnUiThread {
                            taskId = viewModel.mutableTaskData.value?.taskId ?: taskId
                            determineResultAndFinish()
                        }
                }
                invalidateMenu()
                true
            }
            R.id.deleteTaskItem -> {
                if ( forAdding )  {
                    taskId = Constants.IGNORE_ID_AS_LONG
                    if (viewModel.mutableTaskData.value != null) {
                        lifecycleScope.launch {
                            viewModel.deleteWholeTask()
                            runOnUiThread { determineResultAndFinish() }
                        }
                    } else { determineResultAndFinish() }
                } else {
                    lifecycleScope.launch {
                        viewModel.deleteWholeTask()
                        runOnUiThread { determineResultAndFinish() }
                    }
                }
                true
            }
            else -> false
        }
    }

    override fun onRestoreInstanceState(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?
    ) {
        savedInstanceState?.run {
            isUnfinished = getBoolean(UNFINISHED_EDITING, false)
            taskId = getLong(TASK_ID, Constants.IGNORE_ID_AS_LONG)
            goalId = getLong(GOAL_ID, Constants.IGNORE_ID_AS_LONG)
            taskOwnerId = getLong(TASK_OWNER_ID, Constants.IGNORE_ID_AS_LONG)
            lastTabId = getInt(LAST_TAB_ID, TEXTS_TAB_ID)
            forAdding = getBoolean(FOR_ADDING, false)
            canAddTask = getBoolean(CAN_ADD_SUBTASK, false)
            canDoThisTask = getBoolean(CAN_DO_TASK, false)
        }

        super.onRestoreInstanceState(savedInstanceState, persistentState)
    }

    override fun onSaveInstanceState(outState: Bundle) {

        if (isEdit) runBlocking { viewModel.saveMainDataAsUnfinished() }

        outState.run {
            putInt(
                LAST_TAB_ID,
                findViewById<TabLayout>(R.id.taskTabs).id)
            putLong(GOAL_ID, goalId)
            putLong(TASK_ID, taskId)
            putLong(TASK_OWNER_ID, taskOwnerId)
            putBoolean(UNFINISHED_EDITING, isEdit)
            putBoolean(FOR_ADDING, forAdding)
            putBoolean(CAN_DO_TASK, canDoThisTask)
            putBoolean(CAN_ADD_SUBTASK, canAddTask)
        }
        super.onSaveInstanceState(outState)
    }

    private fun recreatePagerAdapter() {
        tabsPager.adapter = TaskDetailsPageAdapter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_details)

        val toolbar:  androidx.appcompat.widget.Toolbar = findViewById(R.id.taskToolbar)

        tabsPager = findViewById(R.id.taskDetailsViewPager)
        val tabs: TabLayout = findViewById(R.id.taskTabs)

        fun recoverSavedState() {
            savedInstanceState?.run {
                Log.i(LOG_TAG, "recovering from saved state")
                isUnfinished = getBoolean(UNFINISHED_EDITING, false)
                taskId = getLong(TASK_ID, Constants.IGNORE_ID_AS_LONG)
                goalId = getLong(GOAL_ID, Constants.IGNORE_ID_AS_LONG)
                taskOwnerId = getLong(TASK_OWNER_ID, Constants.IGNORE_ID_AS_LONG)
                lastTabId = getInt(LAST_TAB_ID, TEXTS_TAB_ID)
                forAdding = getBoolean(FOR_ADDING, false)
                canAddTask = getBoolean(CAN_ADD_SUBTASK, false)
                canDoThisTask = getBoolean(CAN_DO_TASK, false)
            }
        }

        fun getArgs() {
            taskId = intent.getLongExtra(TASK_ID_FROM_REQUESTER, taskId)
            forAdding = intent.getBooleanExtra(FOR_ADDING, forAdding)
            goalId = intent.getLongExtra(GOAL_ID_IF_FOR_ADDING, goalId)
            taskOwnerId = intent.getLongExtra(TASK_OWNER_ID_FROM_REQUESTER, taskOwnerId)
            Log.i(LOG_TAG,
                "taskid $taskId taskowner $taskOwnerId goalid $goalId"
                        + "for addding $forAdding")
        }

        fun processArgsAndSavedState () {
            if (forAdding || isUnfinished) isEdit = true

            (application as App? )?.run {
                databaseObject = DatabaseObjectFactory.newDatabaseObject(this.driver)
            }
            viewModel = ViewModelProvider(this,
                TaskDetailsViewModelFactory(databaseObject, taskId, goalId, taskOwnerId)
            )[TaskDetailsViewModel::class.java]
        }

        fun prepareUI() {

            tabsPager.isUserInputEnabled = false

            recreatePagerAdapter()
            // sadly, mediator on attach() deletes all tabs defined in xml
            TabLayoutMediator(tabs, tabsPager) {
                tab, position -> tab.text = when (position) {
                    TASKS_TAB_ID -> getString(R.string.tasks_name_plural)
                    TEXTS_TAB_ID -> getString(R.string.name_and_description)
                    REMINDER_TAB_ID -> getString(R.string.reminders_reminder)
                    IMP_INTS_TAB_ID -> getString(R.string.impints_name_plural)
                    else -> Constants.EMPTY_STRING
                }
            }.attach()

            tabsPager.currentItem = lastTabId

            setSupportActionBar(toolbar)

            viewModel.mutableTaskData.observe(this) {
                val name = viewModel.mutableTaskData.value?.taskName
                    ?: getString(R.string.tasks_name)
                if (forAdding) {
                    toolbar.title = getString(R.string.adding, name)
                }
                else {
                    toolbar.title = name
                }
                if (it == null) return@observe
                canAddTask = ( it.taskDepth + 1 < Constants.MAX_TASK_DEPTH )
                canDoThisTask = (it.subtasksCount == 0L && !forAdding)
                taskId = it?.taskId ?: taskId

                invalidateMenu()
            }

            if (forAdding) {
                canAddTask = true
                canDoThisTask = false
                invalidateMenu()
            }

            doubleTapCallback = DoubleTapOnBack( this, getString(R.string.abandoning_aoe)) {
                fun doWhenAlmostDone() {
                    taskId = Constants.IGNORE_ID_AS_LONG
                    determineResultOnPlannedFinish()
                    onBackPressed()
                }

                if (( forAdding && viewModel.mutableTaskData.value != null) )  {
                    lifecycleScope.launch {
                        viewModel.deleteWholeTask()
                        runOnUiThread { doWhenAlmostDone() }
                    }
                } else {
                    if (forAdding)
                        doWhenAlmostDone()
                    else {
                        lifecycleScope.launch {
                            viewModel.deleteAddedData()
                            runOnUiThread { doWhenAlmostDone() }
                        }
                    }
                }

            }

            refreshRequesterCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    determineResultOnPlannedFinish()
                    finish()
                }

            }
            onBackPressedDispatcher.addCallback(doubleTapCallback)
            onBackPressedDispatcher.addCallback(refreshRequesterCallback)

            if (isEdit) {
                doubleTapCallback.isEnabled = true
                refreshRequesterCallback.isEnabled = false
            }
            else {
                doubleTapCallback.isEnabled = false
                refreshRequesterCallback.isEnabled = true
            }

            subtaskAddOrEditLauncher = registerForActivityResult(TaskInfoContract()) {
                Log.i(LOG_TAG, "aoe subtask id: $it")
                if (it != Constants.IGNORE_ID_AS_LONG) {
                    if (!userAddedOrDeletedSubtaskAtLeastOnce)
                        userAddedOrDeletedSubtaskAtLeastOnce = true
                    val wasEmpty =
                        viewModel.getTasksSharer()?.isArrayConsideredEmpty() ?: false
                    viewModel.getTasksSharer()?.signalNeedOfChangeFor(it)
                    val isNowEmpty =
                        viewModel.getTasksSharer()?.isArrayConsideredEmpty() ?: false
                    if (wasEmpty || isNowEmpty) recreatePagerAdapter()
                }
            }
            taskDoingLauncher = registerForActivityResult(TaskDoingContract()) {
                if (it != Constants.IGNORE_ID_AS_LONG) {
                    lifecycleScope.launch { viewModel.getEverything() }
                }
            }

            viewModel.getTasksSharer()?.arrayState?.observe(this) {
                if (it == MutablesArrayContentState.REPUTTED
                    && viewModel.getTasksSharer()?.isArrayConsideredEmpty() == true
                    && userAddedOrDeletedSubtaskAtLeastOnce
                ) {
                    recreatePagerAdapter() // so user will see that there is no tasks
                }
            }
     
        }

        fun doInThisOrder() {

            recoverSavedState()

            getArgs()

            processArgsAndSavedState()

            prepareUI()

            lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.getEverything()
            } }

        }; doInThisOrder()


    }

    private fun determineResultOnPlannedFinish() {
        val result = if (taskId == Constants.IGNORE_ID_AS_LONG) RESULT_CANCELED else RESULT_OK
        setResult(result, Intent().apply {
            putExtra(TASK_ID_IF_REFRESH_FOR_REQUESTER, taskId)
        })
    }

    private inner class TaskDetailsPageAdapter(fragAct: FragmentActivity):
        FragmentStateAdapter(fragAct) {

        fun determineSubtaskLength() : Long =
            viewModel.getTasksSharer()?.getArrayOfUserData()
                    ?.filter { md -> md.value != null }?.size?.toLong() ?: 0L

        override fun getItemCount(): Int = TAB_COUNT

        override fun createFragment(position: Int): Fragment {
            if (classNumber == null) return Fragment()
            val subtasksCount: Long = determineSubtaskLength()
            return when (position) {
                TASKS_TAB_ID ->
                    if (viewModel.getTaskDepth() + 1 < Constants.MAX_TASK_DEPTH)
                        if (subtasksCount == 0L)
                            OneTextFragment.newInstance(getString(R.string.tasks_no_tasks))
                        else
                            TaskItemList.newInstance(taskId, classNumber)
                    else
                        OneTextFragment.newInstance(getString(R.string.tasks_subtasks_not_allowed))
                TEXTS_TAB_ID ->
                    TextsFragment.newInstance(isEdit, classNumber)
                IMP_INTS_TAB_ID ->
                    if (viewModel.getImpIntsSharer().isArrayConsideredEmpty())
                        OneTextFragment.newInstance(getString(R.string.impints_no_impints))
                    else
                        ImpIntItemList.newInstance(
                            taskId,
                            OwnerType.TYPE_TASK,
                            classNumber,
                            ! isEdit
                        )
                REMINDER_TAB_ID -> ReminderSetting.newInstance(OwnerType.TYPE_TASK, taskId, isEdit)
                else -> Fragment()
            }
        }
    }

}