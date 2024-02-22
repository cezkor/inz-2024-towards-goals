package com.example.towardsgoalsapp.tasks.ongoing

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.OwnerType
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.impints.ImpIntItemList
import kotlinx.coroutines.launch


class TaskDoing : Fragment() {



    companion object {

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

        val markDoneButton : Button = view.findViewById(R.id.taskMarkDone)
        val markFailedButton : Button = view.findViewById(R.id.taskMarkFailed)

        viewModel.mutableTaskData.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            val impIntsFragment = ImpIntItemList.newInstance(it.taskId,
                OwnerType.TYPE_TASK, classNum, true)
            impIntsFragment.run {
                childFragmentManager.beginTransaction()
                    .replace(R.id.impIntsContainer, this)
                    .setReorderingAllowed(true)
                    .commit()
            }
        }

        markDoneButton.setOnClickListener {
            lifecycleScope.launch {
                viewModel.taskFailed.value = false
                viewModel.saveMainData()
                viewModel.mutableOfTaskOngoingStates.value =
                    TaskOngoingViewModel.TaskOngoingStates.SECOND_QUESTIONS
            }
        }

        markFailedButton.setOnClickListener {
            lifecycleScope.launch {
                viewModel.taskFailed.value = true
                viewModel.saveMainData()
                viewModel.mutableOfTaskOngoingStates.value =
                    TaskOngoingViewModel.TaskOngoingStates.SECOND_QUESTIONS
            }
        }


    }

}