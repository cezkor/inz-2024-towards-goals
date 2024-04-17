package org.cezkor.towardsgoalsapp.habits.questioning

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import org.cezkor.towardsgoalsapp.etc.DoubleTapOnBack
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.database.DatabaseObjectFactory
import org.cezkor.towardsgoalsapp.database.TGDatabase
import org.cezkor.towardsgoalsapp.habits.HabitViewModel
import org.cezkor.towardsgoalsapp.habits.questioning.HabitQuestioningContract.Companion.HABIT_ID_TO_REFRESH_FOR_REQUESTER
import org.cezkor.towardsgoalsapp.main.App
import org.cezkor.towardsgoalsapp.stats.questions.Question
import org.cezkor.towardsgoalsapp.stats.questions.DoubleValueQuestionItemList
import org.cezkor.towardsgoalsapp.stats.questions.ViewModelWithDoubleValueQuestionList
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import org.cezkor.towardsgoalsapp.Constants.Companion.CLASS_NUMBER_NOT_RECOGNIZED
import org.cezkor.towardsgoalsapp.Constants.Companion.viewModelClassToNumber
import org.cezkor.towardsgoalsapp.etc.OneTextFragment
import org.cezkor.towardsgoalsapp.etc.OneTimeEvent
import org.cezkor.towardsgoalsapp.etc.OneTimeEventWithValue
import org.cezkor.towardsgoalsapp.etc.errors.ErrorHandling
import org.cezkor.towardsgoalsapp.habits.questioning.HabitQuestioningContract.Companion.HABIT_MARKING_BY_NOTIFICATION
import org.cezkor.towardsgoalsapp.etc.RefreshTypes
import org.cezkor.towardsgoalsapp.etc.ShouldRefreshUIBroadcastReceiver
import org.cezkor.towardsgoalsapp.reminders.ReminderService
import kotlinx.coroutines.CompletableDeferred
import java.time.Instant

class HabitQuestionsViewModelFactory(private val dbo: TGDatabase,
                                     private val habitId: Long,
                                     private val unitString: String
    ): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HabitQuestionsViewModel(dbo, habitId, unitString) as T
    }
}

