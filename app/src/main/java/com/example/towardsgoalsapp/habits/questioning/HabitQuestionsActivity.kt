package com.example.towardsgoalsapp.habits.questioning

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.etc.DoubleTapOnBack
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.database.DatabaseObjectFactory
import com.example.towardsgoalsapp.database.TGDatabase
import com.example.towardsgoalsapp.habits.HabitDetails
import com.example.towardsgoalsapp.habits.HabitViewModel
import com.example.towardsgoalsapp.habits.questioning.HabitQuestioningContract.Companion.HABIT_ID_TO_REFRESH_FOR_REQUESTER
import com.example.towardsgoalsapp.main.App
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class HabitQuestionsViewModelFactory(private val dbo: TGDatabase,
                                  private val taskId: Long
    ): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HabitQuestionsViewModel(dbo, taskId) as T
    }
}

class HabitQuestionsViewModel(private val dbo: TGDatabase,
                           private var habitId: Long
    ):
    HabitViewModel(dbo, habitId, Constants.IGNORE_ID_AS_LONG) {

    // val habStatsDataRepo

    val shouldLeave = MutableLiveData<Boolean>(false)

    fun markHabitAs(doneWell: Boolean) {
        viewModelScope.launch {
            if (doneWell) habitRepo.markHabitDoneWell(habitId)
            else habitRepo.markHabitDoneNotWell(habitId)
        }
    }

    fun skipHabit() {
        viewModelScope.launch {
            habitRepo.skipHabit(habitId)
        }
    }

}

typealias HabitQuestioningLauncher = ActivityResultLauncher<Long>

class HabitQuestioningContract: ActivityResultContract<Long, Long>() {

    companion object {
        const val HABIT_ID_FROM_REQUESTER = "tdfr"
        const val HABIT_ID_TO_REFRESH_FOR_REQUESTER = "tdtidtorfr"
        const val LOG_TAG = "TDC"
    }
    override fun createIntent(context: Context, input: Long): Intent {
        Log.i(LOG_TAG, "creating intent for $context, input $input")

        return Intent(context, HabitQuestions::class.java).apply {
            action = Intent.ACTION_SEND
            putExtra(HABIT_ID_FROM_REQUESTER, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Long {
        val idToRefresh
            = intent?.getLongExtra(HABIT_ID_TO_REFRESH_FOR_REQUESTER, Constants.IGNORE_ID_AS_LONG)
        Log.i(LOG_TAG, "got data $idToRefresh, is OK: ${resultCode == Activity.RESULT_OK}")
        return if (resultCode == Activity.RESULT_OK && idToRefresh != null)
            idToRefresh
        else
            Constants.IGNORE_ID_AS_LONG
    }
}

class HabitQuestions : AppCompatActivity() {

    companion object {
        const val LOG_TAG = "HabitQuestions"
        const val HABIT_ID = "togtid"

        const val TAB_COUNT = 2
        const val QUESTIONS_TAB_ID = 0;
        const val MARK_HABIT_TAB_ID = 1;
    }

    private lateinit var viewModel: HabitQuestionsViewModel
    private val classNumber = Constants.viewModelClassToNumber[HabitQuestionsViewModel::class.java]

    private lateinit var databaseObject: TGDatabase

    private var habitId: Long = Constants.IGNORE_ID_AS_LONG

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.title_only_menu, menu)
        return true
    }

    override fun onRestoreInstanceState(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?
    ) {
        savedInstanceState?.run {
            habitId = this.getLong(HABIT_ID, Constants.IGNORE_ID_AS_LONG)
        }

        super.onRestoreInstanceState(savedInstanceState, persistentState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // save question results to database

        outState.apply {
            putLong(HABIT_ID, habitId)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (isFinishing) doAfterDoneWithQuestions()
        super.onDestroy()
    }

    private fun doAfterDoneWithQuestions() {
        // save question results to database

        setResult(Activity.RESULT_OK, intent.apply {
            putExtra(HABIT_ID_TO_REFRESH_FOR_REQUESTER, habitId)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habit_questions)

        fun recoverSavedState() {
            savedInstanceState?.run {
                habitId = this.getLong(HABIT_ID, Constants.IGNORE_ID_AS_LONG)
            }
        }

        fun getArgs() {
            intent?.run {
                habitId = getLongExtra(
                    HabitQuestioningContract.HABIT_ID_FROM_REQUESTER,
                    Constants.IGNORE_ID_AS_LONG)
            }
        }

        fun processArgsAndSavedState() {

            (application as App? )?.run {
                databaseObject = DatabaseObjectFactory.newDatabaseObject(this.driver)
            }
            viewModel = ViewModelProvider(this,
                HabitQuestionsViewModelFactory(databaseObject, habitId)
            )[HabitQuestionsViewModel::class.java]
        }


        fun prepareUI() {
            // title setting
            val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.habitToolbar)
            setSupportActionBar(toolbar)
            viewModel.mutableHabitData.observe(this) {
                val habitName = viewModel.mutableHabitData.value?.habitName
                    ?: getString(R.string.habits_name)
                toolbar.title = getString(R.string.habits_questioning, habitName)
            }

            val tabsPager: ViewPager2 = findViewById(R.id.habitQuestionViewPager)
            val tabs: TabLayout = findViewById(R.id.habitQuestionsTabs)

            tabsPager.isUserInputEnabled = false

            tabsPager.adapter = HabitQuestionsPageAdapter(this)

            TabLayoutMediator(tabs, tabsPager) {
                    tab, position -> tab.text = when (position) {
                QUESTIONS_TAB_ID -> getString(R.string.habits_question_tab)
                MARK_HABIT_TAB_ID -> getString(R.string.habits_marking_tab)
                else -> Constants.EMPTY_STRING
            }
            }.attach()

            onBackPressedDispatcher.addCallback(
                DoubleTapOnBack(this, getString(R.string.habits_will_take_defaults))
                    { viewModel.markHabitAs(true); onBackPressed() }
            )

            viewModel.shouldLeave.observe(this) { if (it) {
                doAfterDoneWithQuestions()
                finish()
            } }
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

    private inner class HabitQuestionsPageAdapter(fragAct: FragmentActivity):
        FragmentStateAdapter(fragAct) {
        override fun getItemCount(): Int = TAB_COUNT

        override fun createFragment(position: Int): Fragment {
            if (classNumber == null) return Fragment()

            return when (position) {
                QUESTIONS_TAB_ID -> {
                    HabitQuestionsFragment()
                }
                MARK_HABIT_TAB_ID -> {
                    HabitMarking()
                }
                else -> Fragment()
            }
        }
    }
}