package com.example.towardsgoalsapp.main

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
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

class MainActivity : FragmentActivity() {

    companion object {

        const val LOG_TAG = "MainActivity"
        const val PAGE_NUMBER = "pgn"

    }

    private lateinit var sharedViewModelForAllPages: GoalSynopsisViewModel
    private lateinit var goalPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedViewModelForAllPages =
            ViewModelProvider(this)[GoalSynopsisViewModel::class.java]

        goalPager = findViewById(R.id.goalPager)

        sharedViewModelForAllPages.isGoalDataPresent[3] = true

        // position in page is the same as position in the isGoalDataPresent array
        val adapter = GoalPagesAdapter(this, sharedViewModelForAllPages.isGoalDataPresent)

        var pageNum = 0
        while (pageNum < Constants.MAX_GOALS_AMOUNT){
            Log.i(LOG_TAG, "Creating observer for page $pageNum")

            val currentPageNum = pageNum
            val observer = Observer<GoalData?> {

                // i am unable currently to come up with better
                // solution to making ViewPager2 replace entire page fragment
                // other than to remake its whole adapter
                // todo: if possible, find better solution
                goalPager.adapter = GoalPagesAdapter(this,
                    sharedViewModelForAllPages.isGoalDataPresent)
                goalPager.setCurrentItem(currentPageNum, false)

            }

            sharedViewModelForAllPages.arrayOfGoalData[pageNum].observe(
                this@MainActivity, observer
            )

            pageNum += 1
        }
        goalPager.adapter = adapter

    }

    private inner class GoalPagesAdapter(
        fragmentActivity: FragmentActivity,
        goalPopulation: Array<Boolean>
    ) : FragmentStateAdapter(fragmentActivity) {

        private val LOG_TAG = "GoalPagesAdapter"

        private var shouldBePopulatedWithGoal: Array<Boolean> = goalPopulation

        // todo: read from database which pages are populated with goals
        // and ofc read the goal data, task data, habit data etc.
        override fun getItemCount(): Int = Constants.MAX_GOALS_AMOUNT

        override fun createFragment(position: Int): Fragment {

            val fragment: Fragment = if (shouldBePopulatedWithGoal[position]) {
                GoalSynopsis()
            } else {
                AddGoalSuggestion()
            }

            Log.i(LOG_TAG, "created fragment $fragment, page number $position")

            val bundle = Bundle()
            bundle.putInt(PAGE_NUMBER, position)
            fragment.arguments = bundle

            return fragment
        }

    }
}