package com.example.towardsgoalsapp.goals

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.etc.DoubleTapOnBack
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.habits.HabitData_OLD
import com.example.towardsgoalsapp.habits.HabitItemList
import com.example.towardsgoalsapp.etc.OneTextFragment
import com.example.towardsgoalsapp.etc.TextsFragment
import com.example.towardsgoalsapp.etc.TextsViewModel
import com.example.towardsgoalsapp.tasks.TaskData_OLD
import com.example.towardsgoalsapp.tasks.TaskItemList
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class GoalViewModel(private val goalId: Long): TextsViewModel() {

    val mutableGoalData = MutableLiveData<GoalData_OLD>()

    val arrayOfMutableHabitData: ArrayList<MutableLiveData<HabitData_OLD>> =
        java.util.ArrayList()

    val arrayOfMutableTaskData: ArrayList<MutableLiveData<TaskData_OLD>> =
        java.util.ArrayList()

    private fun calculateProgress(): Double {return .0} // to do when database code is ready

    fun updateGoalTexts(
        newName: String?,
        newDescription: String?
    ) {
        // validation should within AddOrEditGoal
        // update in database

        val old = mutableGoalData.value
        mutableGoalData.value = GoalData_OLD(
            old?.goalId ?: Constants.IGNORE_ID_AS_LONG,
            newName?: old?.goalName ?: Constants.EMPTY_STRING,
            newDescription?: old?.goalDescription ?: Constants.EMPTY_STRING,
            old?.progress ?: calculateProgress()
        )
    }

    fun updateOneTask() {}

    fun updateOneHabit() {}

    fun getEverything(fromUnfinished: Boolean = false) {}
    override fun updateTexts(newName: String?, newDescription: String?) {
        TODO("Not yet implemented")
    }

}

class GoalRefreshRequesterContract: ActivityResultContract<Pair<Long, Boolean>, Long>() {

