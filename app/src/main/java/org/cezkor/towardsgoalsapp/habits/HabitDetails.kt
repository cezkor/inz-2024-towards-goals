package org.cezkor.towardsgoalsapp.habits

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import org.cezkor.towardsgoalsapp.etc.DoubleTapOnBack
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.habits.HabitInfoContract.Companion.HABIT_ID_FROM_REQUESTER
import org.cezkor.towardsgoalsapp.impints.ImpIntItemList
import org.cezkor.towardsgoalsapp.etc.OneTextFragment
import org.cezkor.towardsgoalsapp.etc.TextsFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.cezkor.towardsgoalsapp.database.*
import org.cezkor.towardsgoalsapp.etc.OneTimeHandleable
import org.cezkor.towardsgoalsapp.etc.errors.ErrorHandling
import org.cezkor.towardsgoalsapp.etc.errors.SaveSuccessToastLauncher
import org.cezkor.towardsgoalsapp.habits.HabitInfoContract.Companion.GOAL_ID_FROM_REQUESTER
import org.cezkor.towardsgoalsapp.habits.HabitInfoContract.Companion.HABIT_ID_IF_REFRESH_FOR_REQUESTER
import org.cezkor.towardsgoalsapp.habits.params.HabitTargets
import org.cezkor.towardsgoalsapp.habits.questioning.HabitQuestioningContract
import org.cezkor.towardsgoalsapp.habits.questioning.HabitQuestioningLauncher
import org.cezkor.towardsgoalsapp.main.App
import org.cezkor.towardsgoalsapp.habits.params.HabitStatsFragment
import org.cezkor.towardsgoalsapp.etc.RefreshTypes
import org.cezkor.towardsgoalsapp.etc.ShouldRefreshUIBroadcastReceiver
import org.cezkor.towardsgoalsapp.reminders.ReminderService
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
        val data = intent?.getLongExtra(HABIT_ID_IF_REFRESH_FOR_REQUESTER, org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)
        Log.i(LOG_TAG, "got data $data, is OK: ${resultCode == Activity.RESULT_OK}")

        return if (resultCode == Activity.RESULT_OK && data != null)
            data
        else
            org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG
    }
}


class HabitDetails : AppCompatActivity() {

    companion object{
        const val LOG_TAG = "HabitDetails"

        const val LAST_TAB_IDX = "hdlt"
        const val HABIT_ID = "hdhid"
        const val GOAL_ID = "hdgid"
        const val UNFINISHED_EDITING = "hdue"
        const val FOR_ADDING = HabitInfoContract.FOR_ADDING

        private const val TEXTS_TAB_ID = 0
        private const val TARGETS_TAB_ID = 1
        private const val IMP_INTS_TAB_ID = 2
        private const val REMINDER_TAB_ID = 3
        private const val STATS_TAB_ID = 4

        fun getTabIdOfPosition(position: Int, forAdding: Boolean) : Int {
            return when(position) {
                0 -> TEXTS_TAB_ID
                1 -> TARGETS_TAB_ID
                2 -> if (! forAdding) STATS_TAB_ID else REMINDER_TAB_ID
                3 -> if (! forAdding) REMINDER_TAB_ID else IMP_INTS_TAB_ID
                4 -> if (! forAdding) IMP_INTS_TAB_ID else -1
                else -> -1
            }
        }

        fun getTabCount(forAdding: Boolean) : Int = if (forAdding) 4 else 5

    }

    private val classNumber = org.cezkor.towardsgoalsapp.Constants.viewModelClassToNumber[HabitViewModel::class.java]

    private lateinit var viewModel: HabitViewModel
    private lateinit var databaseObject: TGDatabase
    private lateinit var lbm: LocalBroadcastManager

    private var habitId = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG
    private var goalId = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG

    private var isEdit: Boolean = false
    private var forAdding: Boolean = false
    private var isUnfinished: Boolean = false
    private var isMarkable: Boolean = false

    private var lastTabIdx: Int = 0
    private lateinit var tabsPager: ViewPager2
    private lateinit var tabLayoutMediator: TabLayoutMediator

