package com.example.towardsgoalsapp.tasks

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.etc.DoubleTapOnBack
import com.example.towardsgoalsapp.OwnerType
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.impints.IntImpItemList
import com.example.towardsgoalsapp.etc.OneTextFragment
import com.example.towardsgoalsapp.etc.TextsFragment
import com.example.towardsgoalsapp.reminders.ReminderSetting
import com.example.towardsgoalsapp.tasks.TaskInfoContract.Companion.TASK_ID_FROM_REQUESTER
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class TaskDetailsViewModel(private val taskId: Long): TaskViewModel(taskId) {

    fun getTaskDepth(taskId: Long): Int { return 1 }

    override fun updateOneTask() {}

}

class TaskInfoContract: ActivityResultContract<Pair<Long, Boolean>, Boolean>() {

    companion object {
        const val TASK_ID_FROM_REQUESTER = "tifr"
        const val FOR_ADDING = "tiisfa"
        const val REFRESH_FOR_REQUESTER = "rfr"
        const val LOG_TAG = "TIC"
    }
    override fun createIntent(context: Context, input: Pair<Long, Boolean>): Intent {
        Log.i(LOG_TAG, "creating intent for $context, input $input")

        return Intent(context, TaskDetails::class.java).apply {
            action = Intent.ACTION_SEND
            putExtra(TASK_ID_FROM_REQUESTER, input.first)
            putExtra(FOR_ADDING, input.second)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        val data = intent?.getBooleanExtra(REFRESH_FOR_REQUESTER, false)
        Log.i(LOG_TAG, "got data $data, is OK: ${resultCode == Activity.RESULT_OK}")

        return if (resultCode == Activity.RESULT_OK && data != null)
            data
        else
            false
    }
}


class TaskDetails : AppCompatActivity() {

    companion object{
        const val LOG_TAG = "TaskDetails"

        const val LAST_TAB_ID = "hdlt"
        const val TASK_ID = "hdgid"
        const val UNFINISHED_EDITING = "hdue"
        const val FOR_ADDING = TaskInfoContract.FOR_ADDING

        private const val TASKS_TAB_ID = 1
        private const val TEXTS_TAB_ID = 0
        private const val IMP_INTS_TAB_ID = 3
        private const val REMINDER_TAB_ID = 2
        private const val TAB_COUNT = 4
    }

    private val classNumber = Constants.viewModelClassToNumber[TaskDetailsViewModel::class.java]

    private lateinit var viewModel: TaskDetailsViewModel

    private var taskId = Constants.IGNORE_ID_AS_LONG

    private var isEdit: Boolean = false
    private var forAdding: Boolean = false
    private var isUnfinished: Boolean = false

    private var lastTabId: Int = TEXTS_TAB_ID
    private var menu: Menu? = null
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.task_detail_menu, menu)
        this.menu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.addImpIntItem -> {
                true
            }
            R.id.taskDoneItem -> {
                true
            }
            R.id.editItem -> {
                if (! forAdding)
                    if (isEdit) { isEdit = false; item.title = getString(R.string.edit_end_name) }
                    else { isEdit = true; item.title = getString(R.string.enable_edit) }
                else {
                    // end adding
                    finish()
                }
                true
            }
            R.id.deleteTaskItem -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_details)

        val toolbar:  androidx.appcompat.widget.Toolbar = findViewById(R.id.taskToolbar)

        val tabsPager: ViewPager2 = findViewById(R.id.taskDetailsViewPager)
        val tabs: TabLayout = findViewById(R.id.taskTabs)

        fun recoverSavedState() {
            savedInstanceState?.run {
                isUnfinished = getBoolean(UNFINISHED_EDITING, false)
                taskId = getLong(TASK_ID, Constants.IGNORE_ID_AS_LONG)
                lastTabId = getInt(LAST_TAB_ID, TEXTS_TAB_ID)
                forAdding = getBoolean(FOR_ADDING, false)
            }
        }

        fun getArgs() {
            taskId = intent.getLongExtra(TASK_ID_FROM_REQUESTER, taskId)
            forAdding = intent.getBooleanExtra(FOR_ADDING, forAdding)
        }

        fun processArgsAndSavedState () {
            if (forAdding || isUnfinished) isEdit = true
            viewModel = TaskDetailsViewModel(taskId)
        }

        fun prepareUI() {

            tabsPager.isUserInputEnabled = false

            tabsPager.adapter = TaskDetailsPageAdapter(this)
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

            toolbar.title = viewModel.mutableTaskData.value?.taskName
                ?: R.string.tasks_name.toString()
            setSupportActionBar(toolbar)

            viewModel.mutableTaskData.observe(this) {
                toolbar.title = it.taskName
            }

            if (forAdding) {
                menu?.getItem(R.id.editItem)?.title = getString(R.string.add_end_name)
            }
            onBackPressedDispatcher.addCallback(
                DoubleTapOnBack( this, getString(R.string.abandoning_aoe)) {
                    if (isEdit) setResult(RESULT_CANCELED)
                    onBackPressed()
                }
            )
        }

        fun doInThisOrder() {

            recoverSavedState()

            getArgs()

            processArgsAndSavedState()

            prepareUI()

            viewModel.getEverything()

        }; doInThisOrder()


    }

    override fun onStop() {
        val result = RESULT_CANCELED
        // determine if there is need for refreshing
        val refresh = false
        setResult(result, Intent().apply {
            putExtra(TaskInfoContract.REFRESH_FOR_REQUESTER, refresh)
        })
        super.onStop()
    }

    private inner class TaskDetailsPageAdapter(fragAct: FragmentActivity):
        FragmentStateAdapter(fragAct) {
        override fun getItemCount(): Int = TAB_COUNT

        override fun createFragment(position: Int): Fragment {
            if (classNumber == null) return Fragment()

            return when (position) {
                TASKS_TAB_ID ->
                    if (viewModel.getTaskDepth(taskId) < Constants.MAX_TASK_DEPTH)
                        if (viewModel.arrayOfMutableSubtasksTaskData.isEmpty())
                            OneTextFragment.newInstance(getString(R.string.tasks_no_tasks))
                        else
                            TaskItemList.newInstance(taskId, classNumber)
                    else
                        OneTextFragment.newInstance(getString(R.string.tasks_subtasks_not_allowed))
                TEXTS_TAB_ID ->
                    TextsFragment.newInstance(isEdit)
                IMP_INTS_TAB_ID ->
                    if (viewModel.arrayOfMutableImpIntData.isEmpty())
                        OneTextFragment.newInstance(getString(R.string.impints_no_impints))
                    else
                        IntImpItemList.newInstance(
                            taskId,
                            OwnerType.TYPE_TASK,
                            classNumber,
                            isEdit
                        )
                REMINDER_TAB_ID -> ReminderSetting.newInstance(OwnerType.TYPE_TASK, taskId, isEdit)
                else -> Fragment()
            }
        }



    }

}