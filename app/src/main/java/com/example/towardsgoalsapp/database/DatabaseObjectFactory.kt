package com.example.towardsgoalsapp.database

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.example.towardsgoalsapp.OwnerType

class DatabaseObjectFactory private constructor() {

    companion object {
        fun newDatabaseObject(driver: SqlDriver): TGDatabase {

            return TGDatabase(
                driver,
                GoalDataAdapter = GoalData.Adapter(
                    pageNumberAdapter = IntColumnAdapter
                ),
                UnfinishedGoalDataAdapter = UnfinishedGoalData.Adapter(
                    pageNumberAdapter = IntColumnAdapter
                ),
                ImpIntDataAdapter = ImpIntData.Adapter(
                    ownerTypeAdapter = EnumColumnAdapter<OwnerType>()
                ),
                UnfinishedImpIntDataAdapter = UnfinishedImpIntData.Adapter(
                    ownerTypeAdapter = EnumColumnAdapter<OwnerType>()
                ),
                ReminderDataAdapter = ReminderData.Adapter(
                    ownerTypeAdapter = EnumColumnAdapter<OwnerType>(),
                    remindOnAdapter = TextAndInstantAdapter,
                    lastRemindedAdapter = TextAndInstantAdapter
                ),
                TaskDataAdapter = TaskData.Adapter(
                    taskDepthAdapter = IntColumnAdapter,
                    taskPriorityAdapter = IntColumnAdapter
                ),
                UnfinishedTaskDataAdapter = UnfinishedTaskData.Adapter(
                    taskDepthAdapter = IntColumnAdapter,
                    taskPriorityAdapter = IntColumnAdapter
                ),
                HabitStatsDataAdapter = HabitStatsData.Adapter(
                    addedOnAdapter = TextAndInstantAdapter
                ),
                MarkableTaskStatsDataAdapter = MarkableTaskStatsData.Adapter(
                    addedOnAdapter = TextAndInstantAdapter
                ),
                HabitParameterValueAdapter = HabitParameterValue.Adapter(
                    addedOnAdapter = TextAndInstantAdapter
                ),
                HabitDataAdapter = HabitData.Adapter(
                    habitLastMarkedOnAdapter = TextAndInstantAdapter
                ),
                UnfinishedHabitDataAdapter = UnfinishedHabitData.Adapter(
                    habitLastMarkedOnAdapter = TextAndInstantAdapter
                )
            )
        }
    }

}
