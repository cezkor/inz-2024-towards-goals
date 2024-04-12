package org.cezkor.towardsgoalsapp.habits.params

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.etc.OneTextFragment
import org.cezkor.towardsgoalsapp.etc.OneTimeHandleable
import org.cezkor.towardsgoalsapp.etc.errors.ErrorHandling
import org.cezkor.towardsgoalsapp.habits.HabitViewModel
import kotlinx.coroutines.launch


class HabitTargets : Fragment() {

    companion object {

        const val LOG_TAG = "HTrFr"
        const val FRAG_TAG = "HabitTargetsHabitParametersFragmentTag-223123"

        private const val PERIOD_OF_INVALID_PROGRESS = -100L

        const val IS_EDIT = "htredit"

        fun newInstance(isEdit: Boolean) =
            HabitTargets().apply {
                arguments = Bundle().apply {
                    putBoolean(IS_EDIT, isEdit)
                }
            }

    }

    private lateinit var viewModel: HabitViewModel

    private var isEdit = false

    private var dayCount: Long = 0L

    private lateinit var periodSeekBar : SeekBar

    private lateinit var fragmentForContainer: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.run {
            isEdit = getBoolean(IS_EDIT, isEdit)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return if (isEdit)
            inflater.inflate(R.layout.fragment_editable_habit_targets, container, false)
        else
            inflater.inflate(R.layout.fragment_readonly_habit_targets, container, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run { putBoolean(IS_EDIT, isEdit) }
        super.onSaveInstanceState(outState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.run {
            isEdit = getBoolean(IS_EDIT, isEdit)
        }
        savedInstanceState?.run {
            isEdit = getBoolean(IS_EDIT, isEdit)
        }

        viewModel = ViewModelProvider(requireActivity())[HabitViewModel::class.java]

        periodSeekBar = view.findViewById(R.id.targetPeriodSeekBar)
        val periodTextView : TextView= view.findViewById(R.id.targetNumberTextView)
        val addParameterButton: Button = view.findViewById(R.id.addParameterButton)
        val paramContainerId = R.id.paramsContainer
        val noParamsFragment = OneTextFragment.newInstance(getString(R.string.habits_no_params))

        fun getPeriod(periodIdx: Int) : Long {
            return if (periodIdx > -1 && periodIdx < org.cezkor.towardsgoalsapp.Constants.periodsArray.size) {
                org.cezkor.towardsgoalsapp.Constants.periodsArray[periodIdx]
            } else PERIOD_OF_INVALID_PROGRESS
        }

        fun recreateLongFromText(text: String, default: Long) : Long {
            var numb: Long = default
            try {
                numb = text.toLong()
            }
            catch (e: NumberFormatException) {
                // ignore
            }
            return numb
        }

        fun fixSkipCount(skipCount: Long) : Long {
            val period = getPeriod(periodSeekBar.progress)
            if (period == PERIOD_OF_INVALID_PROGRESS) return 0L
            if (skipCount < 0) return 0L
            if (skipCount > period) return period
            return skipCount
        }

        fun fixDayCount(dayCount: Long) : Long {
            val period = getPeriod(periodSeekBar.progress)
            if (period == PERIOD_OF_INVALID_PROGRESS) return 1L
            if (dayCount < 1) return 1L
            if (dayCount > period) return period
            return dayCount
        }

        // check if should warn user about counts and warn if applicable
        fun warnUserIfPossibleAndNeeded(skipText: String, dayCountText: String) {
            if (! isEdit) return
            val warningTV = view.findViewById<TextView>(R.id.warningTextView)
            val period = getPeriod(periodSeekBar.progress)
            if (period == PERIOD_OF_INVALID_PROGRESS) return
            val skipCount = recreateLongFromText(skipText, 0)
            val dayCount = recreateLongFromText(dayCountText, dayCount)

            Log.i(LOG_TAG,"target count $dayCount, period $period, skip count $skipCount")
            if (dayCount + skipCount > period)
                warningTV.text = getString(
                    R.string.habits_counts_warning
                )
            else {
                warningTV.text = org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING
            }
        }

        fun setCounts(dayCount: Long) {
            if (isEdit) {
                val countEditText: EditText = view.findViewById(R.id.targetCountET)
                val skipCountEditText: EditText = view.findViewById(R.id.skipCountET)

                this.dayCount = fixDayCount(dayCount)
                countEditText.setText(this.dayCount.toString())
                if (skipCountEditText.text.toString() == org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING) {
                    val period = getPeriod(periodSeekBar.progress)
                    if (period == PERIOD_OF_INVALID_PROGRESS) {
                        skipCountEditText.setText("0")
                    } else {
                        skipCountEditText
                            .setText(fixSkipCount(period - dayCount).toString())
                    }
                }
            }
            else {
                val countTV : TextView= view.findViewById(R.id.targetCountROTV)
                countTV.text = dayCount.toString()
            }
        }

        fun fillContainer(noParams: Boolean) {
            fragmentForContainer = if (noParams)
                noParamsFragment
             else
                HabitParamItemList.newInstance(
                    ! isEdit,
                    org.cezkor.towardsgoalsapp.Constants.viewModelClassToNumber[HabitViewModel::class.java]
                        ?: org.cezkor.towardsgoalsapp.Constants.CLASS_NUMBER_NOT_RECOGNIZED
                )

            childFragmentManager.beginTransaction().run {
                setReorderingAllowed(true)
                replace(paramContainerId, fragmentForContainer, FRAG_TAG)
                commitNow()
            }
        }

        fun generateSeekBarUI(period: Long) {
            val dayz = if (period > 1)
                getString(R.string.habits_day_plural)
            else getString(R.string.habits_day)

            periodTextView.text = getString(R.string.habits_target_period_text,
                period,
                dayz)
        }

        periodSeekBar.max = org.cezkor.towardsgoalsapp.Constants.periodsArray.size - 1
        periodSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val period = getPeriod(progress)
                if (period == PERIOD_OF_INVALID_PROGRESS) return
                generateSeekBarUI(period)
                if (isEdit) {
                    val countEditText: EditText = view.findViewById(R.id.targetCountET)
                    val skipCountEditText: EditText = view.findViewById(R.id.skipCountET)
                    setCounts(recreateLongFromText(
                        countEditText.text.toString(), dayCount
                    ))
                    warnUserIfPossibleAndNeeded(
                        skipCountEditText.text.toString(),
                        countEditText.text.toString()
                    )
                }
                else {
                    setCounts(dayCount)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // not used
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // not used
            }

        })

        // to recreate text for textview using existing code
        val prog = periodSeekBar.progress
        if (prog == 0) {
            periodSeekBar.setProgress(1, false)
            if (isEdit) {
                val skipCountEditText: EditText = view.findViewById(R.id.skipCountET)
                skipCountEditText.setText(org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING)
                // force recalculation of skip days
            }
            periodSeekBar.setProgress(0, false)
        }
        else periodSeekBar.setProgress(prog, false)

        viewModel.targetNumbers.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            val count = it.first
            dayCount = count
            val period = it.second
            var periodIdx = org.cezkor.towardsgoalsapp.Constants.periodsArray.indexOf(period)
            if (periodIdx == -1) periodIdx = 0 // period not found => set to first period
            periodSeekBar.setProgress(periodIdx, false)

            if (isEdit) {
                // check if should warn user about counts
                val countEditText: EditText = view.findViewById(R.id.targetCountET)
                val skipCountEditText: EditText = view.findViewById(R.id.skipCountET)
                warnUserIfPossibleAndNeeded(
                    skipCountEditText.text.toString(),
                    countEditText.text.toString()
                )
            }
        }

