package com.example.towardsgoalsapp.tasks

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.goals.GoalSynopsisesViewModel
import com.example.towardsgoalsapp.goals.GoalViewModel

class TaskItemListViewModel(val taskDataList: ArrayList<MutableLiveData<TaskData_OLD>>): ViewModel() {

    fun updateOneDataAt() {}

    fun updateAll() {}

}

class TaskItemList : Fragment() {

    companion object {

        const val GOAL_ID_OF_TASK = "gidofh"
        const val INHERIT_FROM_CLASS_NUMBER = "ihnclnum"
        const val LOG_TAG = "TIList"

        @JvmStatic
        fun newInstance(gid: Long, inheritFromClass: Int) =
            TaskItemList().apply {
                arguments = Bundle().apply {
                    putLong(GOAL_ID_OF_TASK, gid)
                    putInt(INHERIT_FROM_CLASS_NUMBER, inheritFromClass)
                }
            }
    }

    private var goalId = Constants.IGNORE_ID_AS_LONG

    private var tasksCount = Constants.IGNORE_COUNT_AS_INT // will be got from database

    private lateinit var viewModel: TaskItemListViewModel

    private fun extractLiveDataArray(clazz: Class<*>?) : java.util.ArrayList<MutableLiveData<TaskData_OLD>>?{
        return when (clazz) {
            GoalSynopsisesViewModel::class.java -> {
                val inheritedViewModel: GoalSynopsisesViewModel =
                    ViewModelProvider(requireActivity())[clazz]
                inheritedViewModel.taskDataArraysPerGoal[goalId]
            }
            GoalViewModel::class.java -> {
                val inheritedViewModel: GoalViewModel =
                    ViewModelProvider(requireActivity())[clazz]
                inheritedViewModel.arrayOfMutableTaskData
            }
            TaskViewModel::class.java, TaskDetailsViewModel::class.java,
                TaskOngoing::class.java -> {
                val inheritedViewModel: TaskViewModel =
                    ViewModelProvider(requireActivity())[TaskViewModel::class.java]
                inheritedViewModel.arrayOfMutableSubtasksTaskData
            }
            else -> null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            goalId = it.getLong(GOAL_ID_OF_TASK)
            val classNumber = it.getInt(INHERIT_FROM_CLASS_NUMBER)
            val liveDataz = extractLiveDataArray(
                Constants.numberOfClassToViewModelClass[classNumber]
            )
            liveDataz?.run {
                viewModel = TaskItemListViewModel(this)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.task_list_fragment, container, false)
        if (view is RecyclerView) { with(view) {
            view.setItemViewCacheSize(Constants.RV_ITEM_CACHE_SIZE)
            view.setHasFixedSize(true)

            adapter = TaskItemListAdapter(viewModel).apply {
                stateRestorationPolicy = RecyclerView.Adapter
                    .StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
            view.addItemDecoration(
                DividerItemDecoration(view.context, RecyclerView.HORIZONTAL).apply {
                    setDrawable(
                        ContextCompat.getDrawable(view.context, R.drawable.recycler_view_divider)!!
                    )
                }
            )

            var i = 0
            while (i < tasksCount) {
                val k = i
                viewModel.taskDataList[k].observe( viewLifecycleOwner
                ) {
                    Log.i(LOG_TAG, "data changed on pos $k, notifying")
                    view.adapter?.notifyItemChanged(k)
                }
                i += 1
            }
        } }
        return view
    }

}