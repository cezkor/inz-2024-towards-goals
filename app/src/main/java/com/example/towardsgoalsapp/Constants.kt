package com.example.towardsgoalsapp

import androidx.lifecycle.ViewModel
import com.example.towardsgoalsapp.etc.TextsViewModel
import com.example.towardsgoalsapp.goals.GoalSynopsisesViewModel
import com.example.towardsgoalsapp.goals.GoalViewModel
import com.example.towardsgoalsapp.habits.questioning.HabitQuestionsViewModel
import com.example.towardsgoalsapp.habits.HabitViewModel
import com.example.towardsgoalsapp.tasks.TaskDetailsViewModel
import com.example.towardsgoalsapp.tasks.ongoing.TaskOngoingViewModel
import com.example.towardsgoalsapp.tasks.TaskViewModel

class Constants {
    companion object {
        const val IGNORE_ID_AS_LONG: Long = -1L
        const val IGNORE_ID_AS_INT: Int = -1
        const val MAX_GOALS_AMOUNT: Int = 7
        const val IGNORE_PAGE_AS_INT: Int = -1
        const val EMPTY_STRING = ""
        const val RV_ITEM_CACHE_SIZE: Int = 20
        const val MAX_TASK_DEPTH = 3
        const val DATABASE_FILE_NAME = "tg_database.db"
        const val CLASS_NUMBER_NOT_RECOGNIZED: Int = -1000

        val periodsArray: Array<Long> = arrayOf(1, 7, 14, 28, 3*28, 6*28, 12*28)
        val periodsStepArray: Array<Long> = arrayOf(1, 1, 1, 7, 7, 28, 28)

        const val NAME_LENGTH: Int = 100
        const val DESCRIPTION_LENGTH: Int = 2000

        val viewModelClassToNumber: Map<Class<out ViewModel>, Int> = mapOf(
            GoalSynopsisesViewModel::class.java to 100,
            GoalViewModel::class.java to 101,
            TaskViewModel::class.java to 102,
            TaskDetailsViewModel::class.java to 103,
            TaskOngoingViewModel::class.java to 104,
            HabitViewModel::class.java to 105,
            HabitQuestionsViewModel::class.java to 106,
            TextsViewModel::class.java to 107
        )
        val numberOfClassToViewModelClass: Map<Int, Class<out ViewModel>> = mapOf(
            100 to GoalSynopsisesViewModel::class.java,
            101 to GoalViewModel::class.java,
            102 to TaskViewModel::class.java,
            103 to TaskDetailsViewModel::class.java,
            104 to TaskOngoingViewModel::class.java,
            105 to HabitViewModel::class.java,
            106 to HabitQuestionsViewModel::class.java,
            107 to TextsViewModel::class.java
        )
    }
}

enum class OwnerType(val typeString: String) {

    TYPE_HABIT("habit"),
    TYPE_TASK("task"),
    TYPE_NONE("none");

    companion object {
        infix fun from(ts: String): OwnerType? = entries.firstOrNull { it.typeString == ts }
    }
}