class HabitQuestionsViewModel(private val dbo: TGDatabase,
                              private var habitId: Long,
                              private var unitString: String
    ):
    HabitViewModel(dbo, habitId, org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG), ViewModelWithDoubleValueQuestionList {

    companion object {
        const val LOG_TAG = "HQViewModel"
    }

    val shouldLeave = MutableLiveData<Boolean>(false)

    val skippedDaysWithoutMarking = MutableLiveData<OneTimeEventWithValue<Long>>()

    private val questionList = ArrayList<Question<Double>?>()

    private val paramIdToQuestionMap: HashMap<Long, Question<Double>> = HashMap()

    val habitNotPresent: MutableLiveData<OneTimeEvent> = MutableLiveData()

    private var habitDoneWell: Boolean = false
    private var habitDoneNotWell: Boolean = false
    var habitShouldBeMarkedNow: Boolean = false
        private set
    private var markedOn: Instant? = null
    var daysAutoSkipped: Boolean = false
        private set

   val questionSaveReady = MutableLiveData<OneTimeEventWithValue<Boolean>>()
    override fun getQuestionsReadyToSave(): MutableLiveData<OneTimeEventWithValue<Boolean>>
        = questionSaveReady

    fun habitShouldBeMarkedAs(isDoneWell: Boolean) {

        markedOn = Instant.now()
        habitDoneWell = isDoneWell
        habitDoneNotWell = ! isDoneWell
        habitShouldBeMarkedNow = true

    }

    fun skipHabit() {
        markedOn = Instant.now()
        habitDoneWell = false
        habitDoneNotWell = false
        habitShouldBeMarkedNow = true
    }

    override suspend fun saveMainData(): Boolean {
        if (! habitShouldBeMarkedNow) return false
        val goalId: Long = mutableHabitData.value?.goalId ?: return false

        // parameters should not be put if habit is skipped
        // => either it is done well or not well

        // first check if questions can be extracted
        if (habitDoneWell xor habitDoneNotWell) {
            for (k in paramIdToQuestionMap.keys) {
                val _q = paramIdToQuestionMap[k] ?: return false
            }
        }

        if (habitDoneWell && habitDoneNotWell) {
            Log.e(LOG_TAG, "State not allowed (both done well and done not well)")
            // not throwing exception as to allow existing code to close activity if it needs to
            return false
        }

        if (! habitDoneNotWell && ! habitDoneWell )
            habitRepo.skipHabit(habitId, markedOn!!)
        else if (habitDoneWell xor habitDoneNotWell)
            if (habitDoneWell)
                habitRepo.markHabitDoneWell(habitId,markedOn!!)
            else
                habitRepo.markHabitDoneNotWell(habitId, markedOn!!)

        statsDataRepo.putNewHabitStatsData(
            habitId,
            goalId,
            habitDoneWell,
            habitDoneNotWell,
            markedOn!!
        )

        if (habitDoneWell xor habitDoneNotWell) {
            for (k in paramIdToQuestionMap.keys) {
                val question = paramIdToQuestionMap[k] ?: return false
                habitParamsRepo.putValueOfParam(
                    k,
                    question.answer ?: question.defaultValue ?: 0.0,
                    markedOn!!
                )
            }
        }

        return true
    }

    override fun getQuestionList(): ArrayList<Question<Double>?> = questionList

    fun prepareQuestions() {
        paramIdToQuestionMap.clear()
        questionList.clear()

        for (paramMutable in arrayOfMutableHabitParams) {
            val param = paramMutable.value
            param?.run {
                var qText = this.name
                if (param.unit != null) {
                    qText += " " + String.format(unitString, param.unit)
                }
                val question = Question(qText, 0.0)
                questionList.add(question)
                paramIdToQuestionMap.put(param.paramId, question)
            }
        }
        questionsReady.value = true
    }

    val questionsReady = MutableLiveData<Boolean>(null)

    suspend fun checkForMissedDays() {
        if (daysAutoSkipped) return
        val skippedDays = habitRepo.autoSkipDaysWithoutMarkingIfApplicableOfHabit(habitId)
        if (skippedDays > 0) {
            skippedDaysWithoutMarking.value = OneTimeEventWithValue(skippedDays)
        }
        daysAutoSkipped = true
    }

}

typealias HabitQuestioningLauncher = ActivityResultLauncher<Long>

class HabitQuestioningContract: ActivityResultContract<Long, Long>() {

    companion object {
        const val HABIT_ID_FROM_REQUESTER = "hqfr"
        const val HABIT_ID_TO_REFRESH_FOR_REQUESTER = "hqtidtorfr"
        const val HABIT_MARKING_BY_NOTIFICATION = "hqn"
        const val LOG_TAG = "HQC"
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
            = intent?.getLongExtra(HABIT_ID_TO_REFRESH_FOR_REQUESTER, org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)
        Log.i(LOG_TAG, "got data $idToRefresh, is OK: ${resultCode == Activity.RESULT_OK}")
        return if (resultCode == Activity.RESULT_OK && idToRefresh != null)
            idToRefresh
        else
            org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG
    }
}

class HabitQuestions : AppCompatActivity() {

    companion object {
        const val LOG_TAG = "HabitQuestions"
        const val HABIT_ID = "hqchid"

        const val TAB_COUNT = 2
        const val QUESTIONS_TAB_ID = 0;
        const val MARK_HABIT_TAB_ID = 1;
    }

    private lateinit var viewModel: HabitQuestionsViewModel
    private val classNumber = viewModelClassToNumber[HabitQuestionsViewModel::class.java]

    private lateinit var lbm : LocalBroadcastManager

    private lateinit var databaseObject: TGDatabase

    private var habitId: Long = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG
    private var marked = false
    private var openedByNotification = false

