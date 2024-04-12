package org.cezkor.towardsgoalsapp.goals

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.database.HabitData
import org.cezkor.towardsgoalsapp.habits.params.HabitStatsFragment
import org.cezkor.towardsgoalsapp.tasks.TaskStatsFragment
import com.google.android.material.button.MaterialButton

class GoalStatsFragment : Fragment() {

    companion object {

        const val LOG_TAG = "GoalStFrag"
        const val FRAG_TAG = "GoalStatsFrag_FRAG_TAG_298392083"

    }

    private lateinit var viewModel: GoalViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_goal_stats, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[GoalViewModel::class.java]

        val pickButton: MaterialButton = view.findViewById(R.id.pickATaskOrHabit)
        val fragmentContainerId = R.id.fragmentContainer

        viewModel.isInPickingTasksOrHabitForStats.observe(viewLifecycleOwner) {
            pickButton.isEnabled = (! it)
            val fragment: Fragment = if (! it) {
                val pickedData = viewModel.dataPicked
                if (pickedData == null) Fragment()
                when (pickedData) {
                    is HabitData -> {
                        HabitStatsFragment.newInstance(pickedData.habitId)
                    }
                    is Long -> {
                        // assume its goal id
                        TaskStatsFragment.newInstance(pickedData)
                    }
                    else -> Fragment()
                }
            } else PickingFragment()
            childFragmentManager.beginTransaction()
                .replace(fragmentContainerId, fragment, FRAG_TAG)
                .setReorderingAllowed(true)
                .commit()
        }

        pickButton.setOnClickListener {
            viewModel.isInPickingTasksOrHabitForStats.value = true
        }

        viewModel.isInPickingTasksOrHabitForStats.value = false
    }


}