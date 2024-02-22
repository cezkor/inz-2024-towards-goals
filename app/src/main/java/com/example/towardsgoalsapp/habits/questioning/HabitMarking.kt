package com.example.towardsgoalsapp.habits.questioning

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.OwnerType
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.impints.ImpIntItemList


class HabitMarking : Fragment() {

    companion object {
        const val LOG_TAG = "HMFr"
    }

    private lateinit var viewModel: HabitQuestionsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_habit_questions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[HabitQuestionsViewModel::class.java]
        val classNum = Constants.viewModelClassToNumber[HabitQuestionsViewModel::class.java]
            ?: Constants.CLASS_NUMBER_NOT_RECOGNIZED

        val targetText = view.findViewById<TextView>(R.id.targetTextView)
        val markDoneWellButton = view.findViewById<Button>(R.id.habitMarkDoneWell)
        val markDoneNotWellButton = view.findViewById<Button>(R.id.habitMarkNotDoneWell)
        val skipButton = view.findViewById<Button>(R.id.habitSkipButton)

        var impIntsFragment: ImpIntItemList?

        markDoneWellButton.setOnClickListener {
            if (markDoneWellButton.isEnabled && markDoneNotWellButton.isEnabled && skipButton.isEnabled) {
                viewModel.markHabitAs(true)
                markDoneWellButton.isEnabled = false
                viewModel.shouldLeave.value = true
            }
        }
        markDoneNotWellButton.setOnClickListener {
            if (markDoneWellButton.isEnabled && markDoneNotWellButton.isEnabled && skipButton.isEnabled) {
                viewModel.markHabitAs(false)
                markDoneNotWellButton.isEnabled = false
                viewModel.shouldLeave.value = true
            }
        }
        skipButton.setOnClickListener {
            if (markDoneWellButton.isEnabled && markDoneNotWellButton.isEnabled && skipButton.isEnabled) {
                viewModel.skipHabit()
                skipButton.isEnabled = false
                viewModel.shouldLeave.value = true
            }
        }

        viewModel.mutableHabitData.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            targetText.text = getString(R.string.habits_about_target,
                it.habitTotalCount + 1 , it.habitTargetPeriod)
            impIntsFragment = ImpIntItemList.newInstance(it.habitId,
                OwnerType.TYPE_HABIT, classNum, true)
            impIntsFragment?.run {
                childFragmentManager.beginTransaction()
                    .replace(R.id.impIntsContainer, this)
                    .setReorderingAllowed(true)
                    .commit()
            }
        }


    }

}