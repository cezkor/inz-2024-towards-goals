package com.example.towardsgoalsapp.habits

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
import androidx.appcompat.widget.Toolbar
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
import com.example.towardsgoalsapp.habits.HabitInfoContract.Companion.HABIT_ID_FROM_REQUESTER
import com.example.towardsgoalsapp.impints.ImpIntItemList
import com.example.towardsgoalsapp.etc.OneTextFragment
import com.example.towardsgoalsapp.etc.TextsFragment
import com.example.towardsgoalsapp.reminders.ReminderSetting
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.example.towardsgoalsapp.database.*
import com.example.towardsgoalsapp.etc.SaveSuccessToastLauncher
import com.example.towardsgoalsapp.habits.HabitInfoContract.Companion.GOAL_ID_FROM_REQUESTER
import com.example.towardsgoalsapp.habits.HabitInfoContract.Companion.HABIT_ID_IF_REFRESH_FOR_REQUESTER
import com.example.towardsgoalsapp.habits.questioning.HabitQuestioningContract
import com.example.towardsgoalsapp.habits.questioning.HabitQuestioningLauncher
import com.example.towardsgoalsapp.main.App
import com.example.towardsgoalsapp.tasks.TaskDetails
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

typealias HabitInfoLauncher = ActivityResultLauncher<Triple<Long, Long, Boolean>>

class HabitInfoContract: ActivityResultContract<Triple<Long, Long, Boolean>, Long>() {

