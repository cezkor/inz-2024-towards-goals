package com.example.towardsgoalsapp.goals

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.towardsgoalsapp.R


class PickingFragment : Fragment() {

    companion object {
        const val LOG_TAG = "PickTorH"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_goal_picking_task_or_habit, container, false)
    }

    private lateinit var viewModel: GoalViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[GoalViewModel::class.java]
        val context = requireContext()

        val completedHabitImage =
            AppCompatResources.getDrawable(context, R.drawable.full_oval_green)
        val markableImage = AppCompatResources.getDrawable(context, R.drawable.full_oval_gray)

        val radioGroup: RadioGroup = view.findViewById(R.id.pickRadioGroup)
        val rView: RecyclerView = view.findViewById(R.id.pickingRV)

        rView.itemAnimator = null
        rView.setHasFixedSize(true)
        rView.layoutManager = LinearLayoutManager(context)

        radioGroup.setOnCheckedChangeListener { rg, id ->
            when (id) {
                R.id.taskListButton -> {
                    viewModel.dataPicked = viewModel.mutableGoalData.value?.goalId
                    viewModel.isInPickingTasksOrHabitForStats.value = false
                }
                R.id.habitListButton -> {
                    val list = viewModel.habitsSharer.getArrayOfUserData()
                    if (list.isNullOrEmpty()) return@setOnCheckedChangeListener
                    val remappedList = list.mapNotNull { mt -> mt.value }
                        .toCollection(ArrayList())
                    rView.adapter = PickingItemListAdapter(
                        remappedList,
                        markableImage,
                        completedHabitImage
                    ).apply {
                        setOnItemClickListener {
                            viewModel.dataPicked = it
                            viewModel.isInPickingTasksOrHabitForStats.value = false
                        }
                    }
                }
                else -> {
                    // ignore
                }
            }
        }
    }

}