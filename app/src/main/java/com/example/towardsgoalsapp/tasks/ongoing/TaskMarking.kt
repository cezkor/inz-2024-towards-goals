package com.example.towardsgoalsapp.tasks.ongoing

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.etc.OneTimeEvent
import com.example.towardsgoalsapp.etc.errors.ErrorHandling
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant


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

        val summaryTextView = view.findViewById<TextView>(R.id.taskDoingSummaryTV)
        if (viewModel.pomidoroIsOn) {
            val totalMinutes = viewModel.pomidoroState.sumTotalAndCurrentTimes()  / 60
            val totalWork = viewModel.pomidoroState.totalTimeOfWorkInSeconds / 60
            val totalBreak = viewModel.pomidoroState.totalTimeOfBreaksInSeconds / 60

            summaryTextView.text = getString(
                R.string.tasks_doing_summary,
                totalMinutes.toString(),
                totalWork.toString(),
                totalBreak.toString()
            )
        } else {
            val start = viewModel.startedDoingTaskOn
            val end = viewModel.endedDoingTaskOn
            val minutes =
                if (start != null && end != null) Duration.between(end, start).toMinutes() else 0L
            val totalTimeInMinutes = if (minutes >= 0) minutes else 0L
            summaryTextView.text = getString(
                R.string.tasks_doing_summary_if_not_pomidoro,
                totalTimeInMinutes.toString()
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