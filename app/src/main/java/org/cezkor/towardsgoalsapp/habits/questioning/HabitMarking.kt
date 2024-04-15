package org.cezkor.towardsgoalsapp.habits.questioning

import android.graphics.Typeface
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.etc.OneTextFragment
import org.cezkor.towardsgoalsapp.impints.ImpIntItemList


class HabitMarking : Fragment() {

    companion object {
        const val LOG_TAG = "HMFr"
    }

    private lateinit var viewModel: HabitQuestionsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_habit_marking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[HabitQuestionsViewModel::class.java]
        val classNum = org.cezkor.towardsgoalsapp.Constants.viewModelClassToNumber[HabitQuestionsViewModel::class.java]
            ?: org.cezkor.towardsgoalsapp.Constants.CLASS_NUMBER_NOT_RECOGNIZED

        val targetText = view.findViewById<TextView>(R.id.targetTextView)
        val markDoneWellButton = view.findViewById<Button>(R.id.habitMarkDoneWell)
        val markDoneNotWellButton = view.findViewById<Button>(R.id.habitMarkNotDoneWell)
        val skipButton = view.findViewById<Button>(R.id.habitSkipButton)
        val descriptionTextView: TextView = view.findViewById(R.id.descriptionTextView)
        val nameTextView: TextView = view.findViewById(R.id.nameTextView)

        var buttonPushed: Boolean = false

        markDoneWellButton.setOnClickListener {
            if (!buttonPushed) {
                buttonPushed = true
                viewModel.habitShouldBeMarkedAs(true)
                markDoneWellButton.isEnabled = false
                viewModel.shouldLeave.value = true

            }
        }
        markDoneNotWellButton.setOnClickListener {
            if (!buttonPushed) {
                buttonPushed = true
                viewModel.habitShouldBeMarkedAs(false)
                markDoneNotWellButton.isEnabled = false
                viewModel.shouldLeave.value = true
            }
        }
        skipButton.setOnClickListener {
            if (!buttonPushed) {
                buttonPushed = true
                viewModel.skipHabit()
                skipButton.isEnabled = false
                viewModel.shouldLeave.value = true
            }
        }

        viewModel.mutableHabitData.observe(viewLifecycleOwner) { h ->
            if (h == null) return@observe

            val description = h.habitDescription
            if (description.isBlank()) {
                descriptionTextView.typeface = Typeface.defaultFromStyle(Typeface.ITALIC)
                descriptionTextView.text = getString(R.string.no_description)
            }
            else {
                descriptionTextView.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
                descriptionTextView.text = description
            }
            val name = h.habitName
            if (name.isBlank()) {
                nameTextView.typeface = Typeface.defaultFromStyle(Typeface.ITALIC)
                nameTextView.text = getString(R.string.habits_name)
            }
            else {
                nameTextView.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
                nameTextView.text = name
            }

            targetText.text = getString(
                R.string.habits_about_target,
                h.habitTotalCount + 1, h.habitTargetPeriod
            )
            viewModel.getImpIntsSharer().arrayState.observe(viewLifecycleOwner) {
                val impIntsFragment = if (viewModel.getHabitParamsSharer().isArrayConsideredEmpty())
                    OneTextFragment.newInstance(getString(R.string.impints_no_impints))
                else
                    ImpIntItemList.newInstance(
                        h.habitId,
                        org.cezkor.towardsgoalsapp.OwnerType.TYPE_HABIT, classNum, true
                    )

                childFragmentManager.beginTransaction()
                    .replace(R.id.impIntsContainer, impIntsFragment)
                    .setReorderingAllowed(true)
                    .commit()

            }
        }
    }

}