package com.example.towardsgoalsapp.goals

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.habits.HabitData
import com.example.towardsgoalsapp.habits.HabitItemList
import com.example.towardsgoalsapp.main.MainActivity
import com.example.towardsgoalsapp.main.OneTextFragment
import com.example.towardsgoalsapp.tasks.TaskData
import com.example.towardsgoalsapp.tasks.TaskItemList
import com.google.android.material.tabs.TabLayout

class GoalSynopsisViewModel: ViewModel() {

    // meanings of enum entries in order:
    // not ready to show
    // readied when activity view created: with goal data, without it
    // data changed (in case of page with GoalSynopsis)
    // populated with GoalData (to make GoalSynopsis page)
    // populated with AddGoalSuggestion
    enum class MutableGoalDataStates {
        NOT_READY, INITIALIZED_POPULATED, INITIALIZED_EMPTY , REFRESHED, POPULATED, EMPTIED
    }

    val arrayOfGoalData: ArrayList<MutableLiveData<GoalData?>> =
        List(Constants.MAX_GOALS_AMOUNT) { MutableLiveData<GoalData?>() }.toCollection(ArrayList())

    val habitDataArraysPerGoal: HashMap<Long,ArrayList<MutableLiveData<HabitData>>> =
        HashMap(Constants.MAX_GOALS_AMOUNT)

    val taskDataArraysPerGoal: HashMap<Long,ArrayList<MutableLiveData<TaskData>>> =
        HashMap(Constants.MAX_GOALS_AMOUNT)

    val arrayOfGoalDataStates: Array<MutableLiveData<MutableGoalDataStates>> =
        Array(Constants.MAX_GOALS_AMOUNT) {MutableLiveData(MutableGoalDataStates.NOT_READY)}

    val gidToPosition = HashMap<Long, Int>()

    val lastTabIndexes: Array<MutableLiveData<Int>> =
        Array(Constants.MAX_GOALS_AMOUNT) { MutableLiveData(Constants.IGNORE_ID_AS_INT) }
    fun getEverything() {

        for (i in setOf(0,1,2,3,4,5,6)) {
            arrayOfGoalDataStates[i].value = MutableGoalDataStates.INITIALIZED_EMPTY
        }

        arrayOfGoalData[3].value = GoalData(
            100, "a goal", "super description!", 0.1
        )
        taskDataArraysPerGoal[100] = java.util.ArrayList(
            List(25) { MutableLiveData<TaskData>() }.toCollection(ArrayList())
        )
        for (i in 0..<25) taskDataArraysPerGoal[100]?.get(i).apply {
            this?.run { this.value = TaskData(
                (100+i).toLong(), "task $i", "descr",
                0.0, -1, false, 1000
            ) }
        }
        habitDataArraysPerGoal[100] = java.util.ArrayList(
            List(5) { MutableLiveData<HabitData>() }.toCollection(ArrayList())
        )
        for (i in 0..<5) habitDataArraysPerGoal[100]?.get(i).apply {
            this?.run { this.value = HabitData((200+i).toLong(), "habit $i") }
        }

        arrayOfGoalDataStates[3].value = MutableGoalDataStates.INITIALIZED_POPULATED

    }

    fun updateOneGoal() {}

    fun addOneGoal(
        newName: String?,
        newDescription: String?
    ) {
        // add new goal via repository to database
        // get it to view model
    }
}

class GoalSynopsis: Fragment() {

    private lateinit var goalDetailsOpener: ActivityResultLauncher<Pair<Long, Boolean>>

    companion object {
        const val LOG_TAG = "GoalSynopsis"

        @JvmStatic
        fun newInstance(pageNumber: Int) =
            GoalSynopsis().apply {
                arguments = Bundle().apply {
                    putInt(MainActivity.PAGE_NUMBER, pageNumber)
                }
            }

        private val acceptedGoalDataStates =
            setOf(
                GoalSynopsisViewModel.MutableGoalDataStates.INITIALIZED_POPULATED,
                GoalSynopsisViewModel.MutableGoalDataStates.POPULATED
            )

        private const val TASKS_TAB_ID = 0
        private const val HABITS_TAB_ID = 1

        private const val LAST_PAGE_BY_ID = "lpbid"
    }

    private lateinit var pageViewModel: GoalSynopsisViewModel

    private var pageNumber: Int = Constants.IGNORE_PAGE_AS_INT
    private var goalId: Long  = Constants.IGNORE_ID_AS_LONG

    private lateinit var tasksFragment: TaskItemList
    private lateinit var habitsFragment: HabitItemList

    private lateinit var noTasksFragment: OneTextFragment
    private lateinit var noHabitsFragment: OneTextFragment

    private val fragmentContainerId = R.id.goalSynopsisFrameLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // todo: generating page from this fragment causes frame skips - fix it

        val synopsisTitle: TextView = view.findViewById(R.id.goalSynopsisTitle)
        val synopsisDescription: TextView = view.findViewById(R.id.goalSynopsisDescription)
        val synopsisProgress: ProgressBar = view.findViewById(R.id.goalSynopsisProgressBar)

        val tabs: TabLayout = view.findViewById(R.id.goalSynopsisTabs)

        val goalDetailsButton: Button = view.findViewById(R.id.expandGoalButton)

        pageViewModel = ViewModelProvider(requireActivity())[GoalSynopsisViewModel::class.java]

        var lastPageTabId: Int? = TASKS_TAB_ID

        fun updateUI(data: GoalData) {
            synopsisTitle.text = data.goalName
            synopsisDescription.text = data.goalDescription
            synopsisProgress.progress = (100 * data.progress).toInt()
        }