    companion object {
        const val HABIT_ID_FROM_REQUESTER = "hifr"
        const val GOAL_ID_FROM_REQUESTER = "higid"
        const val FOR_ADDING = "hiisfa"
        const val HABIT_ID_IF_REFRESH_FOR_REQUESTER = "rfr"
        const val LOG_TAG = "HIC"
    }
    override fun createIntent(context: Context, input: Triple<Long, Long, Boolean>): Intent {
        Log.i(LOG_TAG, "creating intent for $context, input $input")

        return Intent(context, HabitDetails::class.java).apply {
            action = Intent.ACTION_SEND
            putExtra(HABIT_ID_FROM_REQUESTER, input.first)
            putExtra(GOAL_ID_FROM_REQUESTER, input.second)
            putExtra(FOR_ADDING, input.third)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Long {
        val data = intent?.getLongExtra(HABIT_ID_IF_REFRESH_FOR_REQUESTER, Constants.IGNORE_ID_AS_LONG)
        Log.i(LOG_TAG, "got data $data, is OK: ${resultCode == Activity.RESULT_OK}")

        return if (resultCode == Activity.RESULT_OK && data != null)
            data
        else
            Constants.IGNORE_ID_AS_LONG
    }
}


class HabitDetails : AppCompatActivity() {

    companion object{
        const val LOG_TAG = "HabitDetails"

        const val LAST_TAB_ID = "hdlt"
        const val HABIT_ID = "hdhid"
        const val GOAL_ID = "hdgid"
        const val UNFINISHED_EDITING = "hdue"
        const val FOR_ADDING = HabitInfoContract.FOR_ADDING

        private const val TEXTS_TAB_ID = 0
        private const val TARGETS_TAB_ID = 1
        private const val IMP_INTS_TAB_ID = 2
        private const val REMINDER_TAB_ID = 3
        private const val TAB_COUNT = 4
    }

    private val classNumber = Constants.viewModelClassToNumber[HabitViewModel::class.java]

    private lateinit var viewModel: HabitViewModel
    private lateinit var databaseObject: TGDatabase

    private var habitId = Constants.IGNORE_ID_AS_LONG
    private var goalId = Constants.IGNORE_ID_AS_LONG

    private var isEdit: Boolean = false
    private var forAdding: Boolean = false
    private var isUnfinished: Boolean = false

    private var lastTabId: Int = TEXTS_TAB_ID
    private lateinit var tabsPager: ViewPager2

    private lateinit var habitQuestioningLauncher: HabitQuestioningLauncher

    private lateinit var doubleTapCallback: OnBackPressedCallback
    private lateinit var refreshRequesterCallback: OnBackPressedCallback

    private var canQuestionAboutThisHabit: Boolean = false

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.habit_detail_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (forAdding) {
            menu?.findItem(R.id.editHabitItem)?.title = getString(R.string.add_end_name)
            menu?.findItem(R.id.deleteHabitItem)?.title = getString(R.string.cancel)
        }
        else {
            if (! isEdit)
                menu?.findItem(R.id.editHabitItem)?.title = getString(R.string.enable_edit)
            else
                menu?.findItem(R.id.editHabitItem)?.title = getString(R.string.edit_end_name)
        }
        if (!canQuestionAboutThisHabit) {
            menu?.removeItem(R.id.habitMarkDoneItem)
        }
        return true
    }

    private fun recreatePagerAdapter() {
        tabsPager.adapter = HabitDetailsPageAdapter(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        fun carefullyAddThing(launchUnit: () -> Unit) {
            if (habitId == Constants.IGNORE_ID_AS_LONG) { lifecycleScope.launch {
                // can't add task/impint of goal if there is no goal -> add is as unfinished
                val saved = viewModel.saveMainDataAsUnfinished()
                if (saved && viewModel.mutableHabitData.value != null) {
                    habitId = viewModel.mutableHabitData.value!!.habitId
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
            R.id.editHabitItem -> {
                if (! forAdding) {
                    val curTab = tabsPager.currentItem
                    if (isEdit) {
                        isEdit = false
                        doubleTapCallback.isEnabled = false
                        refreshRequesterCallback.isEnabled = true
                        recreatePagerAdapter()
                        lifecycleScope.launch {
                            SaveSuccessToastLauncher
                                .launchToast(this@HabitDetails, viewModel.saveMainData())
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
                    recreatePagerAdapter()
                    if (viewModel.saveMainData())
                        runOnUiThread {
                            habitId = viewModel.mutableHabitData.value?.habitId ?: habitId
                            determineResultAndFinish()
                        }

                }
                if (canQuestionAboutThisHabit != ! isEdit) {
                    canQuestionAboutThisHabit = ! isEdit
                }
                invalidateMenu()
                true
            }
            R.id.habitMarkDoneItem -> {
                if (! isEdit) {
                    habitQuestioningLauncher.launch(habitId)
                } else {
                    Toast.makeText(this, getString(R.string.habits_dont_do_while_edit),
                        Toast.LENGTH_SHORT)
                }
                true
            }
            R.id.deleteHabitItem -> {
                if (forAdding) {
                    habitId = Constants.IGNORE_ID_AS_LONG;
                    if (viewModel.mutableHabitData.value != null) { lifecycleScope.launch {
                        viewModel.deleteWholeHabit()
                        runOnUiThread { determineResultAndFinish() }
                    } }
                    else {
                        runOnUiThread { determineResultAndFinish() }
                    }
                }
                else { lifecycleScope.launch {
                    viewModel.deleteWholeHabit()
                    runOnUiThread { determineResultAndFinish() }
                } }
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
            habitId = getLong(HABIT_ID, Constants.IGNORE_ID_AS_LONG)
            goalId = getLong(GOAL_ID, Constants.IGNORE_ID_AS_LONG)
            lastTabId = getInt(LAST_TAB_ID, TEXTS_TAB_ID)
            forAdding = getBoolean(FOR_ADDING, false)
        }

        super.onRestoreInstanceState(savedInstanceState, persistentState)
    }

    override fun onSaveInstanceState(outState: Bundle) {

        if (isEdit) runBlocking { viewModel.saveMainDataAsUnfinished() }

        outState.run {
            putInt(
                LAST_TAB_ID,
                findViewById<TabLayout>(R.id.habitTabs).id)
            putLong(HABIT_ID, habitId)
            putBoolean(UNFINISHED_EDITING, isEdit)
            putBoolean(FOR_ADDING, forAdding)
            putLong(HABIT_ID, habitId)
            putLong(GOAL_ID, goalId)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habit_details)

        val toolbar:  Toolbar= findViewById(R.id.habitToolbar)

        tabsPager = findViewById(R.id.habitDetailsViewPager)
        val tabs: TabLayout = findViewById(R.id.habitTabs)

        fun recoverSavedState() {
            savedInstanceState?.run {
                isUnfinished = getBoolean(UNFINISHED_EDITING, false)
                habitId = getLong(HABIT_ID, Constants.IGNORE_ID_AS_LONG)
                goalId = getLong(GOAL_ID, Constants.IGNORE_ID_AS_LONG)
                lastTabId = getInt(LAST_TAB_ID, TEXTS_TAB_ID)
                forAdding = getBoolean(FOR_ADDING, false)
            }
        }

        fun getArgs() {
            habitId = intent.getLongExtra(HABIT_ID_FROM_REQUESTER, habitId)
            goalId = intent.getLongExtra(GOAL_ID_FROM_REQUESTER, goalId)
            forAdding = intent.getBooleanExtra(FOR_ADDING, forAdding)
        }

        fun processArgsAndSavedState () {
            if (forAdding || isUnfinished) isEdit = true

            (application as App? )?.run {
                databaseObject = DatabaseObjectFactory.newDatabaseObject(this.driver)
            }
            viewModel = ViewModelProvider(this,
                HabitViewModelFactory(databaseObject, habitId, goalId)
            )[HabitViewModel::class.java]
        }

        fun prepareUI() {

            tabsPager.isUserInputEnabled = false

            tabsPager.adapter = HabitDetailsPageAdapter(this)

            TabLayoutMediator(tabs, tabsPager) {
                tab, position -> tab.text = when (position) {
                    TEXTS_TAB_ID -> getString(R.string.name_and_description)
                    REMINDER_TAB_ID -> getString(R.string.reminders_reminder)
                    IMP_INTS_TAB_ID -> getString(R.string.impints_name_plural)
                    TARGETS_TAB_ID -> getString(R.string.habits_targets)
                    else -> Constants.EMPTY_STRING
                }
            }.attach()

            tabsPager.currentItem = lastTabId

            setSupportActionBar(toolbar)

            viewModel.mutableHabitData.observe(this) {
                val name : String = viewModel.mutableHabitData.value?.habitName
                    ?: getString(R.string.habits_name)
                if (forAdding) {
                    toolbar.title = getString(R.string.adding, name)
                }
                else {
                    toolbar.title = name
                }

                if (canQuestionAboutThisHabit != ! isEdit) {
                    canQuestionAboutThisHabit = ! isEdit
                    invalidateMenu()
                }

                habitId = viewModel.mutableHabitData.value?.habitId
                    ?: Constants.IGNORE_ID_AS_LONG
            }

            if (forAdding) {
                canQuestionAboutThisHabit = false
                viewModel.targetNumbers.value = Pair(1, 1)
                invalidateOptionsMenu()
            }

            doubleTapCallback = DoubleTapOnBack(this, getString(R.string.abandoning_aoe)) {
                fun doWhenAlmostDone() {
                    habitId = Constants.IGNORE_ID_AS_LONG
                    onBackPressed()
                }

                if (forAdding) {
                    if (viewModel.mutableHabitData.value != null)
                    { lifecycleScope.launch {
                        viewModel.deleteWholeHabit()
                        doWhenAlmostDone()
                    } } else {  doWhenAlmostDone() }
                }
                else { lifecycleScope.launch {
                    viewModel.deleteAddedData()
                    doWhenAlmostDone()
                } }
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

            habitQuestioningLauncher = registerForActivityResult(HabitQuestioningContract()) {
                if (it != Constants.IGNORE_ID_AS_LONG) {
                    lifecycleScope.launch { viewModel.getEverything() }
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
        val result = if (habitId == Constants.IGNORE_ID_AS_LONG) RESULT_CANCELED else RESULT_OK
        setResult(result, Intent().apply {
            putExtra(HABIT_ID_IF_REFRESH_FOR_REQUESTER , habitId)
        })
    }

    private inner class HabitDetailsPageAdapter(fragAct: FragmentActivity):
        FragmentStateAdapter(fragAct) {
        override fun getItemCount(): Int = TAB_COUNT

        override fun createFragment(position: Int): Fragment {
            if (classNumber == null) return Fragment()

            return when (position) {
                TEXTS_TAB_ID ->
                    TextsFragment.newInstance(isEdit, classNumber)
                IMP_INTS_TAB_ID ->
                    if (viewModel.getImpIntsSharer().isArrayConsideredEmpty())
                        OneTextFragment.newInstance(getString(R.string.impints_no_impints))
                    else
                        ImpIntItemList.newInstance(
                            habitId,
                            OwnerType.TYPE_HABIT,
                            classNumber,
                            ! isEdit
                        )
                REMINDER_TAB_ID -> ReminderSetting.newInstance(OwnerType.TYPE_HABIT, habitId, isEdit)
                TARGETS_TAB_ID -> HabitTargets.newInstance(isEdit)
                else -> Fragment()
            }
        }



    }

}