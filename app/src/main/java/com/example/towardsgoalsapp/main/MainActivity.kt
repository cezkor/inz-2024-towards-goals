package com.example.towardsgoalsapp.main

import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.database.DatabaseObjectFactory
import com.example.towardsgoalsapp.database.TGDatabase
import com.example.towardsgoalsapp.goals.AddGoalSuggestion
import com.example.towardsgoalsapp.goals.GoalSynopsis
import com.example.towardsgoalsapp.goals.GoalSynopsisesViewModel
import com.example.towardsgoalsapp.goals.GoalSynopsisesViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {

        const val LOG_TAG = "MainActivity"
        const val PAGE_NUMBER = "pgn"

        private val statesForPopulatingWithGoalSynopsis =
            setOf(
                GoalSynopsisesViewModel.MutableGoalDataStates.INITIALIZED_POPULATED,
                GoalSynopsisesViewModel.MutableGoalDataStates.POPULATED,
                GoalSynopsisesViewModel.MutableGoalDataStates.REFRESHED,
            )
        private val statesForPopulatingWithGoalSuggestion =
            setOf(
                GoalSynopsisesViewModel.MutableGoalDataStates.INITIALIZED_EMPTY,
                GoalSynopsisesViewModel.MutableGoalDataStates.EMPTIED
            )
    }

    private lateinit var sharedViewModelForAllPages: GoalSynopsisesViewModel
    private lateinit var goalPager: ViewPager2
    private lateinit var databaseObject: TGDatabase

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

        (application as App? )?.run {
            databaseObject = DatabaseObjectFactory.newDatabaseObject(this.driver)
        }

        sharedViewModelForAllPages =
            ViewModelProvider(this,
        GoalSynopsisesViewModelFactory(databaseObject))[GoalSynopsisesViewModel::class.java]

        Log.i(LOG_TAG, "created viewmodel: $sharedViewModelForAllPages")

        goalPager = findViewById(R.id.goalPager)

        // position in page is the same as position in the goal data states array
        val adapter = GoalPagesAdapter(this,
            sharedViewModelForAllPages.arrayOfGoalDataStates)

        fun recreatePagerAdapter() {
            goalPager.adapter = GoalPagesAdapter(
                this,
                sharedViewModelForAllPages.arrayOfGoalDataStates
            )
        }; recreatePagerAdapter()

        var pageNum = 0
        while (pageNum < Constants.MAX_GOALS_AMOUNT){
            Log.i(LOG_TAG, "Creating observer for page $pageNum")

            val currentPageNum = pageNum
            val observer = Observer<GoalSynopsisesViewModel.MutableGoalDataStates> {
                when (it) {
                    GoalSynopsisesViewModel.MutableGoalDataStates.NOT_READY,
                    GoalSynopsisesViewModel.MutableGoalDataStates.INITIALIZED_EMPTY,
                    GoalSynopsisesViewModel.MutableGoalDataStates.INITIALIZED_POPULATED
                        -> { /* ignore */ }
                    GoalSynopsisesViewModel.MutableGoalDataStates.REFRESHED -> {
                        // goal synopsis will refresh itself as it is observing Mutable of GoalData
                        goalPager.setCurrentItem(currentPageNum, false)
                    }
                    else -> { // POPULATED, EMPTIED
                        // i am unable currently to come up with better
                        // solution to making ViewPager2 replace entire page fragment
                        // other than to remake its whole adapter
                        recreatePagerAdapter()
                        goalPager.setCurrentItem(currentPageNum, false)
                    }
                }
            }

            sharedViewModelForAllPages.arrayOfGoalDataStates[pageNum].observe(
                this@MainActivity, observer
            )

            pageNum += 1
        }

        sharedViewModelForAllPages.allReady.observe(this) {
            if (it) {
                // if all ready -> create the adapter as everything that could be
                goalPager.adapter = GoalPagesAdapter(
                    this,
                    sharedViewModelForAllPages.arrayOfGoalDataStates
                )
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                Log.i(LOG_TAG, "Getting goals")
                sharedViewModelForAllPages.getEverything()
            }
        }

        savedInstanceState?.run {
            val i: Int = this.getInt(PAGE_NUMBER)
            if ( (i > 0) and (i < Constants.MAX_GOALS_AMOUNT) ) { goalPager.currentItem = i }
        }

    }

    override fun onRestoreInstanceState(
        savedInstanceState: Bundle,
    ) {
        savedInstanceState.run {
            val i: Int = this.getInt(PAGE_NUMBER)
            if ( (i > 0) and (i < Constants.MAX_GOALS_AMOUNT) ) { goalPager.currentItem = i }
        }
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(PAGE_NUMBER, goalPager.currentItem)
        super.onSaveInstanceState(outState)
    }

    private inner class GoalPagesAdapter(
        fragmentActivity: FragmentActivity,
        goalDataStateArray: Array<MutableLiveData<GoalSynopsisesViewModel.MutableGoalDataStates>>,
    ) : FragmentStateAdapter(fragmentActivity) {

        private var goalDataStates: Array<MutableLiveData<GoalSynopsisesViewModel.MutableGoalDataStates>>
        = goalDataStateArray

        override fun getItemCount(): Int = Constants.MAX_GOALS_AMOUNT

        override fun createFragment(position: Int): Fragment {
            val fragment: Fragment = when (goalDataStates[position].value) {
                in statesForPopulatingWithGoalSynopsis -> GoalSynopsis.newInstance(position)
                in statesForPopulatingWithGoalSuggestion-> AddGoalSuggestion.newInstance(position)
                else -> Fragment()
            }

            Log.i(LOG_TAG, "created fragment $fragment, page number $position")

            return fragment
        }

    }
}