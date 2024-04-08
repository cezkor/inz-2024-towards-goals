package com.example.towardsgoalsapp.tasks.details

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.database.TaskData
import com.example.towardsgoalsapp.database.userdata.MutablesArrayContentState
import com.example.towardsgoalsapp.database.userdata.OneModifyUserDataSharer
import com.example.towardsgoalsapp.database.userdata.TaskDataMutableArrayManager
import com.example.towardsgoalsapp.database.userdata.ViewModelUserDataSharer
import com.example.towardsgoalsapp.database.userdata.ViewModelWithManyTasksSharers
import com.example.towardsgoalsapp.database.userdata.ViewModelWithTasksSharer
import com.example.towardsgoalsapp.etc.TupleOfFour
import com.example.towardsgoalsapp.goals.GoalViewModel
import com.example.towardsgoalsapp.tasks.TaskItemListFragment

class TaskEisenhowerMatrixViewModelFactory(private val sharer: OneModifyUserDataSharer<TaskData>): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TaskEisenhowerMatrixViewModel(sharer) as T
    }
}

class TaskEisenhowerMatrixViewModel(
    private val sharer: OneModifyUserDataSharer<TaskData>
): ViewModel() {

    companion object {
        val priorityToIdxMap = mapOf(
            Constants.MAX_PRIORITY     to 0,
            Constants.MAX_PRIORITY - 1 to 1,
            Constants.MAX_PRIORITY - 2 to 2,
            Constants.MAX_PRIORITY - 3 to 3
        )
    }

    val listState = sharer.arrayState

    fun notifyTaskUpdateOf(taskId : Long) = sharer.signalNeedOfChangeFor(taskId)

    fun recalcMatrixPositions() {
        val taskDataList: ArrayList<MutableLiveData<TaskData>> =
            sharer.getArrayOfUserData() ?: return
        val tmpArrays = arrayOf<ArrayList<TaskData>>(
            ArrayList(), ArrayList(), ArrayList(), ArrayList()
        )
        for (tsmt in taskDataList) {
            val task = tsmt.value
            task?.run {
                val priority = this.taskPriority
                // get index of array of quadrant of the matrix by priority
                val idx = priorityToIdxMap[priority] ?: return@run
                tmpArrays[idx].add(this)
            }
        }
        for (i in taskMtArrayManagers.indices) {
            taskMtArrayManagers[i].setUserDataArray(tmpArrays[i])
        }
    }

    val taskMtArrays = arrayOf<ArrayList<MutableLiveData<TaskData>>>(
        ArrayList(), ArrayList(), ArrayList(), ArrayList()
    )

    val taskMtArrayManagers
        = taskMtArrays.map { mta -> TaskDataMutableArrayManager(mta)  }.toTypedArray()

}

class TaskEisenhowerMatrix : Fragment() {

    companion object {
        const val LOG_TAG = "TaskMatrix"

        const val GOAL_ID_OF_TASK = "tegid"
        const val INHERIT_FROM_CLASS_NUMBER = "teicm"

        @JvmStatic
        fun newInstance(gid: Long, inheritFromClass: Int) =
            TaskEisenhowerMatrix().apply {
                arguments = Bundle().apply {
                    putLong(GOAL_ID_OF_TASK, gid)
                    putInt(INHERIT_FROM_CLASS_NUMBER, inheritFromClass)
                }
            }

        val expectedViewModelClasses = setOf(
            Constants.viewModelClassToNumber[GoalViewModel::class.java]
                ?: Constants.CLASS_NUMBER_NOT_RECOGNIZED,
            Constants.viewModelClassToNumber[TaskDetailsViewModel::class.java]
                ?: Constants.CLASS_NUMBER_NOT_RECOGNIZED
        )
    }

    private lateinit var viewModel: TaskEisenhowerMatrixViewModel
    private lateinit var taskDetailsLauncher: TaskInfoLauncher

    private var goalId = Constants.IGNORE_ID_AS_LONG
    private var classNumber = Constants.CLASS_NUMBER_NOT_RECOGNIZED

    private fun extractSharer() : OneModifyUserDataSharer<TaskData>?{
        if ((classNumber in expectedViewModelClasses) && classNumber != Constants.CLASS_NUMBER_NOT_RECOGNIZED) {
            val clazz: Class<out ViewModel>? = Constants.numberOfClassToViewModelClass[classNumber]
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
        return inflater.inflate(R.layout.fragment_task_matrix, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.run {
            goalId = this.getLong(GOAL_ID_OF_TASK, goalId)
            classNumber = this.getInt(INHERIT_FROM_CLASS_NUMBER, classNumber)
            val sharer = extractSharer() ?: return@run
            viewModel = ViewModelProvider(viewModelStore,
                TaskEisenhowerMatrixViewModelFactory(sharer)
            )[TaskEisenhowerMatrixViewModel::class.java]
        }

        val context = view.context
        val taskDoneDrawable = AppCompatResources.getDrawable(context, R.drawable.checked)
        val taskFailedDrawable = AppCompatResources.getDrawable(context, R.drawable.cross)
        val subtasksDrawable = AppCompatResources.getDrawable(context, R.drawable.four_lines)
        val doableTaskDrawable = AppCompatResources.getDrawable(context, R.drawable.full_oval)

        val rvArray = arrayOf<RecyclerView>(
            view.findViewById(R.id.recyclerView1),
            view.findViewById(R.id.recyclerView2),
            view.findViewById(R.id.recyclerView3),
            view.findViewById(R.id.recyclerView4),
        )

        taskDetailsLauncher = registerForActivityResult(TaskInfoContract()) {
            if (it != Constants.IGNORE_ID_AS_LONG) viewModel.notifyTaskUpdateOf(it)
        }

        for (iter in rvArray.indices) {
            val rv = rvArray[iter]
            rv.apply {
                setItemViewCacheSize(Constants.RV_ITEM_CACHE_SIZE)
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                adapter = EisenhowerMatrixListAdapter(
                    viewModel,
                    iter,
                    taskDoneDrawable,
                    taskFailedDrawable,
                    subtasksDrawable,
                    doableTaskDrawable
                ).apply {
                    setOnItemClickListener {
                        taskDetailsLauncher.launch(
                            // for showing/editing
                            TupleOfFour(it.taskId, Constants.IGNORE_ID_AS_LONG,
                                it.taskOwnerId?: Constants.IGNORE_ID_AS_LONG, false)
                        )
                    }
                }
            }

            val listStateForRV = viewModel.taskMtArrayManagers[iter].contentState
            val addedCountForRV = viewModel.taskMtArrayManagers[iter].addedCount
            listStateForRV.observe(viewLifecycleOwner) {
                val currentIter = iter
                if (it == MutablesArrayContentState.ADDED_NEW) {
                    val added = addedCountForRV.value?: 0
                    viewModel.taskMtArrays[currentIter].run {
                        val size = this.size
                        rvArray[currentIter]
                            .adapter?.notifyItemRangeInserted(size - added, added)
                        for (i in size - added..<size) {
                            Log.i(LOG_TAG, "data listener for pos $i, list $currentIter")
                            this[i].observe( viewLifecycleOwner
                            ) { tsd ->
                                Log.i(LOG_TAG, "data changed on pos $i, list $currentIter, notifying")
                                if (tsd == null) {
                                    rvArray[currentIter].adapter?.notifyItemRemoved(i)
                                }
                                else {
                                    rvArray[currentIter].adapter?.notifyItemChanged(i)
                                }
                            }
                        }
                    }
                }
            }
        }

        viewModel.listState.observe(viewLifecycleOwner) {
            viewModel.recalcMatrixPositions()
        }

    }

}