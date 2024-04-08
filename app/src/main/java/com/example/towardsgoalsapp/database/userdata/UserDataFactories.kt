package com.example.towardsgoalsapp.database.userdata

import com.example.towardsgoalsapp.database.GoalData
import com.example.towardsgoalsapp.database.HabitData
import com.example.towardsgoalsapp.database.TaskData


interface RecreateUserDataWithText<UserData> {

    fun createUserDataBasedOnTexts(userData: UserData, name: String?, description: String?) : UserData

}

interface RecreateHabitDataBasedWithTargetsAndTexts : RecreateUserDataWithText<HabitData> {

    fun createUserDataBasedOnTargets(userData: HabitData, targetCount: Long?, targetPeriod: Long?) : HabitData

}

val RecreatingGoalDataFactory = object : RecreateUserDataWithText<GoalData> {


    override fun createUserDataBasedOnTexts(userData: GoalData, name: String?, description: String?) : GoalData {
        return GoalData(
            userData.goalId,
            userData.goalEditUnfinished,
            name?: userData.goalName,
            description?: userData.goalDescription,
            userData.goalProgress,
            userData.pageNumber
        )
    }

}

val RecreatingTaskDataFactory = object : RecreateUserDataWithText<TaskData> {

    override fun createUserDataBasedOnTexts(
        userData: TaskData,
        name: String?,
        description: String?
    ): TaskData {
        return TaskData(
            userData.taskId,
            userData.taskEditUnfinished,
            name?: userData.taskName,
            description?: userData.taskDescription,
            userData.taskProgress,
            userData.taskOwnerId,
            userData.goalId,
            userData.taskDepth,
            userData.subtasksCount,
            userData.taskDone,
            userData.taskFailed,
            userData.taskPriority
        )
    }

}

val RecreatingHabitDataFactory = object : RecreateHabitDataBasedWithTargetsAndTexts {

    override fun createUserDataBasedOnTexts(
        userData: HabitData,
        name: String?,
        description: String?
    ): HabitData {
        return HabitData(
            userData.habitId,
            userData.habitEditUnfinished,
            name?: userData.habitName,
            description?: userData.habitDescription,
            userData.habitTargetCount,
            userData.habitTargetPeriod,
            userData.goalId,
            userData.habitDoneWellCount,
            userData.habitDoneNotWellCount,
            userData.habitTotalCount,
            userData.habitTargetCompleted,
            userData.habitMarkCount,
            userData.habitLastMarkedOn
        )
    }

    override fun createUserDataBasedOnTargets(
        userData: HabitData,
        targetCount: Long?,
        targetPeriod: Long?
    ): HabitData {
        return HabitData(
            userData.habitId,
            userData.habitEditUnfinished,
            userData.habitName,
            userData.habitDescription,
            targetCount ?: userData.habitTargetCount,
            targetPeriod ?: userData.habitTargetPeriod,
            userData.goalId,
            userData.habitDoneWellCount,
            userData.habitDoneNotWellCount,
            userData.habitTotalCount,
            userData.habitTargetCompleted,
            userData.habitMarkCount,
            userData.habitLastMarkedOn
        )
    }

}