package com.example.towardsgoalsapp

import com.example.towardsgoalsapp.goals.GoalSynopsisViewModel

class Constants {
    companion object {
        const val IGNORE_COUNT_AS_INT: Int = -1
        const val IGNORE_ID_AS_LONG: Long = -1L
        const val IGNORE_ID_AS_INT: Int = -1
        const val MAX_GOALS_AMOUNT: Int = 7
        const val IGNORE_PAGE_AS_INT: Int = -1
        const val EMPTY_STRING = ""
        const val RV_ITEM_CACHE_SIZE: Int = 20

        val viewModelClassToNumber: Map<Class<*>, Int> = mapOf(
            GoalSynopsisViewModel::class.java to 100
            // goal details view model class to 101
        )
        val numberOfClassToViewModelClass: Map<Int, Class<*>> = mapOf(
            100 to GoalSynopsisViewModel::class.java
        )
    }
}

enum class OwnerType(val typeString: String) {
    TYPE_HABIT("habit"),
    TYPE_TASK("task"),
    TYPE_NONE("none")
}