        viewModel.mutableHabitData.observe(viewLifecycleOwner) {
            if (it != null) {
                if (! isEdit) {
                    val currentTV = view.findViewById<TextView>(R.id.targetCurrentROTV)
                    val markedWellTV = view.findViewById<TextView>(R.id.targetDoneWellROTV)
                    val markedNotWellTV = view.findViewById<TextView>(R.id.targetDoneNotWellROTV)
                    val skippedTV = view.findViewById<TextView>(R.id.targetSkippedROTV)
                    val current = (it.habitTotalCount + 1)
                    currentTV?.run { text = current.toString() }
                    markedWellTV?.run { text = it.habitDoneWellCount.toString() }
                    markedNotWellTV?.run { text = it.habitDoneNotWellCount.toString() }
                    var skipped =
                        it.habitTotalCount - it.habitDoneWellCount - it.habitDoneNotWellCount
                    if (skipped < 0) skipped = 0
                    skippedTV?.run { text = skipped.toString() }
                }
            }
        }

        if (! isEdit) {
            periodSeekBar.isEnabled = false
            addParameterButton.isEnabled = false
        }
        else {

            val countEditText: EditText = view.findViewById(R.id.targetCountET)
            val skipCountEditText: EditText = view.findViewById(R.id.skipCountET)

            fun fixCountEditText() {
                dayCount = fixDayCount(
                    recreateLongFromText(countEditText.text.toString(), dayCount)
                )
                val dctt = dayCount.toString()
                if (countEditText.text.toString() != dctt)
                    countEditText.setText(dctt)
            }

            fun fixSkipCountEditText(){
                val skipCount = fixSkipCount(
                    recreateLongFromText(skipCountEditText.text.toString(), 0L)
                )
                val sctt = skipCount.toString()
                if (skipCountEditText.text.toString() != sctt)
                    skipCountEditText.setText(sctt)

            }

            countEditText.setOnFocusChangeListener { v, hasFocus ->
                if (! hasFocus) {
                    fixCountEditText()
                    warnUserIfPossibleAndNeeded(
                        skipCountEditText.text.toString(),
                        countEditText.text.toString()
                    )
                }
            }
            countEditText.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) { // user stopped providing input
                    fixCountEditText()
                    warnUserIfPossibleAndNeeded(
                        skipCountEditText.text.toString(),
                        countEditText.text.toString()
                    )
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }
            skipCountEditText.setOnFocusChangeListener { v, hasFocus ->
                if (! hasFocus) {
                    fixSkipCountEditText()
                    warnUserIfPossibleAndNeeded(
                        skipCountEditText.text.toString(),
                        countEditText.text.toString()
                    )
                }
            }
            skipCountEditText.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    fixSkipCountEditText()
                    warnUserIfPossibleAndNeeded(
                        skipCountEditText.text.toString(),
                        countEditText.text.toString()
                    )
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }

            val trueSize = viewModel.getHabitParamsSharer()
                .getArrayOfUserData()?.filter { t -> t.value != null }?.size ?: 0
            if (trueSize >= org.cezkor.towardsgoalsapp.Constants.MAX_HABIT_PARAM_COUNT_PER_HABIT)
                addParameterButton.isEnabled = false
            else
                addParameterButton.setOnClickListener {
                    viewModel.assureOfExistingHabitHandleableMutable.value = OneTimeHandleable {
                        lifecycleScope.launch(viewModel.exceptionHandler) {
                            val wasEmpty = viewModel.getHabitParamsSharer().isArrayConsideredEmpty()
                            var ts : Int
                            if (!wasEmpty) {
                                ts = viewModel.getHabitParamsSharer()
                                    .getArrayOfUserData()?.filter { t -> t.value != null }?.size ?: 0
                                if (ts >= org.cezkor.towardsgoalsapp.Constants.MAX_HABIT_PARAM_COUNT_PER_HABIT) {
                                    ErrorHandling.showThrowableAsToast(
                                        requireActivity(),
                                        Throwable(getString(R.string.habits_no_more_params_allowed,
                                            org.cezkor.towardsgoalsapp.Constants.MAX_HABIT_PARAM_COUNT_PER_HABIT.toString()))
                                    )
                                    return@launch
                                }
                            }
                            viewModel.addOneNewEmptyHabitParam()
                            if (wasEmpty) { fillContainer(false) } // so that it is possible to display list
                            // instead of noParamsFragment
                            // decide whether to block adding parameters or not
                            ts = viewModel.getHabitParamsSharer()
                                .getArrayOfUserData()?.filter { t -> t.value != null }?.size ?: 0
                            addParameterButton.isEnabled =
                                ts < org.cezkor.towardsgoalsapp.Constants.MAX_HABIT_PARAM_COUNT_PER_HABIT
                        }
                    }
            }

            viewModel.getHabitParamsSharer().arrayState.observe(viewLifecycleOwner) {
                if (it == null) return@observe
                val sharer = viewModel.getHabitParamsSharer()
                if (sharer.isArrayConsideredEmpty()) fillContainer(true)
            }
        }

        fillContainer(viewModel.getHabitParamsSharer().isArrayConsideredEmpty())

    }

    override fun onPause() {
        if (isEdit) {
            val targetPeriod = org.cezkor.towardsgoalsapp.Constants.periodsArray[periodSeekBar.progress]
            Log.i(LOG_TAG,"going to put target count: $dayCount, period $targetPeriod")
            viewModel.putTargets(dayCount, targetPeriod)
        }
        super.onPause()
    }


}