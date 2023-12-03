package com.example.towardsgoalsapp.main

import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.goals.AddGoalSuggestion
import com.example.towardsgoalsapp.goals.GoalSynopsis

class MainActivity : FragmentActivity() {

    companion object {
        const val POSITION_ID = "pos_id"
    }

    private lateinit var goalPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        goalPager = findViewById(R.id.goalPager)

        goalPager.adapter = GoalPagesAdapter(this)

    }

    private inner class GoalPagesAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

        val pagerIsPagePopulationWithGoalArray: Array<Boolean> = Array(Constants.MAX_GOALS_AMOUNT) { false }

        // to do: read from database which pages are populated with goals
        // and ofc read the goal data, task data, habit data etc.
        override fun getItemCount(): Int = Constants.MAX_GOALS_AMOUNT

        override fun createFragment(position: Int): Fragment {
            return if (pagerIsPagePopulationWithGoalArray[position]) {
                val fragment = GoalSynopsis()
                val bundle = Bundle()
                bundle.putInt(POSITION_ID, position)
                fragment.arguments = bundle
                fragment
            } else {
                AddGoalSuggestion()
            }
        }


    }
}