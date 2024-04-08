package com.example.towardsgoalsapp.tasks.ongoing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.etc.OneTimeEvent
import com.example.towardsgoalsapp.tasks.ongoing.TaskOngoingViewModel.Companion.LONG_BREAK_ID
import com.example.towardsgoalsapp.tasks.ongoing.TaskOngoingViewModel.Companion.SHORT_BREAK_ID
import com.example.towardsgoalsapp.tasks.ongoing.TaskOngoingViewModel.Companion.WORK_TIME_ID


class PomidoroFragment : Fragment() {

    companion object {
        const val LOG_TAG = "PomFrag"
    }

    private lateinit var viewModel: TaskOngoingViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_task_pomidoro, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val timerTextView = view.findViewById<TextView>(R.id.timeTextView)
        val stateTextView = view.findViewById<TextView>(R.id.pomidoroStateTextView)
        val stateSwitchButton: Button = view.findViewById(R.id.switchStateButton)

        fun updateUIAccordinglyToPomidoroState() {
            val idx = viewModel.pomidoroState.determineIndexBasedOnState()
            val additionalTime = viewModel.pomidoroTimerServiceStartedForTheSameStateAgain

            var textForState : String = when(idx) {
                WORK_TIME_ID -> {
                    getString(R.string.tasks_pomidoro_work_time_state)
                }

                LONG_BREAK_ID -> {
                    getString(R.string.tasks_pomidoro_long_break_state)
                }

                else -> {
                    getString(R.string.tasks_pomidoro_short_break_state)
                }
            }

            val additionalText = if (additionalTime) getString(R.string.extra)
                                 else Constants.EMPTY_STRING
            val time = if (additionalTime) Constants.ADDITIONAL_TIME.toLong()
                       else viewModel.pomidoroQuestions[idx]?.answer?.toLong() ?: 0L

            textForState = String.format("%s (%s) %s",
                textForState,
                getString(R.string.tasks_pomidoro_minutes, time.toString()),
                additionalText)
            stateTextView.text = textForState

            stateSwitchButton.text = if (idx == WORK_TIME_ID) getString(R.string.tasks_take_a_break)
                                     else getString(R.string.tasks_get_to_work)
        }

        viewModel = ViewModelProvider(requireActivity())[TaskOngoingViewModel::class.java]

        updateUIAccordinglyToPomidoroState()

        viewModel.pomidoroIsReady.observe(viewLifecycleOwner) {
            stateSwitchButton.isEnabled = it
        }
        viewModel.pomidoroTimeUpdate.observe(viewLifecycleOwner) {
            it?.handleIfNotHandledWith {
                viewModel.pomidoroState.moveTimeBy(it.value)
                val seconds = viewModel.pomidoroState.timeOfCurrentStateInSeconds
                // format seconds to minutes : seconds
                val lMM = seconds / 60
                val lSS = seconds % 60
                timerTextView.text = getString(R.string.tasks_time_format,
                    String.format("%02d", lMM),
                    String.format("%02d", lSS))
            }
        }
        viewModel.pomidoroStateChangeRequired.observe(viewLifecycleOwner) {
            it?.handleIfNotHandledWith {
                // alarm sound will be played by the TaskDoingTimingService
                var textForState : String = when(viewModel.pomidoroState.determineIndexBasedOnState()) {
                    WORK_TIME_ID -> {
                        getString(R.string.tasks_pomidoro_work_time_state)
                    }

                    LONG_BREAK_ID -> {
                        getString(R.string.tasks_pomidoro_long_break_state)
                    }

                    SHORT_BREAK_ID -> {
                        getString(R.string.tasks_pomidoro_short_break_state)
                    }
                    else -> Constants.EMPTY_STRING
                }
                Toast.
                makeText(context,
                    getString(R.string.tasks_pomidoro_stage_is_over, textForState),
                    Toast.LENGTH_LONG)
                .show()
            } }

        stateSwitchButton.setOnClickListener {
            viewModel.pomidoroState.switchState()
            updateUIAccordinglyToPomidoroState()
            viewModel.pomidoroStateChanged.value = OneTimeEvent()
        }

        timerTextView.text = getString(R.string.tasks_time_format, "00", "00")
    }

}