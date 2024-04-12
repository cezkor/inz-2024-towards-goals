package org.cezkor.towardsgoalsapp.goals

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.habits.HabitItemListFragment
import org.cezkor.towardsgoalsapp.main.MainActivity
import org.cezkor.towardsgoalsapp.etc.OneTextFragment
import org.cezkor.towardsgoalsapp.tasks.TaskItemListFragment
import com.google.android.material.tabs.TabLayout
import org.cezkor.towardsgoalsapp.database.*
import org.cezkor.towardsgoalsapp.etc.ShortenedDescriptionFixer
import org.cezkor.towardsgoalsapp.etc.errors.ErrorHandling

class GoalSynopsis: Fragment() {

    private lateinit var goalDetailsOpener: GoalRefreshRequesterResultLauncher

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
                GoalSynopsisesViewModel.MutableGoalDataStates.INITIALIZED_POPULATED,
                GoalSynopsisesViewModel.MutableGoalDataStates.POPULATED,
                GoalSynopsisesViewModel.MutableGoalDataStates.REFRESHED
            )

        private const val TASKS_TAB_ID = 0
        private const val HABITS_TAB_ID = 1

        private const val LAST_PAGE_BY_ID = "lpbid"
    }

    private lateinit var pageViewModel: GoalSynopsisesViewModel

    private var pageNumber: Int = org.cezkor.towardsgoalsapp.Constants.IGNORE_PAGE_AS_INT
    private var goalId: Long  = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG

    private lateinit var tasksFragment: TaskItemListFragment
    private lateinit var habitsFragment: HabitItemListFragment

    private lateinit var noTasksFragment: OneTextFragment
    private lateinit var noHabitsFragment: OneTextFragment

    private val fragmentContainerId = R.id.goalSynopsisFrameLayout
    private var lastPageTabId: Int = TASKS_TAB_ID

    private var animateTabs = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // todo: generating page from this fragment causes frame skips - fix it

        try {
            requireActivity()
            requireContext()
        }
        catch (e: IllegalStateException) {
            Log.e(LOG_TAG, "no activity nor context", e)
            return
        }

        val synopsisTitle: TextView = view.findViewById(R.id.goalSynopsisTitle)
        val synopsisGoalPageNumber: TextView = view.findViewById(R.id.goalSynopsisGoalPage)
        val synopsisDescription: TextView = view.findViewById(R.id.goalSynopsisDescription)
        val synopsisProgress: ProgressBar = view.findViewById(R.id.goalSynopsisProgressBar)

        val tabs: TabLayout = view.findViewById(R.id.goalSynopsisTabs)

        val goalDetailsButton: Button = view.findViewById(R.id.expandGoalButton)

        val classNumber: Int? = org.cezkor.towardsgoalsapp.Constants.viewModelClassToNumber[GoalSynopsisesViewModel::class.java]
        if (classNumber == null) {
            Log.e(LOG_TAG,"Class number is null")
            return
        }

        pageViewModel = ViewModelProvider(requireActivity())[GoalSynopsisesViewModel::class.java]

        pageViewModel.exceptionMutable.observe(viewLifecycleOwner) {
            ErrorHandling.showExceptionDialog(requireActivity(), it)
        }

        fun updateUI(data: GoalData) {
            synopsisTitle.text = data.goalName
            synopsisGoalPageNumber.text =
                getString(R.string.goals_goal_page, data.pageNumber+1, org.cezkor.towardsgoalsapp.Constants.MAX_GOALS_AMOUNT)
            synopsisDescription.text = ShortenedDescriptionFixer.fix(data.goalDescription)
            synopsisProgress.progress = (100 * data.goalProgress).toInt()
            if (data.goalEditUnfinished)
                goalDetailsButton
                    .setCompoundDrawables(
                        null,
                        null,
                        AppCompatResources.getDrawable(requireContext(), R.drawable.white_alert),
                        null
                    )

            // recreate fragment at current position, reset selection then select its tab
            val curTab
                = if (tabs.selectedTabPosition != -1) tabs.selectedTabPosition else TASKS_TAB_ID
            val newFragment = when (curTab) {
                TASKS_TAB_ID -> { TaskItemListFragment.newInstance(goalId, classNumber) }
                HABITS_TAB_ID -> {
                    HabitItemListFragment.newInstance(goalId, classNumber)
                }
                else -> Fragment()
            }
            if (curTab == TASKS_TAB_ID && newFragment is TaskItemListFragment)
                tasksFragment = newFragment
            if (curTab == HABITS_TAB_ID && newFragment is HabitItemListFragment)
                habitsFragment = newFragment

            val oldAnimateTabsVal = animateTabs
            animateTabs = false
            tabs.selectTab(null)
            tabs.selectTab(tabs.getTabAt(curTab))
            animateTabs = oldAnimateTabsVal
        }

        fun getArgsAndState() {
            arguments?.run {
                pageNumber = this.getInt(MainActivity.PAGE_NUMBER)
            }
            savedInstanceState?.run {
                lastPageTabId = this.getInt(LAST_PAGE_BY_ID, lastPageTabId)
            }
        }

        fun processSavedArgs() {
            if (pageNumber != org.cezkor.towardsgoalsapp.Constants.IGNORE_PAGE_AS_INT) {
                goalId =
                    pageViewModel.arrayOfGoalData[pageNumber].value?.goalId
                        ?: org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG

                pageViewModel.lastTabIndexes[pageNumber].value =
                    lastPageTabId
            }
        }

        fun tieToPageViewModel(){

            fun checkAndUpdate(goal: GoalData?) {
                if (pageViewModel.arrayOfGoalDataStates[pageNumber].value in acceptedGoalDataStates)
                    goal?.run { updateUI(this) }
            }

            pageViewModel.arrayOfGoalDataStates[pageNumber].observe(viewLifecycleOwner) {
                val goal = pageViewModel.arrayOfGoalData[pageNumber].value
                checkAndUpdate(goal)
            }

        }

        fun readyTheMoreButton() {
            goalDetailsOpener = registerForActivityResult(GoalRefreshRequesterContract()){
                if (it == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) return@registerForActivityResult
                // if returned an id -> update goal of such id
                pageViewModel.getOrUpdateOneGoal(it)
            }

            goalDetailsButton.setOnClickListener {
                Log.i(LOG_TAG, "launching intent for goalid $goalId")
                if (goalId != org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)             // i don't need page number
                                                                       // for opening an
                                                                       // existing goal
                    goalDetailsOpener.launch(Triple(goalId, false, null))
            }
        }

        fun setupListsFragments() {

            tasksFragment = TaskItemListFragment.newInstance(goalId, classNumber)
            habitsFragment = HabitItemListFragment.newInstance(goalId, classNumber)

            noHabitsFragment = OneTextFragment.newInstance(
                resources.getString(R.string.habits_no_habits)
            )
            noTasksFragment = OneTextFragment.newInstance(
                resources.getString(R.string.tasks_no_tasks)
            )

            tabs.addOnTabSelectedListener(TabsListener(
                tabs.getTabAt(HABITS_TAB_ID), tabs.getTabAt(TASKS_TAB_ID),
                childFragmentManager, fragmentContainerId, pageViewModel.lastTabIndexes[pageNumber],
                noTasksFragment, noHabitsFragment, SizeGetter()
            ))

            // set tab to show
            val idx = pageViewModel.lastTabIndexes[pageNumber].value
            if ( idx != org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_INT && idx != null){
                tabs.selectTab(null) // to reset selection before added listener
                tabs.selectTab(tabs.getTabAt(idx))
            }
        }

        fun doInThisOrder() {
            getArgsAndState()

            processSavedArgs()

            tieToPageViewModel()

            readyTheMoreButton()

            setupListsFragments()
        }

        doInThisOrder()

    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        lastPageTabId = savedInstanceState?.getInt(LAST_PAGE_BY_ID) ?: lastPageTabId

        super.onViewStateRestored(savedInstanceState)
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
        return inflater.inflate(R.layout.fragment_goal_synopsis, container, false)
    }

    private inner class SizeGetter {
        private fun isGoalDataValid(): Boolean {
            return goalId != org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG
            && pageViewModel.arrayOfGoalDataStates[pageNumber].value in acceptedGoalDataStates
        }

        fun getSizeOfHabitList() : Int = if (isGoalDataValid())
            pageViewModel.getHabitsSharer(goalId)?.getArrayOfUserData()
                ?.filter { md -> md.value != null }?.size ?: 0
            else 0

        fun getSizeOfTaskList() : Int = if (isGoalDataValid())
            pageViewModel.getTasksSharer(goalId)?.getArrayOfUserData()
                ?.filter { md -> md.value != null }?.size ?: 0
            else 0
    }

    private inner class TabsListener(
        private val habitsTab: TabLayout.Tab?,
        private val tasksTab: TabLayout.Tab?,
        private val fManager: FragmentManager,
        private val replacedViewId: Int,
        private val lastTabIndex: MutableLiveData<Int>,
        private val noTasksFragment: OneTextFragment,
        private val noHabitsFragment: OneTextFragment,
        private val sizeGetter: SizeGetter
    ) : TabLayout.OnTabSelectedListener {


        val gsTag = "gstag99999"

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
                    setReorderingAllowed(true)
                    if (animateTabs)
                        setCustomAnimations(R.anim.layout_accelerator, R.anim.layout_deccelerator)
                    replace(replacedViewId, fragment, gsTag)
                    commitNow()
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