    private lateinit var habitQuestioningLauncher: HabitQuestioningLauncher

    private lateinit var doubleTapCallback: OnBackPressedCallback
    private lateinit var refreshRequesterCallback: OnBackPressedCallback

    private var canQuestionAboutThisHabit: Boolean = false
    private var reloadUIBroadcastReceiver: BroadcastReceiver? = null

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
        if ( ! canQuestionAboutThisHabit || isEdit || ! isMarkable) {
            menu?.removeItem(R.id.habitMarkItem)
        }
        return true
    }

    private fun recreatePagerAdapter() {
        tabsPager.adapter = HabitDetailsPageAdapter(this)
        tabLayoutMediator.detach()
        tabLayoutMediator.attach()
    }

    private fun addHabitIfNotPresentAndThenDoUnit(launchUnit: () -> Unit) {
        if (habitId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) {
            lifecycleScope.launch(viewModel.exceptionHandler) {
            // can't add task/impint of goal if there is no goal -> add is as unfinished
            val saved = viewModel.saveMainDataAsUnfinished()
            if (saved && viewModel.mutableHabitData.value != null) {
                habitId = viewModel.mutableHabitData.value!!.habitId
                launchUnit()
            } }
        } else launchUnit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

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
                // set tab idx to that of imp ints
                tabsPager.currentItem = when(getTabIdOfPosition(4, forAdding)){
                    IMP_INTS_TAB_ID -> 4
                    else -> 3 // assume impints tab is on position 3
                }
                fun launchAdding() {
                    lifecycleScope.launch(viewModel.exceptionHandler) {
                        val wasEmpty = viewModel.getImpIntsSharer().isArrayConsideredEmpty()
                        viewModel.addOneNewEmptyImpInt()
                        if (wasEmpty) runOnUiThread {recreatePagerAdapter()}
                    }
                }
                viewModel.assureOfExistingHabitHandleableMutable
                    .value = OneTimeHandleable { launchAdding() }
                true
            }
            R.id.editHabitItem -> {
                if (! forAdding) {
                    val curTabIdx = tabsPager.currentItem
                    if (isEdit) {
                        isEdit = false
                        doubleTapCallback.isEnabled = false
                        refreshRequesterCallback.isEnabled = true
                        recreatePagerAdapter()
                        lifecycleScope.launch(viewModel.exceptionHandler) {
                            SaveSuccessToastLauncher
                                .launchToast(this@HabitDetails, viewModel.saveMainData())
                        }
                    } else {
                        isEdit = true
                        doubleTapCallback.isEnabled = true
                        refreshRequesterCallback.isEnabled = false
                        recreatePagerAdapter()
                    }
                    tabsPager.currentItem = curTabIdx
                }
                else lifecycleScope.launch(viewModel.exceptionHandler) {
                    isEdit = false
                    recreatePagerAdapter()
                    if (viewModel.saveMainData())
                        runOnUiThread {
                            habitId = viewModel.mutableHabitData.value?.habitId ?: habitId
                            determineResultAndFinish()
                        }
                    else ErrorHandling.showThrowableAsToast(
                        this@HabitDetails,
                        Throwable(getString(R.string.failed_to_save_data))
                    )
                }
                if (canQuestionAboutThisHabit != ! isEdit) {
                    canQuestionAboutThisHabit = ! isEdit
                }
                invalidateMenu()
                true
            }
            R.id.habitMarkItem -> {
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
                    habitId = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG;
                    if (viewModel.mutableHabitData.value != null) {
                        lifecycleScope.launch(viewModel.exceptionHandler) {
                        viewModel.deleteWholeHabit()
                        runOnUiThread { determineResultAndFinish() }
                    } }
                    else {
                        runOnUiThread { determineResultAndFinish() }
                    }
                }
                else { lifecycleScope.launch(viewModel.exceptionHandler) {
                    viewModel.deleteWholeHabit()
                    runOnUiThread { determineResultAndFinish() }
                } }
                true
            }
            else -> false
        }
    }

    override fun onStart() {
        super.onStart()
        lbm.sendBroadcast(
            Intent(ReminderService.EDIT_ONGOING_INTENT_FILTER)
                .putExtra(ReminderService.BLOCK_NOTIFICATIONS, true)
        )
    }

    override fun onDestroy() {
        lbm.sendBroadcast(
            Intent(ReminderService.EDIT_ONGOING_INTENT_FILTER)
                .putExtra(ReminderService.BLOCK_NOTIFICATIONS, false)
        )
        reloadUIBroadcastReceiver?.run { lbm.unregisterReceiver(this) }
        super.onDestroy()
    }

    override fun onRestoreInstanceState(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?
    ) {
        savedInstanceState?.run {
            isUnfinished = getBoolean(UNFINISHED_EDITING, false)
            habitId = getLong(HABIT_ID, org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)
            goalId = getLong(GOAL_ID, org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)
            lastTabIdx = getInt(LAST_TAB_IDX, 0)
            forAdding = getBoolean(FOR_ADDING, false)
        }

        super.onRestoreInstanceState(savedInstanceState, persistentState)
    }

    override fun onSaveInstanceState(outState: Bundle) {

        if (isEdit) runBlocking { viewModel.saveMainDataAsUnfinished() }

        outState.run {
            putInt(
                LAST_TAB_IDX,
                tabsPager.currentItem)
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

        lbm = LocalBroadcastManager.getInstance(this)

        val toolbar:  Toolbar= findViewById(R.id.habitToolbar)

        tabsPager = findViewById(R.id.habitDetailsViewPager)
        val tabs: TabLayout = findViewById(R.id.habitTabs)
        val tabBack: ImageButton = findViewById(R.id.tabBackButton)
        val tabNext: ImageButton = findViewById(R.id.tabNextButton)

        fun recoverSavedState() {
            savedInstanceState?.run {
                isUnfinished = getBoolean(UNFINISHED_EDITING, false)
                habitId = getLong(HABIT_ID, org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)
                goalId = getLong(GOAL_ID, org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)
                lastTabIdx = getInt(LAST_TAB_IDX, 0)
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

            reloadUIBroadcastReceiver = ShouldRefreshUIBroadcastReceiver(
                hashSetOf(Pair(habitId, RefreshTypes.HABIT))
            ) { lifecycleScope.launch { if (! forAdding) viewModel.getEverything() } }
            lbm.registerReceiver(reloadUIBroadcastReceiver!!, IntentFilter(
                ShouldRefreshUIBroadcastReceiver.INTENT_FILTER) )
        }

        fun prepareUI() {

            viewModel.exceptionMutable.observe(this) {
                ErrorHandling.showExceptionDialog(this, it)
            }

            tabs.tabMode = TabLayout.MODE_SCROLLABLE
            tabs.tabGravity = TabLayout.GRAVITY_FILL

            tabBack.setOnClickListener {
                var curIdx = tabs.selectedTabPosition
                if (curIdx == -1) return@setOnClickListener
                if (curIdx - 1 < 0) return@setOnClickListener
                if (tabs.tabCount < 1 || curIdx -1 >= tabs.tabCount)
                    return@setOnClickListener
                tabs.selectTab(tabs.getTabAt(curIdx - 1))
            }
            tabNext.setOnClickListener {
                var curIdx = tabs.selectedTabPosition
                if (curIdx == -1) return@setOnClickListener
                if (curIdx + 1 >= tabs.tabCount) return@setOnClickListener
                if (tabs.tabCount < 1) return@setOnClickListener
                tabs.selectTab(tabs.getTabAt(curIdx + 1))
            }

            tabsPager.isUserInputEnabled = false

            tabsPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    viewModel.currentTabIdx = position
                }
            })
            viewModel.setTabEvent.observe(this) {
                it?.handleIfNotHandledWith { tabsPager.currentItem = viewModel.currentTabIdx }
            }

            tabLayoutMediator = TabLayoutMediator(tabs, tabsPager) {
                tab, position -> tab.text = when (getTabIdOfPosition(position, forAdding)) {
                    TEXTS_TAB_ID -> getString(R.string.name_and_description)
                    REMINDER_TAB_ID -> getString(R.string.reminders_reminder)
                    IMP_INTS_TAB_ID -> getString(R.string.impints_name_plural)
                    TARGETS_TAB_ID -> getString(R.string.habits_targets)
                    STATS_TAB_ID -> getString(R.string.stats_name)
                    else -> org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING
                }
            }
            recreatePagerAdapter()

            tabsPager.currentItem = lastTabIdx

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

                isMarkable = if (! forAdding)
                                 HabitLogic.checkIfHabitIsMarkable(it?.habitLastMarkedOn)
                            else false
                if (canQuestionAboutThisHabit != ! isEdit) {
                    canQuestionAboutThisHabit = ! isEdit
                }

                invalidateMenu()

                habitId = viewModel.mutableHabitData.value?.habitId
                    ?: org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG
            }

            if (forAdding) {
                canQuestionAboutThisHabit = false
                viewModel.targetNumbers.value = Pair(1, 1)
                invalidateOptionsMenu()
            }

            doubleTapCallback = DoubleTapOnBack(this, getString(R.string.abandoning_aoe)) {
                fun doWhenAlmostDone() {
                    habitId = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG
                    onBackPressed()
                }

                if (forAdding) {
                    if (viewModel.mutableHabitData.value != null)
                    { lifecycleScope.launch(viewModel.exceptionHandler) {
                        viewModel.deleteWholeHabit()
                        doWhenAlmostDone()
                    } } else {  doWhenAlmostDone() }
                }
                else { lifecycleScope.launch(viewModel.exceptionHandler) {
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

            habitQuestioningLauncher = registerForActivityResult(HabitQuestioningContract()) {
                if (it != org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) {
                    lifecycleScope.launch(viewModel.exceptionHandler) {
                        viewModel.getEverything()
                        recreatePagerAdapter()
                    }
                }
            }

            viewModel.assureOfExistingHabitHandleableMutable.observe(this) {
                if (it != null && ! it.handled) {
                    addHabitIfNotPresentAndThenDoUnit { it.handle() }
                }
            }

            if (isEdit) {
                doubleTapCallback.isEnabled = true
                refreshRequesterCallback.isEnabled = false
            }
            else {
                doubleTapCallback.isEnabled = false
                refreshRequesterCallback.isEnabled = true
            }

            viewModel.canShowHabitStatsAtAll.observe(this) {
                recreatePagerAdapter()
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
        val result = if (habitId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) RESULT_CANCELED else RESULT_OK
        setResult(result, Intent().apply {
            putExtra(HABIT_ID_IF_REFRESH_FOR_REQUESTER , habitId)
        })
    }

    private inner class HabitDetailsPageAdapter(fragAct: FragmentActivity):
        FragmentStateAdapter(fragAct) {
        override fun getItemCount(): Int = getTabCount(forAdding)

        override fun createFragment(position: Int): Fragment {
            if (classNumber == null) return Fragment()

            return when (getTabIdOfPosition(position, forAdding)) {
                TEXTS_TAB_ID ->
                    TextsFragment.newInstance(isEdit, classNumber)
                IMP_INTS_TAB_ID ->
                    if (viewModel.getImpIntsSharer().isArrayConsideredEmpty())
                        OneTextFragment.newInstance(getString(R.string.impints_no_impints))
                    else
                        ImpIntItemList.newInstance(
                            habitId,
                            org.cezkor.towardsgoalsapp.OwnerType.TYPE_HABIT,
                            classNumber,
                            ! isEdit
                        )
                REMINDER_TAB_ID -> HabitReminderSetting.newInstance(isEdit)
                STATS_TAB_ID -> if (viewModel.canShowHabitStatsAtAll.value == true)
                                    HabitStatsFragment.newInstance(habitId)
                                else OneTextFragment
                                    .newInstance(getString(R.string.habits_no_data))
                TARGETS_TAB_ID -> HabitTargets.newInstance(isEdit)

                else -> Fragment()
            }
        }



    }

}