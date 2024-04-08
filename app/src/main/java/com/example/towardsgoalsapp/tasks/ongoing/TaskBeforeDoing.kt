package com.example.towardsgoalsapp.tasks.ongoing

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.stats.questions.DoubleValueQuestionItemList
import kotlinx.coroutines.launch


class TaskBeforeDoing : Fragment() {

    companion object {
        const val LOG_TAG = "TBDFrag"

        const val FRAG_TAG = "TBDPomidoro_FRAG_TAG_4124371"
    }

    private lateinit var viewModel: TaskOngoingViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_task_before_doing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[TaskOngoingViewModel::class.java]
        val classNum = Constants.viewModelClassToNumber[TaskOngoingViewModel::class.java]
            ?: Constants.CLASS_NUMBER_NOT_RECOGNIZED

        val descriptionTextView = view.findViewById<TextView>(R.id.descriptionTextView)
        viewModel.descriptionOfData.observe(viewLifecycleOwner) {
            descriptionTextView.text = it
        }

        val pomidoroContainerId = R.id.questionListContainer
        val pomidoroSettingsFragment =
            DoubleValueQuestionItemList.newInstance(classNum, true)

        val pomidoroToggleButton: ToggleButton = view.findViewById(R.id.togglePomidoro)
        pomidoroToggleButton.setOnCheckedChangeListener { buttonView, isChecked ->
            viewModel.pomidoroIsOn = isChecked
            val fragment: Fragment = if (isChecked) {
                pomidoroSettingsFragment
            } else
                Fragment()
            childFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(pomidoroContainerId, fragment, FRAG_TAG)
                .commitNow()
        }

        var taskNextPushed = false

        val taskNextButton : Button = view.findViewById(R.id.taskNext)
        taskNextButton.setOnClickListener {
            // requesting focus so keyboard gets hidden

            (requireActivity().application.getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager)
                .hideSoftInputFromWindow(
                    requireActivity().window.decorView.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
            it.requestFocusFromTouch()
            if (taskNextPushed) return@setOnClickListener
            taskNextPushed = true
            if (viewModel.pomidoroIsOn) {
                // force fragment to save questions
                val f = childFragmentManager.findFragmentByTag(FRAG_TAG)
                if (f is DoubleValueQuestionItemList)
                    f.forceSavingQuestions()
                // listen to questions being saved
                viewModel.pomidoroSettingsReadyToSave.observe(viewLifecycleOwner) {
                    it?.handleIfNotHandledWith {
                        lifecycleScope.launch(viewModel.exceptionHandler) {
                            viewModel.saveMainData()
                            // in state BEFORE_DOING_TASK, database is not used
                            // so it is not needed to check if saved data successfully
                            viewModel.mutableOfTaskOngoingStates.value =
                                TaskOngoingViewModel.TaskOngoingStates.DOING_TASK
                        }
                    }
                }
            }
            else {
                viewModel.mutableOfTaskOngoingStates.value =
                    TaskOngoingViewModel.TaskOngoingStates.DOING_TASK
            }
        }

    }

}