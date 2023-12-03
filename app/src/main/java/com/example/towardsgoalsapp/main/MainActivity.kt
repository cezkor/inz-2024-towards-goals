package com.example.towardsgoalsapp.main

import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.goals.GoalSynopsis

class MainActivity : FragmentActivity() {

    private lateinit var goalPageNum: TextView
    private lateinit var goalPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        goalPageNum = findViewById(R.id.goalPageNumber)
        goalPager = findViewById(R.id.goalPager)

        goalPager.adapter = GoalPagesAdapter(this)

    }

    private inner class GoalPagesAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

        override fun getItemCount(): Int {
            return Constants.MAX_GOALS_AMOUNT
        }

        override fun createFragment(position: Int): Fragment {
            return GoalSynopsis(position)
        }

    }
}