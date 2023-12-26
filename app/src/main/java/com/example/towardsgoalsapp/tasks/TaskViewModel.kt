package com.example.towardsgoalsapp.tasks

import androidx.lifecycle.MutableLiveData
import com.example.towardsgoalsapp.impints.ImpIntData_OLD
import com.example.towardsgoalsapp.etc.TextsViewModel

open class TaskViewModel(private val taskId: Long): TextsViewModel() {

    val mutableTaskData = MutableLiveData<TaskData_OLD>()

    val arrayOfMutableSubtasksTaskData: ArrayList<MutableLiveData<TaskData_OLD>> =
        java.util.ArrayList()

    val arrayOfMutableImpIntData: ArrayList<MutableLiveData<ImpIntData_OLD>> =
        java.util.ArrayList()

    private fun calculateProgress(): Double {return .0} // to do when database code is ready

    open fun updateOneTask() {}
    open fun getEverything(fromUnfinished: Boolean = false) {}

    open fun updateTaskAsUnfinished() {}
    override fun updateTexts(newName: String?, newDescription: String?) {
        // validate
        // update in database
//
//        val old = mutableTaskData.value
//        mutableTaskData.value = TaskData(
//            old?.taskId?: Constants.IGNORE_ID_AS_LONG,
//            newName?: old?.taskName ?: Constants.EMPTY_STRING,
//            newDescription?: old?.taskDescription ?: Constants.EMPTY_STRING,
//            old?.taskProgress?: 0.0,
//            old?.taskOverId?: Constants.IGNORE_ID_AS_LONG,
//            old?.taskDone?: false,
//            old?.subtasksCount?: Constants.IGNORE_COUNT_AS_LONG
//        )
    }
}