    companion object {
        const val GOAL_ID_FROM_REQUESTER = "gid1"
        const val FOR_ADDING = "gfa"
        const val GOAL_ID_TO_REQUESTER = "gid2"
        const val LOG_TAG = "GoalRRC"
    }
    override fun createIntent(context: Context, input: Pair<Long, Boolean>): Intent {
        Log.i(LOG_TAG, "creating intent for $context, input $input")

        return Intent(context, GoalDetails::class.java).apply {
            action = Intent.ACTION_SEND
            putExtra(GOAL_ID_FROM_REQUESTER, input.first)
            putExtra(FOR_ADDING, input.second)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Long {
        val data: Long? = intent?.getLongExtra(GOAL_ID_TO_REQUESTER, Constants.IGNORE_ID_AS_LONG)
        Log.i(LOG_TAG, "got data $data, is OK: ${resultCode == Activity.RESULT_OK}")

        return if (resultCode == Activity.RESULT_OK) data ?: Constants.IGNORE_ID_AS_LONG
        else Constants.IGNORE_ID_AS_LONG
    }

}


class GoalDetails : AppCompatActivity() {

    companion object{
        const val LOG_TAG = "GoalDetails"

        const val LAST_TAB_ID = "gdlt"
        const val GOAL_ID = "gdgid"
        const val UNFINISHED_EDITING = "gdue"
        const val FOR_ADDING = GoalRefreshRequesterContract.FOR_ADDING

        private const val TEXTS_TAB_ID = 0
        private const val TASKS_TAB_ID = 2
        private const val HABITS_TAB_ID = 3
        private const val STATS_TAB_ID = 1
        private const val TAB_COUNT = 3
    }

    private val classNumber = Constants.viewModelClassToNumber[GoalViewModel::class.java]

    private lateinit var viewModel: GoalViewModel

    private var goalId = Constants.IGNORE_ID_AS_LONG

    private var isEdit: Boolean = false
    private var forAdding: Boolean = false
    private var isUnfinished: Boolean = false

    private var menu: Menu? = null
    private var lastTabId: Int = TEXTS_TAB_ID

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.goal_detail_menu, menu)
        this.menu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.editGoalItem -> {
                if (! forAdding)
                    if (isEdit) { isEdit = false; item.title = getString(R.string.edit_end_name) }
                    else { isEdit = true; item.title = getString(R.string.enable_edit) }
                else {
                    // end adding
                    finish()
                }
                true
            }
            R.id.addHabitGoalItem -> {
                true
            }
            R.id.addTaskGoalItem -> {
                true
            }
            R.id.deleteGoalItem -> {
                // change text when it is adding
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {

        outState.run {
            putInt(LAST_TAB_ID,
                findViewById<TabLayout>(R.id.goalTabs).id)
            putLong(GOAL_ID, goalId)
            putBoolean(UNFINISHED_EDITING, isEdit)
            putBoolean(FOR_ADDING, forAdding)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_details)

        val toolbar: Toolbar = findViewById(R.id.goalToolbar)

        val tabsPager: ViewPager2 = findViewById(R.id.goalDetailsViewPager)
        val tabs: TabLayout = findViewById(R.id.goalTabs)

        fun recoverSavedState() {
            savedInstanceState?.run {
                isUnfinished = getBoolean(UNFINISHED_EDITING, false)
                goalId = getLong(GOAL_ID, Constants.IGNORE_ID_AS_LONG)
                lastTabId = getInt(LAST_TAB_ID, TEXTS_TAB_ID)
                forAdding = getBoolean(FOR_ADDING, false)
            }
        }

        fun getArgs() {
            val id = intent.getLongExtra(GoalRefreshRequesterContract.GOAL_ID_FROM_REQUESTER,
                goalId)
            goalId = id
            forAdding =
                intent.getBooleanExtra(GoalRefreshRequesterContract.FOR_ADDING, forAdding)
        }

        fun processArgsAndSavedState() {
            if (forAdding || isUnfinished) isEdit = true

            viewModel = GoalViewModel(goalId)
        }

        fun prepareUI() {

            tabsPager.isUserInputEnabled = false

            tabsPager.adapter = GoalDetailsPageAdapter(this)
            // sadly, mediator on attach() deletes all tabs defined in xml
            TabLayoutMediator(tabs, tabsPager) {
                tab, position -> tab.text = when (position) {
                    TEXTS_TAB_ID -> getString(R.string.name_and_description)
                    TASKS_TAB_ID -> getString(R.string.tasks_name_plural)
                    HABITS_TAB_ID -> getString(R.string.habits_name_plural)
                    STATS_TAB_ID -> getString(R.string.stats_name)
                    else -> Constants.EMPTY_STRING
                }
            }.attach()

            tabsPager.currentItem = lastTabId

            toolbar.title = viewModel.mutableGoalData.value?.goalName
                ?: R.string.default_title.toString()
            setSupportActionBar(toolbar)

            viewModel.mutableGoalData.observe(this) {
                toolbar.title = it.goalName
            }

            if (forAdding) {
                menu?.getItem(R.id.editItem)?.title = getString(R.string.add_end_name)
            }

            onBackPressedDispatcher.addCallback(
                DoubleTapOnBack(this, getString(R.string.abandoning_aoe)) {
                    setResult(RESULT_CANCELED)
                    onBackPressed()
                }
            )
        }

        fun doInThisOrder() {

            recoverSavedState()

            getArgs()

            processArgsAndSavedState()

            prepareUI()

            viewModel.getEverything(isUnfinished)

        }; doInThisOrder()


    }

    override fun onStop() {
        val result = if (goalId == Constants.IGNORE_ID_AS_LONG) RESULT_CANCELED else RESULT_OK
        setResult(result, Intent().apply {
            putExtra(GoalRefreshRequesterContract.GOAL_ID_TO_REQUESTER, goalId)
        })
        super.onStop()
    }

    private inner class GoalDetailsPageAdapter(fragAct: FragmentActivity):
        FragmentStateAdapter(fragAct) {
        override fun getItemCount(): Int = TAB_COUNT

        override fun createFragment(position: Int): Fragment {
            if (classNumber == null) return Fragment()

            return when (position) {
                TEXTS_TAB_ID -> TextsFragment.newInstance(isEdit)
                TASKS_TAB_ID ->
                    if (viewModel.arrayOfMutableTaskData.isEmpty())
                        OneTextFragment.newInstance(getString(R.string.tasks_no_tasks))
                    else
                        TaskItemList.newInstance(goalId, classNumber)
                HABITS_TAB_ID ->
                    if (viewModel.arrayOfMutableHabitData.isEmpty())
                        OneTextFragment.newInstance(getString(R.string.habits_no_habits))
                    else
                        HabitItemList.newInstance(goalId, classNumber)
                STATS_TAB_ID -> Fragment() // todo: make stats fragment
                else -> Fragment()
            }
        }



    }

}