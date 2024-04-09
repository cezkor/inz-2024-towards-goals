package com.example.towardsgoalsapp.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.towardsgoalsapp.BuildConfig
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.database.DatabaseGeneration
import com.example.towardsgoalsapp.database.DatabaseObjectFactory
import com.example.towardsgoalsapp.database.TGDatabase
import com.example.towardsgoalsapp.etc.errors.ErrorHandling
import com.example.towardsgoalsapp.etc.errors.ErrorHandlingViewModel
import com.example.towardsgoalsapp.goals.AddGoalSuggestion
import com.example.towardsgoalsapp.goals.GoalSynopsis
import com.example.towardsgoalsapp.goals.GoalSynopsisesViewModel
import com.example.towardsgoalsapp.goals.GoalSynopsisesViewModelFactory
import com.example.towardsgoalsapp.reminders.ReminderService
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

    private var isServiceRunning: Boolean = false
    private fun tryToRunReminderServiceIfApplicable() {
        val lbm = LocalBroadcastManager.getInstance(this)
        lbm.sendBroadcastSync(Intent(ReminderService.IS_SERVICE_ALIVE_INTENT_FILTER))
        if (! isServiceRunning) {
            startForegroundService(Intent(this, ReminderService::class.java))
        }
    }

    private var externalUIRefreshReceiver: BroadcastReceiver? = null

    private var reminderServiceAliveReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) {
                isServiceRunning = false
                return
            }
            if (intent.action != ReminderService.SERVICE_ALIVE_INTENT_FILTER) {
                isServiceRunning = false
                return
            }
            isServiceRunning = true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.title_only_menu, menu)
        return true
    }

    override fun onDestroy() {
        externalUIRefreshReceiver?.run {
            LocalBroadcastManager.getInstance(this@MainActivity)
                .unregisterReceiver(this)
        }
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tryToRunReminderServiceIfApplicable()

        val toolbar:  androidx.appcompat.widget.Toolbar= findViewById(R.id.goalToolbar)
        toolbar.setTitle(R.string.app_name)
        setSupportActionBar(toolbar)

        (application as App? )?.run {
            databaseObject = DatabaseObjectFactory.newDatabaseObject(this.driver)
        }

        Log.i(LOG_TAG, "application should be running on test data: " +
                "${BuildConfig.SHOULD_USE_TEST_DATA}")

        sharedViewModelForAllPages =
            ViewModelProvider(this,
        GoalSynopsisesViewModelFactory(databaseObject))[GoalSynopsisesViewModel::class.java]
        Log.i(LOG_TAG, "created viewmodel")

        sharedViewModelForAllPages.exceptionMutable.observe(this) {
            ErrorHandling.showExceptionDialog(this, it)
        }

        fun prepareActivity() {
            //  match everything
            externalUIRefreshReceiver = ShouldRefreshUIBroadcastReceiver(null) {
                lifecycleScope.launch {
                    Log.i(LOG_TAG, "Getting goals because of refresh request")
                    sharedViewModelForAllPages.getEverything()
                }
            }
            LocalBroadcastManager.getInstance(this).registerReceiver(
                externalUIRefreshReceiver!!,
                IntentFilter(ShouldRefreshUIBroadcastReceiver.INTENT_FILTER)
            )

            goalPager = findViewById(R.id.goalPager)

            // position in page is the same as position in the goal data states array
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
                it.handleIfNotHandledWith {
                    if (! it.value) return@handleIfNotHandledWith
                    // if all ready -> create the adapter as everything that could be
                    goalPager.adapter = GoalPagesAdapter(
                        this,
                        sharedViewModelForAllPages.arrayOfGoalDataStates
                    )
                }
            }

            lifecycleScope.launch(sharedViewModelForAllPages.exceptionHandler) {
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

        if (BuildConfig.SHOULD_USE_TEST_DATA) {
            lifecycleScope.launch(sharedViewModelForAllPages.exceptionHandler) {
                Toast.makeText(this@MainActivity,
                    getString(R.string.check_for_test_data), Toast.LENGTH_SHORT).show()
                if (DatabaseGeneration.assureDatabaseHasTestData(databaseObject)) {
                    Log.i(LOG_TAG, "getEverything because filled out test data")
                    Toast.makeText(this@MainActivity,
                        getString(R.string.put_test_data), Toast.LENGTH_SHORT).show()
                }
                else {
                    Log.w(LOG_TAG, "test data not loaded")
                    Toast.makeText(this@MainActivity,
                        getString(R.string.test_data_not_loaded), Toast.LENGTH_SHORT).show()
                }
                prepareActivity()
        } }
        else prepareActivity()
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