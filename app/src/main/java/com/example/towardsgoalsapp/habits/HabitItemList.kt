package com.example.towardsgoalsapp.habits

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
import com.example.towardsgoalsapp.goals.GoalSynopsisViewModel

class HabitItemListViewModel(val habitDataList: ArrayList<MutableLiveData<HabitData>>): ViewModel() {
    fun updateOneDataAt() {}

    fun updateAll() {}

}

class HabitItemList : Fragment() {

    companion object {

        const val GOAL_ID_OF_HABITS = "gidofh"
        const val INHERIT_FROM_CLASS_NUMBER = "ihnclnum"
        const val LOG_TAG = "HIList"

        @JvmStatic
        fun newInstance(gid: Long, inheritFromClass: Int) =
            HabitItemList().apply {
                arguments = Bundle().apply {
                    putLong(GOAL_ID_OF_HABITS, gid)
                    putInt(INHERIT_FROM_CLASS_NUMBER, inheritFromClass)
                }
            }
    }

    private var goalId = Constants.IGNORE_ID_AS_LONG

    private var habitsCount = Constants.IGNORE_COUNT_AS_INT // will be got from database

    private lateinit var viewModel: HabitItemListViewModel

    private fun extractLiveDataArray(clazz: Class<*>?) : java.util.ArrayList<MutableLiveData<HabitData>>?{
        return when (clazz) {
            GoalSynopsisViewModel::class.java -> {
                val inheritedViewModel: GoalSynopsisViewModel =
                    ViewModelProvider(requireActivity())[GoalSynopsisViewModel::class.java]
                inheritedViewModel.habitDataArraysPerGoal[goalId]
            }
            else -> null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            goalId = it.getLong(GOAL_ID_OF_HABITS)
            val classNumber = it.getInt(INHERIT_FROM_CLASS_NUMBER)
            var liveDataz = extractLiveDataArray(
                Constants.numberOfClassToViewModelClass[classNumber]
            )
            liveDataz?.run {
                viewModel = HabitItemListViewModel(this)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.task_list_fragment, container, false)
        // Set the adapter
        if (view is RecyclerView) { with(view) {
            view.setItemViewCacheSize(Constants.RV_ITEM_CACHE_SIZE)
            view.setHasFixedSize(true)

            adapter = HabitItemListAdapter(viewModel).apply {
                stateRestorationPolicy = RecyclerView.Adapter
                    .StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
            // adding a divider since i was not able to find a xml attribute/style for that
            view.addItemDecoration(
                DividerItemDecoration(view.context, RecyclerView.HORIZONTAL).apply {
                    setDrawable(
                        ContextCompat.getDrawable(view.context, R.drawable.recycler_view_divider)!!
                    )
                }
            )

            var i = 0
            while (i < habitsCount) {
                val k = i
                viewModel.habitDataList[k].observe( viewLifecycleOwner
                ) {
                    Log.i(LOG_TAG, "data changed on pos $k, notifying")
                    view.adapter?.notifyItemChanged(k)
                }
                i += 1
            }
        } }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

}