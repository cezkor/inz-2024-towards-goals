package com.example.towardsgoalsapp.main

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.goals.AddGoalSuggestion
import com.example.towardsgoalsapp.goals.GoalData
import com.example.towardsgoalsapp.goals.GoalSynopsis
import com.example.towardsgoalsapp.goals.GoalSynopsisViewModel

class MainActivity : AppCompatActivity() {

    companion object {

        const val LOG_TAG = "MainActivity"
        const val PAGE_NUMBER = "pgn"

        private val statesForPopulatingWithGoalSynopsis =
            setOf(
                GoalSynopsisViewModel.MutableGoalDataStates.INITIALIZED_POPULATED,
                GoalSynopsisViewModel.MutableGoalDataStates.POPULATED,
                GoalSynopsisViewModel.MutableGoalDataStates.REFRESHED,
            )
        private val statesForPopulatingWithGoalSuggestion =
            setOf(
                GoalSynopsisViewModel.MutableGoalDataStates.INITIALIZED_EMPTY,
                GoalSynopsisViewModel.MutableGoalDataStates.EMPTIED
            )
    }

    private lateinit var sharedViewModelForAllPages: GoalSynopsisViewModel
    private lateinit var goalPager: ViewPager2

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.title_only_menu, menu)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar:  androidx.appcompat.widget.Toolbar= findViewById(R.id.goalToolbar)
        toolbar.setTitle(R.string.app_name)
        setSupportActionBar(toolbar)

        sharedViewModelForAllPages =
            ViewModelProvider(this)[GoalSynopsisViewModel::class.java]

        // update viewmodel with data

//        sharedViewModelForAllPages.
//        arrayOfGoalDataStates[3].value =
//            GoalSynopsisViewModel.MutableGoalDataStates.INITIALIZED_POPULATED
//        sharedViewModelForAllPages.
//        arrayOfGoalData[3].value= GoalData(100, "no", "des", 0.5)

        goalPager = findViewById(R.id.goalPager)

        // position in page is the same as position in the goal data states array
        val adapter = GoalPagesAdapter(this,
            sharedViewModelForAllPages.arrayOfGoalDataStates)

        goalPager.adapter = adapter

        var pageNum = 0
        while (pageNum < Constants.MAX_GOALS_AMOUNT){
            Log.i(LOG_TAG, "Creating observer for page $pageNum")

            val currentPageNum = pageNum
            val observer = Observer<GoalSynopsisViewModel.MutableGoalDataStates> {
                when (it) {
                    GoalSynopsisViewModel.MutableGoalDataStates.INITIALIZED_POPULATED,
                    GoalSynopsisViewModel.MutableGoalDataStates.INITIALIZED_EMPTY -> {
                        goalPager.adapter = GoalPagesAdapter(
                            this,
                            sharedViewModelForAllPages.arrayOfGoalDataStates
                        )
                    }
                    GoalSynopsisViewModel.MutableGoalDataStates.NOT_READY -> { /* ignore */ }
                    GoalSynopsisViewModel.MutableGoalDataStates.REFRESHED -> {
                        // goal synopsis will refresh itself as it is observing Mutable of GoalData
                        goalPager.setCurrentItem(currentPageNum, false)
                    }
                    else -> { // REPLACED, EMPTIED
                        // i am unable currently to come up with better
                        // solution to making ViewPager2 replace entire page fragment
                        // other than to remake its whole adapter
                        // todo: if possible, find better solution
                        goalPager.adapter = GoalPagesAdapter(
                            this,
                            sharedViewModelForAllPages.arrayOfGoalDataStates
                        )
                        goalPager.setCurrentItem(currentPageNum, false)
                    }
                }
            }

            sharedViewModelForAllPages.arrayOfGoalDataStates[pageNum].observe(
                this@MainActivity, observer
            )

            pageNum += 1
        }

    }

    private inner class GoalPagesAdapter(
        fragmentActivity: FragmentActivity,
        goalDataStateArray: Array<MutableLiveData<GoalSynopsisViewModel.MutableGoalDataStates>>
    ) : FragmentStateAdapter(fragmentActivity) {

        private val LOG_TAG = "GoalPagesAdapter"

        private var goalDataStates: Array<MutableLiveData<GoalSynopsisViewModel.MutableGoalDataStates>>
        = goalDataStateArray

        // todo: read from database which pages are populated with goals
        // and ofc read the goal data, task data, habit data etc.
        override fun getItemCount(): Int = Constants.MAX_GOALS_AMOUNT

        override fun createFragment(position: Int): Fragment {
            val fragment: Fragment = when (goalDataStates[position].value) {
                in statesForPopulatingWithGoalSynopsis -> GoalSynopsis()
                in statesForPopulatingWithGoalSuggestion-> AddGoalSuggestion()
                else -> Fragment()
            }

            Log.i(LOG_TAG, "created fragment $fragment, page number $position")

            val bundle = Bundle()
            bundle.putInt(PAGE_NUMBER, position)
            fragment.arguments = bundle

            return fragment
        }

    }
}