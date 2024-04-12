package org.cezkor.towardsgoalsapp.tasks.details

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.etc.TextsFragment


class TaskTextsAndPriority : Fragment() {

    companion object {

        const val IS_EDIT = "ttapie"
        const val CLASS_NUMBER = "ttapcn"

        @JvmStatic
        fun newInstance(isEdit: Boolean, classNumber: Int) = TaskTextsAndPriority().apply {
            arguments = Bundle().apply {
                putInt(CLASS_NUMBER,classNumber)
                putBoolean(IS_EDIT, isEdit)
            }
        }
    }

    private lateinit var viewModel: TaskDetailsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_task_texts_and_priority, container, false)
    }

    private var isEdit: Boolean = false
    private var classNumber: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[TaskDetailsViewModel::class.java]

        arguments?.run {
            isEdit = this.getBoolean(IS_EDIT, isEdit)
            classNumber = this.getInt(CLASS_NUMBER)
        }

        classNumber?.run {
            childFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.textsContainer, TextsFragment.newInstance(isEdit, this))
                .commit()
        }

        val pSeekBar : SeekBar = view.findViewById(R.id.taskPrioritySeekbar)
        val pText : TextView = view.findViewById(R.id.taskPriorityLabelTextView)

        val priorityToLabelMap = mapOf<Int, String>(
            0 to getString(R.string.tasks_priority_least),
            1 to getString(R.string.tasks_priority_quite),
            2 to getString(R.string.tasks_priority_significant),
            3 to getString(R.string.tasks_priority_most)
        )

        pSeekBar.max = org.cezkor.towardsgoalsapp.Constants.MAX_PRIORITY
        pSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val priorityLabel = priorityToLabelMap[progress] ?: return
                pText.text = priorityLabel
                if (fromUser) viewModel.priority = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // not used
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // not used
            }

        })
        // setting progress accordingly to priority value
        val p = viewModel.priority
        if (p == 0) {
            // because by default seekbar has value 0, setting it again to 0 won't trigger
            // the change listener
            pSeekBar.setProgress(1,false)
        }
        pSeekBar.setProgress(p, false)

        pSeekBar.isEnabled = isEdit

        viewModel.mutableTaskData.observe(viewLifecycleOwner) {
            val p = it?.taskPriority
            p?.run { pSeekBar.setProgress(p, false) }
        }

    }

}