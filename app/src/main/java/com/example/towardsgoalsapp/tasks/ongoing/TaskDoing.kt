package com.example.towardsgoalsapp.tasks.ongoing

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.OwnerType
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.etc.OneTextFragment
import com.example.towardsgoalsapp.impints.ImpIntItemList


class TaskDoing : Fragment() {

    companion object {
        const val LOG_TAG = "TaskDoing"
        const val FRAG_TAG = "taskdoing_FRAG_TAG_23231445"
    }

    private lateinit var viewModel: TaskOngoingViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_task_doing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[TaskOngoingViewModel::class.java]
        val classNum = Constants.viewModelClassToNumber[TaskOngoingViewModel::class.java]
            ?: Constants.CLASS_NUMBER_NOT_RECOGNIZED

        val pButton = view.findViewById<Button>(R.id.proceedButton)

        viewModel.mutableTaskData.observe(viewLifecycleOwner) { t ->
            if (t == null) return@observe
            viewModel.getImpIntsSharer().arrayState.observe(viewLifecycleOwner) {
                val impIntsFragment = if (viewModel.getImpIntsSharer().isArrayConsideredEmpty())
                    OneTextFragment.newInstance(getString(R.string.impints_no_impints))
                else
                    ImpIntItemList.newInstance(t.taskId,
                        OwnerType.TYPE_TASK, classNum, true)
                childFragmentManager.beginTransaction()
                    .replace(R.id.impIntsContainer, impIntsFragment)
                    .setReorderingAllowed(true)
                    .commit()
            }

            val pomoFrag = if (viewModel.pomodoroIsOn) PomodoroFragment()
                        else
                            OneTextFragment.newInstance(getString(R.string.tasks_pomodoro_disabled))

            childFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.pomodoroContainer, pomoFrag, FRAG_TAG)
                .commit()
        }

        pButton.setOnClickListener {
            // switch to marking tasks
            viewModel.mutableOfTaskOngoingStates.value = TaskOngoingViewModel.TaskOngoingStates.MARKING_TASK
        }
    }


}