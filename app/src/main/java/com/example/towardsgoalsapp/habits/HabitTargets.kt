package com.example.towardsgoalsapp.habits

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.INVALID_POSITION
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R


class HabitTargets : Fragment() {

    companion object {

        const val LOG_TAG = "HTrFr"

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

    private lateinit var rangeArray : ArrayList<Long>

    private lateinit var periodSeekBar : SeekBar
    private lateinit var targetCountSpinner: Spinner

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_habit_targets, container, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run { putBoolean(IS_EDIT, isEdit) }
        super.onSaveInstanceState(outState)
    }

    private fun createRangeArray(progress: Int, setDefaultOnFail: Boolean) : Long {
        val period: Long
        rangeArray = ArrayList<Long>()
        val correctProgress = if (progress > -1 && progress < Constants.periodsArray.size) {
            progress
        } else if (setDefaultOnFail) 0 else return PERIOD_OF_INVALID_PROGRESS
        period = Constants.periodsArray[correctProgress]
        val step = Constants.periodsStepArray[correctProgress]
        var st = step
        while (st <= period) {
            rangeArray.add(st)
            st += step
        }
        return period
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[HabitViewModel::class.java]

        arguments?.run { isEdit = getBoolean(IS_EDIT, isEdit) }

        periodSeekBar = view.findViewById(R.id.targetPeriodSeekBar)
        val periodTextView : TextView= view.findViewById(R.id.targetNumberTextView)
        targetCountSpinner = view.findViewById(R.id.targetCountSpinner)

        fun generateSeekBarUI(period: Long) {
            val dayz = if (period > 1)
                getString(R.string.habits_day_plural)
            else getString(R.string.habits_day)

            periodTextView.text = getString(R.string.habits_target_period_text,
                period, dayz)

            val newAdapter = ArrayAdapter(requireContext(),
                android.R.layout.simple_spinner_item, rangeArray)
            newAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            targetCountSpinner.adapter = newAdapter
        }

        periodSeekBar.max = Constants.periodsArray.size
        periodSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val period = createRangeArray(progress, false)
                if (period == PERIOD_OF_INVALID_PROGRESS) return
                generateSeekBarUI(period)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // not used
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // not used
            }

        })

        val prog = periodSeekBar.progress
        periodSeekBar.setProgress(-1, false)
        periodSeekBar.setProgress(prog, false)

        viewModel.targetNumbers.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            val count = it.first
            val period = it.second
            var periodIdx = Constants.periodsArray.indexOf(period)
            if (periodIdx == -1) periodIdx = 0
            var countIdx = INVALID_POSITION
            periodSeekBar.setProgress(periodIdx, false)
            try {
                countIdx = rangeArray.indexOf(count)
            }
            catch (e: UninitializedPropertyAccessException) {
                val newPeriod = createRangeArray(periodIdx, true)
                generateSeekBarUI(newPeriod)
                countIdx = rangeArray.indexOf(count)
            }
            finally {
                if (countIdx == INVALID_POSITION) countIdx = 0
            }
            Log.i(LOG_TAG, "targets as: ${rangeArray[countIdx]} as count of days"
            + " ${Constants.periodsArray[periodSeekBar.progress]} as period")
            targetCountSpinner.setSelection(countIdx)
        }

        if (! isEdit) {
            periodSeekBar.isEnabled = false
            targetCountSpinner.isEnabled = false
        }

    }

    override fun onPause() {
        if (isEdit) {
            val targetPeriod = Constants.periodsArray[periodSeekBar.progress]
            var tcidx = targetCountSpinner.selectedItemPosition
            if (tcidx == INVALID_POSITION) tcidx = 0
            val targetCount = rangeArray[tcidx]
            viewModel.putTargets(targetCount, targetPeriod)
        }
        super.onPause()
    }


}