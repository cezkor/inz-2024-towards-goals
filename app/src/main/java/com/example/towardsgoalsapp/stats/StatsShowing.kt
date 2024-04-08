package com.example.towardsgoalsapp.stats

import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.database.HabitParameter
import com.example.towardsgoalsapp.database.repositories.HabitParamsRepository
import com.example.towardsgoalsapp.database.repositories.StatsDataRepository

class StatsShowing {
    companion object {

        suspend fun canShowHabitParamsStats(
            habitParamsRepo: HabitParamsRepository,
            habitId: Long
        ) : Boolean {

            val areThereAnyParams = (habitParamsRepo.getParamCountOf(habitId) ?: 0) > 0
            var areAllParamsWithNoValues: Boolean = false
            if (! areThereAnyParams)
                areAllParamsWithNoValues = true
            else {
                val params = habitParamsRepo.getAllByOwnerId(habitId)
                val toRemove = HashSet<HabitParameter>()
                for (hp in params) {
                    val paramValuesCount = habitParamsRepo.getParamValueCountOf(hp.paramId)
                    if (paramValuesCount == null || paramValuesCount < Constants.MINIMUM_SAMPLE)
                        toRemove.add(hp)
                }
                params.removeAll(toRemove)
                if (params.isEmpty())
                    areAllParamsWithNoValues = true
            }

            return areThereAnyParams && ! areAllParamsWithNoValues
        }

        suspend fun canShowHabitGeneralStats(
            statsDataRepo: StatsDataRepository,
            habitId: Long
        ) : Boolean {
            val habitStats = statsDataRepo.getAllHabitStatsDataByHabit(habitId)
            if (habitStats.size < 1) return false
            return true
        }

        suspend fun canShowTaskGeneralStats(
            statsDataRepo: StatsDataRepository,
            goalId: Long
        ) : Boolean {
            val taskStats = statsDataRepo.getAllMarkableTaskStatsDataByGoal(goalId)
            if (taskStats.size < 1) return false
            return true
        }

    }
}