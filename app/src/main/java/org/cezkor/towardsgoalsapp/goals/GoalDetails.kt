package org.cezkor.towardsgoalsapp.goals

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
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import org.cezkor.towardsgoalsapp.etc.DoubleTapOnBack
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.habits.HabitItemListFragment
import org.cezkor.towardsgoalsapp.etc.OneTextFragment
import org.cezkor.towardsgoalsapp.etc.TextsFragment
import org.cezkor.towardsgoalsapp.etc.TextsViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.cezkor.towardsgoalsapp.database.*
import org.cezkor.towardsgoalsapp.database.repositories.GoalRepository
import org.cezkor.towardsgoalsapp.database.repositories.HabitRepository
import org.cezkor.towardsgoalsapp.database.repositories.TaskRepository
import org.cezkor.towardsgoalsapp.etc.DescriptionFixer
import org.cezkor.towardsgoalsapp.database.userdata.HabitDataMutableArrayManager
import org.cezkor.towardsgoalsapp.database.userdata.MutablesArrayContentState
import org.cezkor.towardsgoalsapp.etc.NameFixer
import org.cezkor.towardsgoalsapp.database.userdata.OneModifyUserDataSharer
import org.cezkor.towardsgoalsapp.database.userdata.RecreatingGoalDataFactory
import org.cezkor.towardsgoalsapp.etc.errors.SaveSuccessToastLauncher
import org.cezkor.towardsgoalsapp.database.userdata.TaskDataMutableArrayManager
import org.cezkor.towardsgoalsapp.etc.TupleOfFour
import org.cezkor.towardsgoalsapp.database.userdata.ViewModelUserDataSharer
import org.cezkor.towardsgoalsapp.database.userdata.ViewModelWithHabitsSharer
import org.cezkor.towardsgoalsapp.database.userdata.ViewModelWithTasksSharer
import org.cezkor.towardsgoalsapp.etc.errors.ErrorHandling
import org.cezkor.towardsgoalsapp.habits.HabitInfoContract
import org.cezkor.towardsgoalsapp.habits.HabitInfoLauncher
import org.cezkor.towardsgoalsapp.main.App
import org.cezkor.towardsgoalsapp.etc.RefreshTypes
import org.cezkor.towardsgoalsapp.etc.ShouldRefreshUIBroadcastReceiver
import org.cezkor.towardsgoalsapp.reminders.ReminderService
import org.cezkor.towardsgoalsapp.tasks.details.MultipleTaskFragment
import org.cezkor.towardsgoalsapp.tasks.details.TaskInfoContract
import org.cezkor.towardsgoalsapp.tasks.details.TaskInfoLauncher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GoalViewModelFactory(
    private val dbo: TGDatabase,
    private val goalId: Long,
    private val pageNumber: Int
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GoalViewModel(dbo, goalId, pageNumber) as T
    }
}

