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
                    remindOnAdapter = TextAndInstantAdapter
                ),
                TaskDataAdapter = TaskData.Adapter(
                    taskDepthAdapter = IntColumnAdapter,
                ),
                UnfinishedTaskDataAdapter = UnfinishedTaskData.Adapter(
                    taskDepthAdapter = IntColumnAdapter,
                ),
                HabitStatsDataAdapter = HabitStatsData.Adapter(
                    addedOnAdapter = TextAndInstantAdapter
                ),
                TaskStatsDataAdapter = TaskStatsData.Adapter(
                    addedOnAdapter = TextAndInstantAdapter
                )
            )
        }
    }

}
