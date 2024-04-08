package com.example.towardsgoalsapp.database.repositories

import com.example.towardsgoalsapp.database.HabitStatsData
import com.example.towardsgoalsapp.database.MarkableTaskStatsData
import com.example.towardsgoalsapp.database.TGDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

class StatsDataRepository (private val db: TGDatabase) {

    suspend fun getAllMarkableTaskStatsDataByGoal(goalId: Long) : ArrayList<MarkableTaskStatsData> {
        return withContext(Dispatchers.IO) {
            db.markableTaskStatsDataQueries.selectAllTaskStatsOf(goalId)
                .executeAsList().toCollection(ArrayList())
        }
    }

    suspend fun getAllHabitStatsDataByHabit(habitId: Long) : ArrayList<HabitStatsData> {
        return withContext(Dispatchers.IO) {
            db.habitStatsDataQueries.selectAllHabitStatsOfHabit(habitId)
                .executeAsList().toCollection(ArrayList())
        }
    }

    suspend fun putNewHabitStatsData(
        habitId: Long,
        goalId: Long,
        habitDoneWell: Boolean,
        habitDoneNotWell: Boolean,
        addedOn: Instant
    ) {
        return withContext(Dispatchers.IO) {
            db.habitStatsDataQueries.insertData(
                goalId,
                habitId,
                habitDoneWell,
                habitDoneNotWell,
                addedOn
            )
        }
    }

    suspend fun putNewMarkableTaskStatsData(
        taskId: Long,
        goalId: Long,
        taskFailed: Boolean,
        addedOn: Instant
    ) {
        return withContext(Dispatchers.IO) {
            db.markableTaskStatsDataQueries.insertData(
                taskId,
                goalId,
                addedOn,
                taskFailed
            )
        }
    }

}