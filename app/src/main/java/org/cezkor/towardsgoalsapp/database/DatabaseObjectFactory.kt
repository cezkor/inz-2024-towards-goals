package org.cezkor.towardsgoalsapp.database

import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver

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
                    ownerTypeAdapter = EnumColumnAdapter<org.cezkor.towardsgoalsapp.OwnerType>()
                ),
                UnfinishedImpIntDataAdapter = UnfinishedImpIntData.Adapter(
                    ownerTypeAdapter = EnumColumnAdapter<org.cezkor.towardsgoalsapp.OwnerType>()
                ),
                ReminderDataAdapter = ReminderData.Adapter(
                    ownerTypeAdapter = EnumColumnAdapter<org.cezkor.towardsgoalsapp.OwnerType>(),
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
