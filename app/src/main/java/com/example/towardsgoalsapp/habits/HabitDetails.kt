package com.example.towardsgoalsapp.habits

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.DoubleTapOnBack
import com.example.towardsgoalsapp.OwnerType
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.goals.GoalDetails
import com.example.towardsgoalsapp.habits.HabitInfoContract.Companion.HABIT_ID_FROM_REQUESTER
import com.example.towardsgoalsapp.impints.ImpIntData
import com.example.towardsgoalsapp.impints.IntImpItemList
import com.example.towardsgoalsapp.main.OneTextFragment
import com.example.towardsgoalsapp.main.TextsFragment
import com.example.towardsgoalsapp.main.TextsViewModel
import com.example.towardsgoalsapp.reminders.ReminderSetting
import com.example.towardsgoalsapp.tasks.TaskInfoContract.Companion.FOR_ADDING
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class HabitViewModel(private val habitId: Long): TextsViewModel() {

    val mutableHabitData = MutableLiveData<HabitData>()

    val arrayOfMutableImpIntData: ArrayList<MutableLiveData<ImpIntData>> =
        java.util.ArrayList()

    fun updateOneHabit() {}

    fun getEverything() {}
    override fun updateTexts(newName: String?, newDescription: String?) {
        TODO("Not yet implemented")
    }

}

class HabitInfoContract: ActivityResultContract<Pair<Long, Boolean>, Boolean>() {

    companion object {
        const val HABIT_ID_FROM_REQUESTER = "hifr"
        const val FOR_ADDING = "hiisfa"
        const val REFRESH_FOR_REQUESTER = "rfr"
        const val LOG_TAG = "HIC"
    }
    override fun createIntent(context: Context, input: Pair<Long, Boolean>): Intent {
        Log.i(LOG_TAG, "creating intent for $context, input $input")

        return Intent(context, HabitDetails::class.java).apply {
            action = Intent.ACTION_SEND
            putExtra(HABIT_ID_FROM_REQUESTER, input.first)
            putExtra(FOR_ADDING, input.second)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        val data = intent?.getBooleanExtra(REFRESH_FOR_REQUESTER, false)
        Log.i(LOG_TAG, "got data $data, is OK: ${resultCode == Activity.RESULT_OK}")

        return if (resultCode == Activity.RESULT_OK && data != null)
            data
        else
            false
    }
}


class HabitDetails : AppCompatActivity() {

    companion object{
        const val LOG_TAG = "HabitDetails"

        const val LAST_TAB_ID = "hdlt"
        const val HABIT_ID = "hdgid"
        const val UNFINISHED_EDITING = "hdue"
        const val FOR_ADDING = HabitInfoContract.FOR_ADDING

        private const val TEXTS_TAB_ID = 0
        private const val IMP_INTS_TAB_ID = 2
        private const val REMINDER_TAB_ID = 1
        private const val TAB_COUNT = 3
    }

    private val classNumber = Constants.viewModelClassToNumber[HabitViewModel::class.java]

    private lateinit var viewModel: HabitViewModel

    private var habitId = Constants.IGNORE_ID_AS_LONG

    private var isEdit: Boolean = false
    private var forAdding: Boolean = false
    private var isUnfinished: Boolean = false