class GoalViewModel(private val dbo: TGDatabase,
                    private var goalId: Long,
                    private var pageNumber: Int
): TextsViewModel(), ViewModelWithTasksSharer, ViewModelWithHabitsSharer {

    private val goalRepo = GoalRepository(dbo)
    private val taskRepo = TaskRepository(dbo)
    private val habitRepo = HabitRepository(dbo)

    val mutableGoalData = MutableLiveData<GoalData>(null)

    val arrayOfMutableHabitData: ArrayList<MutableLiveData<HabitData>> =
        java.util.ArrayList()

    private val habitsMLDManager: HabitDataMutableArrayManager
        = HabitDataMutableArrayManager(arrayOfMutableHabitData)

    private var addedHabitsSet: Set<Long> = setOf()

    val arrayOfMutableTaskData: ArrayList<MutableLiveData<TaskData>> =
        java.util.ArrayList()

    private val tasksMLDManager: TaskDataMutableArrayManager
            = TaskDataMutableArrayManager(arrayOfMutableTaskData)

    private var addedTasksSet: Set<Long> = setOf()

    private val getMutex = Mutex()

    val tasksSharer = object :
        OneModifyUserDataSharer<TaskData> {
        override fun getArrayOfUserData(): ArrayList<MutableLiveData<TaskData>>?
                = arrayOfMutableTaskData
        override fun signalNeedOfChangeFor(userDataId: Long) { viewModelScope.launch(exceptionHandler) {
            if (userDataId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) return@launch
            val manager = tasksMLDManager
            val list = getArrayOfUserData() ?: return@launch
            val hadSuchTask = manager.hasUserDataOfId(userDataId)
            val taskData = taskRepo.getOneById(userDataId)
            if (taskData == null) {
                // it was probably removed
                if (hadSuchTask)
                    manager.deleteOneOldUserDataById(userDataId)
            }
            else {
                if (! hadSuchTask) {
                    addedTasksSet = addedTasksSet.plus(userDataId)
                }
                manager.updateOneUserData(taskData)
            }
        } }
        override val arrayState: MutableLiveData<MutablesArrayContentState>
                = tasksMLDManager.contentState
        override val addedCount
                = tasksMLDManager.addedCount
        override fun isArrayConsideredEmpty(): Boolean {
            return arrayOfMutableTaskData.none { md -> md.value != null } ||
                    arrayOfMutableTaskData.isEmpty()
        }
    }

    val habitsSharer = object :
        OneModifyUserDataSharer<HabitData> {
        override fun getArrayOfUserData(): ArrayList<MutableLiveData<HabitData>>?
                = arrayOfMutableHabitData
        override fun signalNeedOfChangeFor(userDataId: Long) { viewModelScope.launch(exceptionHandler) {
            if (userDataId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) return@launch
            val manager = habitsMLDManager
            val list = getArrayOfUserData() ?: return@launch
            val hadSuchHabit = manager.hasUserDataOfId(userDataId)
            val habitData = habitRepo.getOneById(userDataId)
            if (habitData == null) {
                // it was probably removed
                if (hadSuchHabit)
                    manager.deleteOneOldUserDataById(userDataId)
            }
            else {
                if (! hadSuchHabit) {
                    addedHabitsSet = addedHabitsSet.plus(userDataId)
                }
                manager.updateOneUserData(habitData)
            }

        } }
        override val arrayState: MutableLiveData<MutablesArrayContentState>
                = habitsMLDManager.contentState
        override val addedCount
                = habitsMLDManager.addedCount

        override fun isArrayConsideredEmpty(): Boolean {
            return arrayOfMutableHabitData.none { md -> md.value != null } ||
                    arrayOfMutableHabitData.isEmpty()
        }
    }

    override fun getTasksSharer(): ViewModelUserDataSharer<TaskData>? = tasksSharer

    override fun getHabitsSharer(): ViewModelUserDataSharer<HabitData>? = habitsSharer

    suspend fun getEverything() = getMutex.withLock {
        mutableGoalData.value = goalRepo.getOneById(goalId)
        if (mutableGoalData.value == null) return@withLock
        if (pageNumber == org.cezkor.towardsgoalsapp.Constants.IGNORE_PAGE_AS_INT) {
            pageNumber = mutableGoalData.value!!.pageNumber
        }
        nameOfData.value = mutableGoalData.value?.goalName ?: nameOfData.value
        descriptionOfData.value =
            mutableGoalData.value?.goalDescription ?: descriptionOfData.value

        habitsMLDManager.setUserDataArray(
            habitRepo.getAllByGoalId(goalId)
        )
        tasksMLDManager.setUserDataArray(
            taskRepo.getAllByGoalId(goalId)
        )
    }

    override fun putTexts(newName: String?, newDescription: String?) {
        var name: String? = newName ?: mutableGoalData.value?.goalName
        var desc: String? = newDescription ?: mutableGoalData.value?.goalDescription
        if (name == null || desc == null) return
        name = NameFixer.fix(name)
        desc = DescriptionFixer.fix(desc)
        nameOfData.value = name
        descriptionOfData.value = desc
    }

    suspend fun deleteAddedData() {
        for (tid in addedTasksSet) taskRepo.deleteById(tid)
        for (hid in addedHabitsSet) habitRepo.deleteById(hid)
        addedHabitsSet = setOf()
        addedTasksSet = setOf()
    }

    suspend fun deleteWholeGoal() {
        goalRepo.deleteById(goalId)
    }

    private suspend fun addGoal() : Long {
        return goalRepo.addOneGoal(
            nameOfData.value ?: org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING,
            descriptionOfData.value ?: org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING,
            pageNumber
        )
    }

    suspend fun saveMainDataAsUnfinished() : Boolean  {
        if (pageNumber == org.cezkor.towardsgoalsapp.Constants.IGNORE_PAGE_AS_INT) return false
        if (goalId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) {

            val newGoalId = addGoal()
            if (newGoalId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) return false
            goalId = newGoalId
        }
        var goal: GoalData? = goalRepo.getOneById(goalId) ?: return false
        goalRepo.markEditing(goalId, true)
        val newUnfGoalData = RecreatingGoalDataFactory.createUserDataBasedOnTexts(
            goal!!, nameOfData.value, descriptionOfData.value
        )
        goalRepo.putAsUnfinished(newUnfGoalData)
        mutableGoalData.value = goalRepo.getOneById(goalId)

        addedAnyData = true
        return true
    }

    override suspend fun saveMainData() : Boolean {
        if (pageNumber == org.cezkor.towardsgoalsapp.Constants.IGNORE_PAGE_AS_INT) return false
        if (goalId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) {
            val newGoalId = addGoal()
            if (newGoalId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) return false
            goalId = newGoalId
        }
        else goalRepo.updateTexts(
            goalId,
            nameOfData.value ?: org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING,
            descriptionOfData.value ?: org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING
        )
        var goal: GoalData? = goalRepo.getOneById(goalId) ?: return false
        if (goal!!.goalEditUnfinished) {
            goalRepo.markEditing(goalId, false)
            goal = goalRepo.getOneById(goalId)
        }
        if (goal == null) return false
        mutableGoalData.value = goal!!
        addedAnyData = true
        return true
    }

    val isInPickingTasksOrHabitForStats: MutableLiveData<Boolean> = MutableLiveData(false)
    var dataPicked : Any? = null
}

