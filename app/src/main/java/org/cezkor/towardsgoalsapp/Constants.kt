package org.cezkor.towardsgoalsapp

import androidx.lifecycle.ViewModel
import org.cezkor.towardsgoalsapp.etc.TextsViewModel
import org.cezkor.towardsgoalsapp.goals.GoalSynopsisesViewModel
import org.cezkor.towardsgoalsapp.goals.GoalViewModel
import org.cezkor.towardsgoalsapp.habits.questioning.HabitQuestionsViewModel
import org.cezkor.towardsgoalsapp.habits.HabitViewModel
import org.cezkor.towardsgoalsapp.tasks.details.TaskDetailsViewModel
import org.cezkor.towardsgoalsapp.tasks.ongoing.TaskOngoingViewModel
import org.cezkor.towardsgoalsapp.tasks.TaskViewModel

class Constants {
    companion object {

        private const val TEST_DATABASE_FILE_NAME = "test_database.db"
        private const val TRUE_DATABASE_FILE_NAME = "tg_database.db"
        val DATABASE_FILE_NAME = if (org.cezkor.towardsgoalsapp.BuildConfig.SHOULD_USE_TEST_DATA)
            org.cezkor.towardsgoalsapp.Constants.Companion.TEST_DATABASE_FILE_NAME
                                 else
            org.cezkor.towardsgoalsapp.Constants.Companion.TRUE_DATABASE_FILE_NAME

        const val IGNORE_ID_AS_LONG: Long = -1L
        const val IGNORE_ID_AS_INT: Int = -1
        const val MAX_GOALS_AMOUNT: Int = 7
        const val IGNORE_PAGE_AS_INT: Int = -1
        const val EMPTY_STRING = ""
        const val RV_ITEM_CACHE_SIZE: Int = 20
        const val MAX_TASK_DEPTH = 3
        const val MAX_HABIT_PARAM_COUNT_PER_HABIT = 12
        const val TASK_POMODORO_NOTIFICATION_CHANNEL = "TowardsGoals_Pomodoro"
        const val REMINDER_NOTIFICATION_CHANNEL = "TowardsGoals_Reminder"
        const val CLASS_NUMBER_NOT_RECOGNIZED: Int = -1000
        const val X_BIAS_FLOAT : Float = 0f
        const val MAX_PRIORITY: Int = 3
        const val MIN_PRIORITY: Int = 0
        const val MAX_WORK_TIME: Int = 45
        const val MAX_SHORT_BREAK_TIME: Int = 15
        const val MAX_LONG_BREAK_TIME: Int = 30
        const val DEFAULT_WORK_TIME: Int = 25
        const val DEFAULT_SHORT_BREAK_TIME: Int = 5
        const val DEFAULT_LONG_BREAK_TIME: Int = 5
        const val ADDITIONAL_TIME: Int = 5

        val periodsArray: Array<Long> = arrayOf(1, 7, 14, 28, 2*28, 3*28, 6*28, 365)

        const val NAME_LENGTH: Int = 100
        const val SHORTENED_NAME_LENGTH: Int = 30 - 1 // + 1 for '…' if needed
        const val SHORTENED_DESCRIPTION_LENGTH: Int = 350 - 1 // + 1 for '…' if needed
        const val EISENHOWER_MATRIX_NAME_LENGTH: Int = 18 - 1 // + 1 for '…' if needed
        const val VERY_SHORT_NAME_LENGTH: Int = 20
        const val DESCRIPTION_LENGTH: Int = 2000
        const val UNIT_NAME_LENGTH: Int = 5
        const val MINIMUM_SAMPLE: Int = 5

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
        infix fun from(ts: String): org.cezkor.towardsgoalsapp.OwnerType? = entries.firstOrNull { it.typeString == ts }
    }
}

