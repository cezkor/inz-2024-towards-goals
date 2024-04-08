package com.example.towardsgoalsapp.tasks.details

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.tasks.TaskItemListFragment


class MultipleTaskFragment : Fragment() {

    companion object {
        const val LOG_TAG = "TaskMultiple"
        const val GOAL_ID_OF_TASK = "tmlgid"
        const val INHERIT_FROM_CLASS_NUMBER = "tmlicm"
        const val FRAG_TAG = "TaskMultiple_FRAG_TAG_399980012"

        @JvmStatic
        fun newInstance(gid: Long, inheritFromClass: Int) =
            MultipleTaskFragment().apply {
                arguments = Bundle().apply {
                    putLong(GOAL_ID_OF_TASK, gid)
                    putInt(INHERIT_FROM_CLASS_NUMBER, inheritFromClass)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_multiple_tasks, container, false)
    }

    private var subFragment = Fragment()

    private var goalId: Long = Constants.IGNORE_ID_AS_LONG
    private var classNumber: Int = Constants.CLASS_NUMBER_NOT_RECOGNIZED

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            goalId = it.getLong(GOAL_ID_OF_TASK, goalId)
            classNumber = it.getInt(INHERIT_FROM_CLASS_NUMBER, classNumber)
        }

        val radioGroup: RadioGroup = view.findViewById(R.id.taskViewRadioGroup)
        val fragmentContainerId = R.id.fragmentContainerFrameLayout

        val matrixFragment = TaskEisenhowerMatrix.newInstance(goalId, classNumber)
        val listFragment = TaskItemListFragment.newInstance(goalId, classNumber)

        fun replaceFragment() {
            childFragmentManager
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(fragmentContainerId, subFragment, FRAG_TAG)
                .commit()
        }

        radioGroup.setOnCheckedChangeListener { rg, id ->
            when (id) {
                R.id.taskShowListButton -> {
                    subFragment = listFragment
                    replaceFragment()
                }
                R.id.taskShowMatrixButton -> {
                    subFragment = matrixFragment
                    replaceFragment()
                }
                else -> {
                    // ignore
                }
            }
        }
        // begin with list of tasks
        radioGroup.check(R.id.taskShowListButton)

    }

}