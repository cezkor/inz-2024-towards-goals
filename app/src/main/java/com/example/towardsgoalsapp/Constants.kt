package com.example.towardsgoalsapp

import com.example.towardsgoalsapp.goals.GoalSynopsisViewModel
import com.example.towardsgoalsapp.goals.GoalViewModel
import com.example.towardsgoalsapp.tasks.TaskDetails
import com.example.towardsgoalsapp.tasks.TaskDetailsViewModel
import com.example.towardsgoalsapp.tasks.TaskOngoingViewModel
import com.example.towardsgoalsapp.tasks.TaskViewModel

class Constants {
    companion object {
        const val IGNORE_COUNT_AS_LONG: Long = -1L
        const val IGNORE_COUNT_AS_INT: Int = -1
        const val IGNORE_ID_AS_LONG: Long = -1L
        const val IGNORE_ID_AS_INT: Int = -1
        const val MAX_GOALS_AMOUNT: Int = 7
        const val IGNORE_PAGE_AS_INT: Int = -1
        const val EMPTY_STRING = ""
        const val RV_ITEM_CACHE_SIZE: Int = 20
        const val MAX_TASK_DEPTH = 3

        val viewModelClassToNumber: Map<Class<*>, Int> = mapOf(
            GoalSynopsisViewModel::class.java to 100,
            GoalViewModel::class.java to 101,
            TaskViewModel::class.java to 102,
            TaskDetailsViewModel::class.java to 103,
            TaskOngoingViewModel::class.java to 104
        )
        val numberOfClassToViewModelClass: Map<Int, Class<*>> = mapOf(
            100 to GoalSynopsisViewModel::class.java,
            101 to GoalViewModel::class.java,
            102 to TaskViewModel::class.java,
            103 to TaskDetailsViewModel::class.java,
            104 to TaskOngoingViewModel::class.java
        )
    }
}

enum class OwnerType(val typeString: String) {
    TYPE_HABIT("habit"),
    TYPE_TASK("task"),
    TYPE_NONE("none")
}
