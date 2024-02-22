package com.example.towardsgoalsapp.tasks.ongoing

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import com.example.towardsgoalsapp.R
import kotlinx.coroutines.launch


class TaskQuestionSecond : Fragment() {

    companion object {

    }

    private lateinit var viewModel: TaskOngoingViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_task_questions2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[TaskOngoingViewModel::class.java]

        val taskNextButton : Button = view.findViewById(R.id.taskNext)
        taskNextButton.setOnClickListener {

            // put data into view model from ui elements

            lifecycleScope.launch {
                if (viewModel.saveMainData())
                    viewModel.mutableOfTaskOngoingStates.value =
                        TaskOngoingViewModel.TaskOngoingStates.FINISHED
            }
        }

    }

}