        fun getArgs() {
            arguments?.run {
                pageNumber = this.getInt(MainActivity.PAGE_NUMBER)
                lastPageTabId = this.getInt(LAST_PAGE_BY_ID)
            }
        }

        fun processSavedArgs() {
            if (pageNumber != Constants.IGNORE_PAGE_AS_INT) {
                goalId =
                    pageViewModel.arrayOfGoalData[pageNumber].value?.goalId
                        ?: Constants.IGNORE_ID_AS_LONG

                pageViewModel.lastTabIndexes[pageNumber].value =
                    lastPageTabId ?: TASKS_TAB_ID
            }
        }

        fun tieToPageViewModel(){
            val updater = Observer<GoalData?> {

                if (pageViewModel.arrayOfGoalDataStates[pageNumber].value in acceptedGoalDataStates)
                    it?.run { updateUI(this) }
                // else ignore -> we're being replaced by no goal or there was no goal at all
            }

            pageViewModel.arrayOfGoalData[pageNumber].observe(
                viewLifecycleOwner, updater
            )

        }

        fun readyTheMoreButton() {
            goalDetailsOpener = registerForActivityResult(GoalRefreshRequesterContract()){

                if (it == Constants.IGNORE_ID_AS_LONG) return@registerForActivityResult
                // get goal from given id
                // val pos = pageViewModel.gidToPosition[it]

            }

            goalDetailsButton.setOnClickListener {
                Log.i(LOG_TAG, "launching intent for goalid $goalId")
                if (goalId != Constants.IGNORE_ID_AS_LONG)
                    goalDetailsOpener.launch(Pair(goalId, true))
            }
        }

        fun setupListsFragments() {
            val classNumber: Int? = Constants.viewModelClassToNumber[GoalSynopsisViewModel::class.java]
            if (classNumber != null) {
                tasksFragment = TaskItemList.newInstance(goalId, classNumber)
                habitsFragment = HabitItemList.newInstance(goalId, classNumber)
            } else { Log.i(LOG_TAG, "class number is null"); return }

            noHabitsFragment = OneTextFragment.newInstance(
                resources.getString(R.string.habits_no_habits)
            )
            noTasksFragment = OneTextFragment.newInstance(
                resources.getString(R.string.tasks_no_tasks)
            )

            tabs.addOnTabSelectedListener(TabsListener(
                tabs.getTabAt(HABITS_TAB_ID), tabs.getTabAt(TASKS_TAB_ID),
                childFragmentManager, fragmentContainerId,
                habitsFragment , tasksFragment, pageViewModel.lastTabIndexes[pageNumber],
                noHabitsFragment, noTasksFragment, SizeGetter()
            ))

            // set tab to show
            val idx = pageViewModel.lastTabIndexes[pageNumber].value
            if ( idx != Constants.IGNORE_ID_AS_INT && idx != null){
                tabs.selectTab(null) // to reset selection before added listener
                tabs.selectTab(tabs.getTabAt(idx))
            }
        }

        fun doInThisOrder() {
            getArgs()

            processSavedArgs()

            tieToPageViewModel()

            readyTheMoreButton()

            setupListsFragments()
        }

        doInThisOrder()

    }

    override fun onSaveInstanceState(outState: Bundle) {
        val tmp = pageViewModel.lastTabIndexes[pageNumber].value
        tmp?.run { outState.putInt(LAST_PAGE_BY_ID, this) }
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_goal_synopsis, container, true)
    }

    private inner class SizeGetter {
        private fun isGoalDataValid(): Boolean {
            return goalId != Constants.IGNORE_ID_AS_LONG
            && pageViewModel.arrayOfGoalDataStates[pageNumber].value in acceptedGoalDataStates
        }

        fun getSizeOfHabitList() : Int = if (isGoalDataValid())
            pageViewModel.habitDataArraysPerGoal[goalId]?.size ?: 0
            else 0

        fun getSizeOfTaskList() : Int = if (isGoalDataValid())
            pageViewModel.taskDataArraysPerGoal[goalId]?.size ?: 0
            else 0
    }

    private inner class TabsListener(
        private val habitsTab: TabLayout.Tab?,
        private val tasksTab: TabLayout.Tab?,
        private val fManager: FragmentManager,
        private val replacedViewId: Int,
        private val habitsFragment: HabitItemList,
        private val tasksFragment: Fragment,
        private val lastTabIndex: MutableLiveData<Int>,
        private val noTasksFragment: OneTextFragment,
        private val noHabitsFragment: OneTextFragment,
        private val sizeGetter: SizeGetter
    ) : TabLayout.OnTabSelectedListener {

        override fun onTabSelected(tab: TabLayout.Tab?) {
            val fragment: Fragment? = when (tab) {
                habitsTab -> {
                    lastTabIndex.value = HABITS_TAB_ID
                    if (sizeGetter.getSizeOfHabitList() > 0)
                        habitsFragment
                    else noHabitsFragment
                }
                tasksTab -> {
                    lastTabIndex.value = TASKS_TAB_ID
                    if (sizeGetter.getSizeOfTaskList() > 0)
                        tasksFragment
                    else noTasksFragment
                }
                else -> null
            }
            fragment?.run{
                fManager.beginTransaction().run {
                    setCustomAnimations(R.anim.layout_accelerator, R.anim.layout_deccelerator)
                    replace(replacedViewId, fragment)
                    commit()
                }
            }
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {
            // not used
        }

        override fun onTabReselected(tab: TabLayout.Tab?) {
            // ignore
        }

    }



}
