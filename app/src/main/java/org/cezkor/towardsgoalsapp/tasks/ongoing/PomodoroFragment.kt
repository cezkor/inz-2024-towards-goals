package org.cezkor.towardsgoalsapp.tasks.ongoing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.etc.AndroidContextTranslation
import org.cezkor.towardsgoalsapp.etc.OneTimeEvent
import org.cezkor.towardsgoalsapp.etc.SecondsFormatting
import org.cezkor.towardsgoalsapp.tasks.ongoing.TaskOngoingViewModel.Companion.LONG_BREAK_ID
import org.cezkor.towardsgoalsapp.tasks.ongoing.TaskOngoingViewModel.Companion.SHORT_BREAK_ID
import org.cezkor.towardsgoalsapp.tasks.ongoing.TaskOngoingViewModel.Companion.WORK_TIME_ID
import kotlinx.coroutines.launch


class PomodoroFragment : Fragment() {

    companion object {
        const val LOG_TAG = "PomFrag"
    }

    private lateinit var viewModel: TaskOngoingViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_task_pomodoro, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val timerTextView = view.findViewById<TextView>(R.id.currentTimeTextView)
        val totalTimeTextView = view.findViewById<TextView>(R.id.totalTimeTextView)
        val stateTextView = view.findViewById<TextView>(R.id.pomodoroStateTextView)
        val stateSwitchButton: Button = view.findViewById(R.id.switchStateButton)

        val translation = AndroidContextTranslation(requireContext())

        // setting up timers may last for a period long enough for user to notice
        // uninitialized text
        // setting TextViews to show time of 0 seconds
        timerTextView.text = SecondsFormatting.formatSeconds(translation, 0)
        totalTimeTextView.text = getString(R.string.tasks_total_time,
            SecondsFormatting.formatSeconds(translation, 0))

        suspend fun updateUIAccordinglyToPomodoroState() {
            val idx = viewModel.pomodoroState.determineIndexBasedOnState()
            val additionalTime = viewModel.pomodoroTimerTickingForTheSameState.get()

            var textForState : String = when(idx) {
                WORK_TIME_ID -> {
                    getString(R.string.tasks_pomodoro_work_time_state)
                }

                LONG_BREAK_ID -> {
                    getString(R.string.tasks_pomodoro_long_break_state)
                }

                else -> {
                    getString(R.string.tasks_pomodoro_short_break_state)
                }
            }

            val additionalText = if (additionalTime) getString(R.string.extra)
                                 else org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING
            val time = if (additionalTime) org.cezkor.towardsgoalsapp.Constants.ADDITIONAL_TIME.toLong()
                       else viewModel.pomodoroQuestions[idx]?.answer?.toLong() ?: 0L

            textForState = String.format("%s (%s) %s",
                textForState,
                getString(R.string.tasks_pomodoro_minutes, time.toString()),
                additionalText)
            stateTextView.text = textForState

            stateSwitchButton.text = if (idx == WORK_TIME_ID) getString(R.string.tasks_take_a_break)
            else getString(R.string.tasks_get_to_work)
        }

        viewModel = ViewModelProvider(requireActivity())[TaskOngoingViewModel::class.java]

        viewModel.pomodoroTimeUpdate.observe(viewLifecycleOwner) {
            it?.handleIfNotHandledWith {
                lifecycleScope.launch {
                    viewModel.pomodoroState.moveTimeBy(it.value)
                    val seconds = viewModel.pomodoroState.getTimeOfCurrentState()
                    val totalSeconds = viewModel.pomodoroState.sumTotalAndCurrentTimes()
                    timerTextView.text = SecondsFormatting.formatSeconds(translation, seconds)
                    totalTimeTextView.text = getString(R.string.tasks_total_time,
                        SecondsFormatting.formatSeconds(translation, totalSeconds))
                }
            }
        }
        viewModel.pomodoroStateChangeRequired.observe(viewLifecycleOwner) {
            it?.handleIfNotHandledWith {
                // alarm sound will be played by the TaskDoingTimingService
                lifecycleScope.launch {
                    // don't remind the user about changing state of pomodoro if they already
                    // clicked it
                    if (viewModel.pomodoroStateChangedByUser.get()) return@launch
                    var textForState : String = when(viewModel.pomodoroState.determineIndexBasedOnState()) {
                        WORK_TIME_ID -> {
                            getString(R.string.tasks_pomodoro_work_time_state)
                        }

                        LONG_BREAK_ID -> {
                            getString(R.string.tasks_pomodoro_long_break_state)
                        }

                        SHORT_BREAK_ID -> {
                            getString(R.string.tasks_pomodoro_short_break_state)
                        }
                        else -> org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING
                    }
                    Toast.
                    makeText(context,
                        getString(R.string.tasks_pomodoro_stage_is_over, textForState),
                        Toast.LENGTH_LONG)
                        .show()

                    viewModel.pomodoroTimerTickingForTheSameState.set(true)
                    viewModel.pomodoroStartTicking.value = OneTimeEvent()
                    updateUIAccordinglyToPomodoroState()
                }
            } }

        stateSwitchButton.setOnClickListener {
            stateSwitchButton.isEnabled = false
            lifecycleScope.launch {
                viewModel.pomodoroStateChangedByUser.set(true)
                viewModel.pomodoroState.switchState()
                // set text of timer to 00:00 as we changed state
                timerTextView.text = SecondsFormatting.formatSeconds(translation, 0)
                viewModel.pomodoroTimerTickingForTheSameState.set(false)
                updateUIAccordinglyToPomodoroState()
                viewModel.pomodoroStartTicking.value = OneTimeEvent()
                viewModel.pomodoroStateChangedByUser.set(false)
                stateSwitchButton.isEnabled = true
            }
        }

        viewModel.pomodoroIsReady.observe(viewLifecycleOwner) {
            val ready = it?: false
            stateSwitchButton.isEnabled = ready
            if (ready)
                lifecycleScope.launch {
                    viewModel.pomodoroStartTicking.value = OneTimeEvent()
                    updateUIAccordinglyToPomodoroState()
                }
        }
    }

}