typealias GoalRefreshRequesterResultLauncher = ActivityResultLauncher<Triple<Long, Boolean, Int?>>

class GoalRefreshRequesterContract: ActivityResultContract<Triple<Long, Boolean, Int?>, Long>() {

    companion object {
        const val GOAL_ID_FROM_REQUESTER = "gid1"
        const val FOR_ADDING = "gfa"
        const val PAGE_NUMBER = "gpn"
        const val GOAL_ID_TO_REQUESTER = "gid2"
        const val LOG_TAG = "GoalRRC"

    }
    override fun createIntent(context: Context, input: Triple<Long, Boolean, Int?>): Intent {
        Log.i(LOG_TAG, "creating intent for $context, input $input")

        return Intent(context, GoalDetails::class.java).apply {
            action = Intent.ACTION_SEND
            putExtra(GOAL_ID_FROM_REQUESTER, input.first)
            putExtra(FOR_ADDING, input.second)
            input.third?.run { putExtra(PAGE_NUMBER, this) }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Long {
        val data: Long? = intent?.getLongExtra(GOAL_ID_TO_REQUESTER, org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)
        Log.i(LOG_TAG, "got data $data, is OK: ${resultCode == Activity.RESULT_OK}")

        return if (resultCode == Activity.RESULT_OK) data ?: org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG
        else org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG
    }

}


class GoalDetails : AppCompatActivity() {

    companion object{
        const val LOG_TAG = "GoalDetails"

        const val LAST_TAB_IDX = "gdlt"
        const val PAGE_NUMBER = GoalRefreshRequesterContract.PAGE_NUMBER
        const val GOAL_ID = "gdgid"
        const val UNFINISHED_EDITING = "gdue"
        const val FOR_ADDING = GoalRefreshRequesterContract.FOR_ADDING
        private const val TEXTS_TAB_ID = 0
        private const val STATS_TAB_ID = 1
        private const val HABITS_TAB_ID = 2
        private const val TASKS_TAB_ID = 3

        fun getTabIdOfPosition(position: Int, forAdding: Boolean) : Int {
            return when(position) {
                0 -> TEXTS_TAB_ID
                1 -> if (forAdding) TASKS_TAB_ID else STATS_TAB_ID
                2 -> if (forAdding) HABITS_TAB_ID else TASKS_TAB_ID
                3 -> if (forAdding) -1 else HABITS_TAB_ID
                else -> -1
            }
        }

        fun getTabCount(forAdding: Boolean) = if (forAdding) 3 else 4
    }

    private val classNumber = org.cezkor.towardsgoalsapp.Constants.viewModelClassToNumber[GoalViewModel::class.java]

    private lateinit var viewModel: GoalViewModel
    private lateinit var tabsPager: ViewPager2
    private lateinit var tabLayoutMediator: TabLayoutMediator
    private lateinit var databaseObject: TGDatabase
    private lateinit var lbm: LocalBroadcastManager

    private var goalId = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG
    private var pageNumber: Int = org.cezkor.towardsgoalsapp.Constants.IGNORE_PAGE_AS_INT

    private var isEdit: Boolean = false
    private var forAdding: Boolean = false
    private var isUnfinished: Boolean = false

    private var lastTabPosition: Int = 0

    private lateinit var taskAddOrEditLauncher: TaskInfoLauncher
    private lateinit var habitAddOrEditLauncher: HabitInfoLauncher

    private lateinit var doubleTapCallback: OnBackPressedCallback
    private lateinit var refreshRequesterCallback: OnBackPressedCallback

    private var userAddedOrRemovedTaskAtLeastOnce = false
    private var userAddedOrRemovedHabitAtLeastOnce = false

    private var reloadUIBroadcastReceiver: BroadcastReceiver? = null

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.goal_detail_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (forAdding) {
            menu?.findItem(R.id.editGoalItem)?.title = getString(R.string.add_end_name)
            menu?.findItem(R.id.deleteGoalItem)?.title = getString(R.string.cancel)
        }
        else {
            if (! isEdit)
                menu?.findItem(R.id.editGoalItem)?.title = getString(R.string.enable_edit)
            else
                menu?.findItem(R.id.editGoalItem)?.title = getString(R.string.edit_end_name)
        }
        return true
    }

    private fun recreatePagerAdapter() {
        // this method is called whenever needed to recreate any of fragments
        // first we have to (re)create adapter
        tabsPager.adapter = GoalDetailsPageAdapter(this)
        // then reattach tablayout manager because of changed adapter (so tabs are reattached)
        tabLayoutMediator.detach()
        tabLayoutMediator.attach()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        fun determineResultAndFinish() {
            determineResultOnPlannedFinish()
            finish()
        }

        fun carefullyAddThing(launchUnit: () -> Unit) {
            if (goalId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) { lifecycleScope.launch(viewModel.exceptionHandler) {
                // can't add task/habit of goal if there is no goal -> add is as unfinished
                val saved = viewModel.saveMainDataAsUnfinished()
                if (saved && viewModel.mutableGoalData.value != null) {
                    goalId = viewModel.mutableGoalData.value!!.goalId
                    launchUnit()
                } }
            } else launchUnit()
        }

        return when (item.itemId) {
            R.id.editGoalItem -> {

                if (! forAdding) {
                    if (isEdit) {
                        isEdit = false
                        doubleTapCallback.isEnabled = true
                        refreshRequesterCallback.isEnabled = false
                        recreatePagerAdapter()

                        lifecycleScope.launch { SaveSuccessToastLauncher
                            .launchToast(this@GoalDetails, viewModel.saveMainData()) }
                    } else {
                        isEdit = true
                        doubleTapCallback.isEnabled = false
                        refreshRequesterCallback.isEnabled = true
                        recreatePagerAdapter()
                    }
                    invalidateMenu()
                }

                else lifecycleScope.launch(viewModel.exceptionHandler) {
                    isEdit = false
                    recreatePagerAdapter() // so user won't edit before saving
                    // also destroying fragments will force them to save data from their ui
                    // if they store it directly
                    // add and leave
                    if (viewModel.saveMainData())
                        runOnUiThread { determineResultAndFinish() }
                    else
                        ErrorHandling.showThrowableAsToast(
                            this@GoalDetails,
                                   Throwable(getString(R.string.adding_data_failed))
                            )
                }
                true
            }
            R.id.addHabitGoalItem -> {
                fun launchAdding() {
                    habitAddOrEditLauncher.launch(
                        Triple(org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG, goalId, true)
                    )
                }
                carefullyAddThing { launchAdding() }
                true
            }
            R.id.addTaskGoalItem -> {
                fun launchAdding() {
                    taskAddOrEditLauncher.launch(
                        TupleOfFour(
                            org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG, goalId,
                            org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG ,true)
                        // for adding without a task as owner
                    )
                }
                carefullyAddThing { launchAdding() }
                true
            }
            R.id.deleteGoalItem -> {
                if (forAdding) {
                    goalId = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG;
                    if (viewModel.mutableGoalData.value != null)
                    { lifecycleScope.launch(viewModel.exceptionHandler) {
                        viewModel.deleteWholeGoal()
                        runOnUiThread { determineResultAndFinish() }
                    } } else {
                        // didn't add, requester should ignore
                        runOnUiThread { determineResultAndFinish() }
                    }

                }
                else { lifecycleScope.launch(viewModel.exceptionHandler) {
                    viewModel.deleteWholeGoal()
                    runOnUiThread { determineResultAndFinish() }
                } }
                true
            }
            else -> false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {

        if (isEdit) runBlocking { viewModel.saveMainDataAsUnfinished() }

        outState.run {
            putInt(PAGE_NUMBER, pageNumber)
            putInt(LAST_TAB_IDX,
                tabsPager.currentItem)
            putLong(GOAL_ID, goalId)
            putBoolean(UNFINISHED_EDITING, isEdit)
            putBoolean(FOR_ADDING, forAdding)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?
    ) {
        savedInstanceState?.run {
            isUnfinished = getBoolean(UNFINISHED_EDITING, false)
            goalId = getLong(GOAL_ID, org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)
            lastTabPosition = getInt(LAST_TAB_IDX, 0)
            forAdding = getBoolean(FOR_ADDING, false)
            pageNumber = getInt(PAGE_NUMBER, org.cezkor.towardsgoalsapp.Constants.IGNORE_PAGE_AS_INT)
        }

        super.onRestoreInstanceState(savedInstanceState, persistentState)
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
        if (reloadUIBroadcastReceiver != null) lbm.unregisterReceiver(reloadUIBroadcastReceiver!!)
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_details)

        lbm = LocalBroadcastManager.getInstance(this)

        val toolbar: Toolbar = findViewById(R.id.goalToolbar)

        tabsPager = findViewById(R.id.goalDetailsViewPager)
        val tabs: TabLayout = findViewById(R.id.goalTabs)
        val tabBack: ImageButton = findViewById(R.id.tabBackButton)
        val tabNext: ImageButton = findViewById(R.id.tabNextButton)

        fun recoverSavedState() {
            savedInstanceState?.run {
                isUnfinished = getBoolean(UNFINISHED_EDITING, false)
                goalId = getLong(GOAL_ID, org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)
                lastTabPosition = getInt(LAST_TAB_IDX, 0)
                forAdding = getBoolean(FOR_ADDING, false)
                pageNumber = getInt(PAGE_NUMBER, org.cezkor.towardsgoalsapp.Constants.IGNORE_PAGE_AS_INT)
            }
        }

        fun getArgs() {
            val id = intent.getLongExtra(GoalRefreshRequesterContract.GOAL_ID_FROM_REQUESTER,
                goalId)
            goalId = id
            forAdding =
                intent.getBooleanExtra(GoalRefreshRequesterContract.FOR_ADDING, forAdding)
            pageNumber = intent.getIntExtra(GoalRefreshRequesterContract.PAGE_NUMBER, pageNumber)
        }

        fun processArgsAndSavedState() {
            if (forAdding || isUnfinished) isEdit = true

            (application as App? )?.run {
                databaseObject = DatabaseObjectFactory.newDatabaseObject(this.driver)
            }
            viewModel = ViewModelProvider(this,
                GoalViewModelFactory(databaseObject, goalId, pageNumber)
            )[GoalViewModel::class.java]

            reloadUIBroadcastReceiver = ShouldRefreshUIBroadcastReceiver(
                hashSetOf(Pair(goalId, RefreshTypes.GOAL))
            ) { lifecycleScope.launch { if (! forAdding) viewModel.getEverything() } }
            lbm.registerReceiver(reloadUIBroadcastReceiver!!, IntentFilter(
                ShouldRefreshUIBroadcastReceiver.INTENT_FILTER
            ))
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

            // sadly, mediator on attach() deletes all tabs defined in xml
            tabLayoutMediator = TabLayoutMediator(tabs, tabsPager) {
                tab, position -> tab.text = when (getTabIdOfPosition(position, forAdding)) {
                    TEXTS_TAB_ID -> getString(R.string.name_and_description)
                    TASKS_TAB_ID -> getString(R.string.tasks_name_plural)
                    HABITS_TAB_ID -> getString(R.string.habits_name_plural)
                    STATS_TAB_ID -> getString(R.string.stats_name)
                    else -> org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING
                }
            }
            recreatePagerAdapter()

            tabsPager.currentItem = lastTabPosition

            setSupportActionBar(toolbar)

            viewModel.mutableGoalData.observe(this) {
                val name = viewModel.mutableGoalData.value?.goalName
                    ?: getString(R.string.goals_name)
                if (forAdding) {
                    toolbar.title = getString(R.string.adding, name)
                }
                else {
                    toolbar.title = name
                }
                goalId = viewModel.mutableGoalData.value?.goalId
                    ?: org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG
            }

            if (forAdding) {
                invalidateMenu()
            }

            doubleTapCallback = DoubleTapOnBack(this, getString(R.string.abandoning_aoe)) {
                fun doWhenAlmostDone() {
                    goalId = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG
                    onBackPressed()
                }

                if (forAdding) {
                    if (viewModel.mutableGoalData.value != null)
                    { lifecycleScope.launch {
                        viewModel.deleteWholeGoal()
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

            taskAddOrEditLauncher = registerForActivityResult(TaskInfoContract()) {
                if (it != org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) {
                    if (!userAddedOrRemovedTaskAtLeastOnce)
                        userAddedOrRemovedTaskAtLeastOnce = true
                    val wasEmpty = viewModel.getTasksSharer()?.isArrayConsideredEmpty() == true
                    viewModel.tasksSharer.signalNeedOfChangeFor(it)
                    if (wasEmpty) recreatePagerAdapter()
                }
            }
            habitAddOrEditLauncher = registerForActivityResult(HabitInfoContract()) {
                if (it != org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) {
                    if (!userAddedOrRemovedHabitAtLeastOnce)
                        userAddedOrRemovedHabitAtLeastOnce = true
                    val wasEmpty = viewModel.getHabitsSharer()?.isArrayConsideredEmpty() == true
                    viewModel.habitsSharer.signalNeedOfChangeFor(it)
                    if (wasEmpty) recreatePagerAdapter()
                }
            }

            viewModel.getTasksSharer()?.arrayState?.observe(this) {
                if (it == MutablesArrayContentState.REPUTTED
                    && userAddedOrRemovedTaskAtLeastOnce
                    && viewModel.getTasksSharer()?.isArrayConsideredEmpty() == true) {
                    recreatePagerAdapter() // so user can see there is no task
                }
            }

            viewModel.getHabitsSharer()?.arrayState?.observe(this) {
                if (it == MutablesArrayContentState.REPUTTED
                    && userAddedOrRemovedHabitAtLeastOnce
                    && viewModel.getHabitsSharer()?.isArrayConsideredEmpty() == true) {
                    recreatePagerAdapter() // so user can see there is no habit
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
        val result = if (goalId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) RESULT_CANCELED else RESULT_OK
        setResult(result, Intent().apply {
            putExtra(GoalRefreshRequesterContract.GOAL_ID_TO_REQUESTER, goalId)
        })
    }

    private inner class GoalDetailsPageAdapter(fragAct: FragmentActivity):
        FragmentStateAdapter(fragAct) {

        override fun getItemCount(): Int = getTabCount(forAdding)

        override fun createFragment(position: Int): Fragment {
            if (classNumber == null) return Fragment()

            return when (getTabIdOfPosition(position, forAdding)) {
                TEXTS_TAB_ID -> TextsFragment.newInstance(isEdit, classNumber)
                TASKS_TAB_ID ->
                    if (viewModel.getTasksSharer()?.isArrayConsideredEmpty() == true)
                        OneTextFragment.newInstance(getString(R.string.tasks_no_tasks))
                    else
                        MultipleTaskFragment.newInstance(goalId, classNumber)
                HABITS_TAB_ID ->
                    if (viewModel.getHabitsSharer()?.isArrayConsideredEmpty() == true)
                        OneTextFragment.newInstance(getString(R.string.habits_no_habits))
                    else
                        HabitItemListFragment.newInstance(goalId, classNumber)
                STATS_TAB_ID -> GoalStatsFragment()
                else -> Fragment()
            }
        }



    }

}