    private lateinit var adapter: HabitQuestionsPageAdapter
    private lateinit var tabLayoutMediator: TabLayoutMediator
    private lateinit var tabsPager: ViewPager2

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.title_only_menu, menu)
        return true
    }

    override fun onRestoreInstanceState(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?
    ) {
        savedInstanceState?.run {
            habitId = this.getLong(HABIT_ID, org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)
        }

        super.onRestoreInstanceState(savedInstanceState, persistentState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            putLong(HABIT_ID, habitId)
        }
        super.onSaveInstanceState(outState)
    }

    private suspend fun doAfterDoneWithQuestions() : Boolean {
        // await for questions to be saved if possible
        // set activity result
        if (questionsFragment is DoubleValueQuestionItemList) {
            val def = CompletableDeferred<Boolean>()
            fun runCoroutine() {
                lifecycleScope.launch {
                    val saved = viewModel.saveMainData()
                    if (saved || viewModel.daysAutoSkipped)
                        setResult(Activity.RESULT_OK, intent.apply {
                            putExtra(HABIT_ID_TO_REFRESH_FOR_REQUESTER, habitId)
                        })
                    def.complete(saved)
                }
            }
            if (viewModel.getQuestionList().isNotEmpty()) {
                (questionsFragment as DoubleValueQuestionItemList).forceSavingQuestions()
                viewModel.questionSaveReady.observe(this) {
                    it?.handleIfNotHandledWith {
                        runCoroutine()
                    }
                }
            }
            else { // no questions to save
                runCoroutine()
            }
            return def.await()
        }
        Log.e(LOG_TAG, "questions fragment is not DoubleValueQuestionItemList")
        return false
    }


    private fun informReminderServiceAboutLeaving(marked: Boolean) {
        lbm.sendBroadcast(
            Intent(ReminderService.TASK_OR_HABIT_ONGOING_LEFT_INTENT_FILTER)
                .putExtra(ReminderService.REMINDER_ID, viewModel.reminderId)
                .putExtra(ReminderService.HAS_BEEN_MARKED, marked)
        )
    }

    private fun informReminderServiceAboutOngoing() {
        lbm.sendBroadcast(
            Intent(ReminderService.TASK_OR_HABIT_BEING_ONGOING_INTENT_FILTER)
        )
    }

    override fun onDestroy() {
        if (isFinishing) {
            val goalId = viewModel.mutableHabitData.value?.goalId ?: org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG
            informReminderServiceAboutLeaving(marked)
            if (openedByNotification) {
                lbm.sendBroadcast(
                    ShouldRefreshUIBroadcastReceiver.createIntent(
                        goalId, RefreshTypes.GOAL
                    )
                )
                lbm.sendBroadcast(
                    ShouldRefreshUIBroadcastReceiver.createIntent(
                        habitId, RefreshTypes.HABIT
                    )
                )
            }
        }
        super.onDestroy()
    }

    private fun recreateAdapter() {
        tabLayoutMediator.detach()
        tabsPager.adapter = HabitQuestionsPageAdapter(this)
        tabLayoutMediator.attach()
    }

    override fun onResume() {
        super.onResume()
        informReminderServiceAboutOngoing()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habit_questions)

        lbm = LocalBroadcastManager.getInstance(this)

        fun recoverSavedState() {
            savedInstanceState?.run {
                habitId = this.getLong(HABIT_ID, org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)
            }
        }

        fun getArgs() {
            habitId = intent.getLongExtra(
                HabitQuestioningContract.HABIT_ID_FROM_REQUESTER,
                org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)
            openedByNotification =
                intent.getBooleanExtra(HABIT_MARKING_BY_NOTIFICATION, false)
        }

        fun processArgsAndSavedState() {

            (application as App? )?.run {
                databaseObject = DatabaseObjectFactory.newDatabaseObject(this.driver)
            }
            viewModel = ViewModelProvider(this,
                HabitQuestionsViewModelFactory(
                    databaseObject, habitId, getString(R.string.habits_params_unit_is_in)
                ) )[HabitQuestionsViewModel::class.java]
        }


        fun prepareUI() {
            viewModel.habitNotPresent.observe(this) {
                it.handleIfNotHandledWith {
                    this@HabitQuestions.setResult(RESULT_CANCELED)
                    finish()
                }
            }

            viewModel.exceptionMutable.observe(this) {
                ErrorHandling.showExceptionDialog(this, it)
            }

            viewModel.skippedDaysWithoutMarking.observe(this) {
                it?.run { this.handleIfNotHandledWith {
                    val s = this.value
                    Toast.makeText(
                        this@HabitQuestions,
                        getString(R.string.habits_you_missed_days, s),
                        Toast.LENGTH_LONG
                    ).show()
                } }
            }

            // title setting
            val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.habitToolbar)
            setSupportActionBar(toolbar)
            viewModel.mutableHabitData.observe(this) {
                val habitName = viewModel.mutableHabitData.value?.habitName
                    ?: getString(R.string.habits_name)
                toolbar.title = getString(R.string.habits_questioning, habitName)
            }

            tabsPager= findViewById(R.id.habitQuestionViewPager)
            val tabs: TabLayout = findViewById(R.id.habitQuestionsTabs)

            tabsPager.isUserInputEnabled = false

            adapter = HabitQuestionsPageAdapter(this)
            tabsPager.adapter = adapter
            tabLayoutMediator = TabLayoutMediator(tabs, tabsPager) {
                    tab, position -> tab.text = when (position) {
                QUESTIONS_TAB_ID -> getString(R.string.habits_parameters)
                MARK_HABIT_TAB_ID -> getString(R.string.habits_marking_tab)
                else -> org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING
            } }
            tabLayoutMediator.attach()

            onBackPressedDispatcher.addCallback(
                DoubleTapOnBack(this, getString(R.string.habits_abandon_habit)) {
                    finish()
                }
            )

            viewModel.shouldLeave.observe(this) { if (it) {

                lifecycleScope.launch(viewModel.exceptionHandler) {
                    if (doAfterDoneWithQuestions()) runOnUiThread {
                        marked = true
                        finish()
                    }
                    else runOnUiThread {
                        ErrorHandling.showThrowableAsToast(
                            this@HabitQuestions,
                            Throwable(getString(R.string.failed_to_save_data_cancelling))
                        )
                        finish()
                    }
                }
            } }

            viewModel.questionsReady.observe(this) {
                if (it == null) return@observe
                if (it) {
                // recreate and select questions tab
                questionsFragment = DoubleValueQuestionItemList.newInstance(
                    viewModelClassToNumber[HabitQuestionsViewModel::class.java]
                        ?: CLASS_NUMBER_NOT_RECOGNIZED
                )
                recreateAdapter()
            } }
        }

        fun doInThisOrder() {

            recoverSavedState()

            getArgs()

            processArgsAndSavedState()

            prepareUI()

            lifecycleScope.launch(viewModel.exceptionHandler) { repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.checkForMissedDays()
                viewModel.getEverything()
                viewModel.prepareQuestions()
            } }

        }; doInThisOrder()

    }

    private var questionsFragment : Fragment? = null

    private inner class HabitQuestionsPageAdapter(fragAct: FragmentActivity):
        FragmentStateAdapter(fragAct) {
        override fun getItemCount(): Int = TAB_COUNT

        private val noQuestions =
            OneTextFragment.newInstance(getString(R.string.habits_no_params_to_question))

        override fun createFragment(position: Int): Fragment {
            if (classNumber == null) return Fragment()

            return when (position) {
                QUESTIONS_TAB_ID -> {
                    if (viewModel.getQuestionList().isEmpty())
                        noQuestions
                    else
                        questionsFragment ?: noQuestions
                }
                MARK_HABIT_TAB_ID -> {
                    HabitMarking()
                }
                else -> Fragment()
            }
        }
    }
}