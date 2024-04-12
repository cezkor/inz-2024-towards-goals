package org.cezkor.towardsgoalsapp.tasks.ongoing

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.etc.AndroidContextTranslation
import org.cezkor.towardsgoalsapp.etc.OneTimeEvent
import org.cezkor.towardsgoalsapp.etc.SecondsFormatting
import org.cezkor.towardsgoalsapp.etc.errors.ErrorHandling
import kotlinx.coroutines.launch
import java.time.Duration


class TaskMarking : Fragment() {

    companion object {
        const val LOG_TAG ="TaskMark"
    }

    private lateinit var viewModel: TaskOngoingViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_task_marking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[TaskOngoingViewModel::class.java]

        val translation = AndroidContextTranslation(requireContext())

        val summaryTextView = view.findViewById<TextView>(R.id.taskDoingSummaryTV)
        if (viewModel.pomodoroIsOn) {
            lifecycleScope.launch {
                val totalTime = viewModel.pomodoroState.sumTotalAndCurrentTimes()
                var totalWork = viewModel.pomodoroState.getTotalTimeOfWork()
                var totalBreak = viewModel.pomodoroState.getTotalTimeOfBreaks()
                if (viewModel.pomodoroState.isBreak()) {
                    totalBreak += viewModel.pomodoroState.getTimeOfCurrentState()
                }
                else {
                    totalWork += viewModel.pomodoroState.getTimeOfCurrentState()
                }

                summaryTextView.text = getString(
                    R.string.tasks_doing_summary,
                    SecondsFormatting.formatSeconds(translation, totalTime),
                    SecondsFormatting.formatSeconds(translation, totalWork),
                    SecondsFormatting.formatSeconds(translation, totalBreak)
                )
            }
        } else {
            val start = viewModel.startedDoingTaskOn
            val end = viewModel.endedDoingTaskOn
            val seconds =
                if (start != null && end != null) Duration.between(end, start).seconds else 0L
            val totalTimeInSeconds = if (seconds >= 0L) seconds else 0L
            summaryTextView.text = getString(
                R.string.tasks_doing_summary_if_not_pomodoro,
                SecondsFormatting.formatSeconds(translation, totalTimeInSeconds)
            )
        }

        val markDoneButton : Button = view.findViewById(R.id.taskMarkDone)
        val markFailedButton : Button = view.findViewById(R.id.taskMarkFailed)

        markDoneButton.setOnClickListener {
            lifecycleScope.launch(viewModel.exceptionHandler) {
                viewModel.taskFailed.value = false
                if (viewModel.saveMainData())
                    viewModel.mutableOfTaskOngoingStates.value =
                        TaskOngoingViewModel.TaskOngoingStates.FINISHED
                else {
                    ErrorHandling.showThrowableAsToast(
                        requireActivity(),
                        Throwable(getString(R.string.failed_to_save_data) + ". "
                                + getString(R.string.tap_back_to_leave_and_not_save))
                    )
                    viewModel.onFailedToSaveData.value = OneTimeEvent()
                }
            }
        }

        markFailedButton.setOnClickListener {
            lifecycleScope.launch(viewModel.exceptionHandler) {
                viewModel.taskFailed.value = true
                if (viewModel.saveMainData())
                    viewModel.mutableOfTaskOngoingStates.value =
                        TaskOngoingViewModel.TaskOngoingStates.FINISHED
                else {
                    ErrorHandling.showThrowableAsToast(
                        requireActivity(),
                        Throwable(getString(R.string.failed_to_save_data) + ". "
                                + getString(R.string.tap_back_to_leave_and_not_save))
                    )
                    viewModel.onFailedToSaveData.value = OneTimeEvent()
                }

            }
        }

    }

}