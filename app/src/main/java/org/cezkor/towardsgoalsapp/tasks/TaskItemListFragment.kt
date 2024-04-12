package org.cezkor.towardsgoalsapp.tasks

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import org.cezkor.towardsgoalsapp.Constants.Companion.CLASS_NUMBER_NOT_RECOGNIZED
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.goals.GoalSynopsisesViewModel
import org.cezkor.towardsgoalsapp.goals.GoalViewModel
import org.cezkor.towardsgoalsapp.database.*
import org.cezkor.towardsgoalsapp.database.userdata.MutablesArrayContentState
import org.cezkor.towardsgoalsapp.database.userdata.OneModifyUserDataSharer
import org.cezkor.towardsgoalsapp.etc.TupleOfFour
import org.cezkor.towardsgoalsapp.database.userdata.ViewModelUserDataSharer
import org.cezkor.towardsgoalsapp.database.userdata.ViewModelWithManyTasksSharers
import org.cezkor.towardsgoalsapp.database.userdata.ViewModelWithTasksSharer
import org.cezkor.towardsgoalsapp.tasks.details.TaskDetailsViewModel
import org.cezkor.towardsgoalsapp.tasks.details.TaskInfoContract
import org.cezkor.towardsgoalsapp.tasks.details.TaskInfoLauncher
import org.cezkor.towardsgoalsapp.tasks.ongoing.TaskDoingContract
import org.cezkor.towardsgoalsapp.tasks.ongoing.TaskDoingLauncher

class TaskItemListViewModelFactory(private val sharer: OneModifyUserDataSharer<TaskData>): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TaskItemListViewModel(sharer) as T
    }
}

class TaskItemListViewModel(
    private val sharer: OneModifyUserDataSharer<TaskData>
): ViewModel() {
    val taskDataList: ArrayList<MutableLiveData<TaskData>>? = sharer.getArrayOfUserData()

    val listState = sharer.arrayState

    val addedCount = sharer.addedCount
    fun notifyTaskUpdateOf(taskId : Long) = sharer.signalNeedOfChangeFor(taskId)
}

class TaskItemListFragment : Fragment() {

    companion object {

        const val GOAL_ID_OF_TASK = "gidofh"
        const val INHERIT_FROM_CLASS_NUMBER = "ihnclnum"
        const val LOG_TAG = "TIList"

        @JvmStatic
        fun newInstance(gid: Long, inheritFromClass: Int) =
            TaskItemListFragment().apply {
                arguments = Bundle().apply {
                    putLong(GOAL_ID_OF_TASK, gid)
                    putInt(INHERIT_FROM_CLASS_NUMBER, inheritFromClass)
                }
            }

        val expectedViewModelClasses = setOf(
            org.cezkor.towardsgoalsapp.Constants.viewModelClassToNumber[GoalSynopsisesViewModel::class.java]
                ?: CLASS_NUMBER_NOT_RECOGNIZED,
            org.cezkor.towardsgoalsapp.Constants.viewModelClassToNumber[GoalViewModel::class.java]
                ?: CLASS_NUMBER_NOT_RECOGNIZED,
            org.cezkor.towardsgoalsapp.Constants.viewModelClassToNumber[TaskDetailsViewModel::class.java]
                ?: CLASS_NUMBER_NOT_RECOGNIZED
        )
    }

    private var goalId = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG

    private lateinit var viewModel: TaskItemListViewModel

    private lateinit var taskDoingLauncher: TaskDoingLauncher
    private lateinit var taskDetailsLauncher: TaskInfoLauncher