    private var lastTabId: Int = TEXTS_TAB_ID
    private var menu: Menu? = null
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.habit_detail_menu, menu)
        this.menu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.addImpIntItem -> {
                true
            }
            R.id.editItem -> {
                if (! forAdding)
                    if (isEdit) { isEdit = false; item.title = getString(R.string.edit_end_name) }
                    else { isEdit = true; item.title = getString(R.string.enable_edit) }
                else {
                    // end adding
                    finish()
                }
                true
            }
            R.id.habitDoneNotWellItem -> {
                true
            }
            R.id.habitDoneWellItem -> {
                true
            }
            R.id.deleteHabitItem -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            putInt(
                LAST_TAB_ID,
                findViewById<TabLayout>(R.id.habitTabs).id)
            putLong(HABIT_ID, habitId)
            putBoolean(UNFINISHED_EDITING, isEdit)
            putBoolean(FOR_ADDING, forAdding)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habit_details)

        val toolbar:  Toolbar= findViewById(R.id.habitToolbar)

        val tabsPager: ViewPager2 = findViewById(R.id.habitDetailsViewPager)
        val tabs: TabLayout = findViewById(R.id.habitTabs)

        fun recoverSavedState() {
            savedInstanceState?.run {
                isUnfinished = getBoolean(UNFINISHED_EDITING, false)
                habitId = getLong(HABIT_ID, Constants.IGNORE_ID_AS_LONG)
                lastTabId = getInt(LAST_TAB_ID, TEXTS_TAB_ID)
                forAdding = getBoolean(FOR_ADDING, false)
            }
        }

        fun getArgs() {
            habitId = intent.getLongExtra(HABIT_ID_FROM_REQUESTER, habitId)
            forAdding = intent.getBooleanExtra(FOR_ADDING, forAdding)
        }

        fun processArgsAndSavedState () {
            if (forAdding || isUnfinished) isEdit = true
            viewModel = HabitViewModel(habitId)
        }

        fun prepareUI() {

            tabsPager.isUserInputEnabled = false

            tabsPager.adapter = HabitDetailsPageAdapter(this)

            TabLayoutMediator(tabs, tabsPager) {
                tab, position -> tab.text = when (position) {
                    TEXTS_TAB_ID -> getString(R.string.name_and_description)
                    REMINDER_TAB_ID -> getString(R.string.reminders_reminder)
                    IMP_INTS_TAB_ID -> getString(R.string.impints_name_plural)
                    else -> Constants.EMPTY_STRING
                }
            }.attach()

            tabsPager.currentItem = lastTabId

            toolbar.title = viewModel.mutableHabitData.value?.habitName
                ?: R.string.habits_name.toString()
            setSupportActionBar(toolbar)

            viewModel.mutableHabitData.observe(this) {
                toolbar.title = it.habitName
            }

            if (forAdding) {
                menu?.getItem(R.id.editItem)?.title = getString(R.string.add_end_name)
            }
            onBackPressedDispatcher.addCallback(
                DoubleTapOnBack(this, getString(R.string.abandoning_aoe)) {
                    if (isEdit) setResult(RESULT_CANCELED)
                    onBackPressed()
                }
            )
        }

        fun doInThisOrder() {

            recoverSavedState()

            getArgs()

            processArgsAndSavedState()

            prepareUI()

            viewModel.getEverything()

        }; doInThisOrder()


    }

    override fun onStop() {
        val result = RESULT_CANCELED
        // determine if there is need for refreshing
        val refresh = false
        setResult(result, Intent().apply {
            putExtra(HabitInfoContract.REFRESH_FOR_REQUESTER, refresh)
        })
        super.onStop()
    }

    private inner class HabitDetailsPageAdapter(fragAct: FragmentActivity):
        FragmentStateAdapter(fragAct) {
        override fun getItemCount(): Int = TAB_COUNT

        override fun createFragment(position: Int): Fragment {
            if (classNumber == null) return Fragment()

            return when (position) {
                TEXTS_TAB_ID ->
                    TextsFragment.newInstance(isEdit)
                IMP_INTS_TAB_ID ->
                    if (viewModel.arrayOfMutableImpIntData.isEmpty())
                        OneTextFragment.newInstance(getString(R.string.impints_no_impints))
                    else
                        IntImpItemList.newInstance(
                            habitId,
                            OwnerType.TYPE_HABIT,
                            classNumber,
                            isEdit
                        )
                REMINDER_TAB_ID -> ReminderSetting.newInstance(OwnerType.TYPE_HABIT, habitId, isEdit)
                else -> Fragment()
            }
        }



    }

}