    private fun extractSharer(classnumber: Int) : OneModifyUserDataSharer<TaskData>?{
        if ((classnumber in expectedViewModelClasses) && classnumber != CLASS_NUMBER_NOT_RECOGNIZED) {
            val clazz: Class<out ViewModel>? = org.cezkor.towardsgoalsapp.Constants.numberOfClassToViewModelClass[classnumber]
            if (clazz == null ) return null
            else {

                val inheritedViewModel = ViewModelProvider(requireActivity())[clazz]
                var sharer: ViewModelUserDataSharer<TaskData>? = null
                if (inheritedViewModel is ViewModelWithTasksSharer) {
                    sharer = inheritedViewModel.getTasksSharer()
                }
                if (inheritedViewModel is ViewModelWithManyTasksSharers) {
                    sharer = inheritedViewModel.getTasksSharer(goalId)
                }
                return if (sharer is OneModifyUserDataSharer) sharer else null
            }
        }
        return null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        try {
            requireActivity()
        }
        catch (e: IllegalStateException) {
            Log.e(LOG_TAG, "unable to get activity", e)
            return null
        }

        if (arguments == null) {
            Log.e(LOG_TAG, "no arguments")
            return null
        }

        arguments?.let {
            goalId = it.getLong(GOAL_ID_OF_TASK)
            val classNumber = it.getInt(INHERIT_FROM_CLASS_NUMBER)
            val sharer = extractSharer(classNumber)
            sharer?.run {
                viewModel = ViewModelProvider(viewModelStore,
                    TaskItemListViewModelFactory(this))[TaskItemListViewModel::class.java]
            }
        }

        val view = inflater.inflate(R.layout.task_list_fragment, container, false)
        if (view is RecyclerView) { with(view) {

            // for reasons unknown to me
            // animations of recycler view items can cause crashes
            view.itemAnimator = null

            taskDetailsLauncher = registerForActivityResult(TaskInfoContract()) {
//                val hasAllActiveListeners: Boolean =
//                    viewModel.taskDataList?.firstOrNull { tm -> !tm.hasActiveObservers() }
//                        ?.hasActiveObservers() ?: false
//                Log.i(LOG_TAG, "active listeners $hasAllActiveListeners")
                if (it != org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) viewModel.notifyTaskUpdateOf(it)
            }

            taskDoingLauncher = registerForActivityResult(TaskDoingContract()) {
                if (it != org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) viewModel.notifyTaskUpdateOf(it)
            }

            view.setItemViewCacheSize(org.cezkor.towardsgoalsapp.Constants.RV_ITEM_CACHE_SIZE)
            view.setHasFixedSize(true)

            val taskDoneDrawable = AppCompatResources.getDrawable(context, R.drawable.checked)
            val taskFailedDrawable = AppCompatResources.getDrawable(context, R.drawable.cross)
            val unfinishedDrawable = AppCompatResources.getDrawable(context, R.drawable.alert)
            val subtasksDrawable = AppCompatResources.getDrawable(context, R.drawable.four_lines)

            adapter = TaskItemListAdapter(viewModel,
                taskDoneDrawable, taskFailedDrawable, subtasksDrawable ,unfinishedDrawable)
            .apply {
                stateRestorationPolicy = RecyclerView.Adapter
                    .StateRestorationPolicy.PREVENT_WHEN_EMPTY
                setOnItemClickListener {
                    taskDetailsLauncher.launch(
                        // for showing/editing
                        TupleOfFour(it.taskId, org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG,
                            it.taskOwnerId?: org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG, false)
                    )
                }
                setOnDoTaskButtonClickListener {
                    taskDoingLauncher.launch(it.taskId)
                }
            }

            view.addItemDecoration(
                DividerItemDecoration(view.context, RecyclerView.HORIZONTAL).apply {
                    setDrawable(
                        ContextCompat.getDrawable(view.context, R.drawable.recycler_view_divider)!!
                    )
                }
            )

            Log.i(LOG_TAG, "list state before creating listener ${viewModel.listState.value?.name}")
            viewModel.listState.observe(viewLifecycleOwner) {
                Log.i(LOG_TAG, "list state ${it.name}")
                if (it == MutablesArrayContentState.ADDED_NEW) {
                    val added = viewModel.addedCount.value?: 0
                    viewModel.taskDataList?.run {
                        val size = this.size
                        adapter?.notifyItemRangeInserted(size - added, added)
                        for (i in size - added..<size) {
                            Log.i(LOG_TAG, "data listener for pos $i")
                            this[i].observe( viewLifecycleOwner
                            ) { tsit ->
                                Log.i(LOG_TAG, "data changed on pos $i, notifying")
                                if (tsit == null) {
                                    view.adapter?.notifyItemRemoved(i)
                                }
                                else {
                                    view.adapter?.notifyItemChanged(i)
                                }
                            }
                        }
                    }
                }
            }

        } }
        return view
    }

}