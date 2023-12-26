package com.example.towardsgoalsapp.database

import androidx.lifecycle.MutableLiveData
import com.example.towardsgoalsapp.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HabitRepository(private val db: TGDatabase): OwnedByOneTypeOnlyOwnerUserData {
    override suspend fun getAllByOwnerId(ownerId: Long): ArrayList<HabitData> {
        return withContext(Dispatchers.IO) {
            db.habitDataQueries.selectAllOf(ownerId).executeAsList()
                .toCollection(ArrayList())
        }
    }

    suspend fun getAllByGoalId(goalId: Long): ArrayList<HabitData> = getAllByOwnerId(goalId)

    suspend fun addHabit(habitName: String, habitDescription: String,
                        habitTargetCount: Long, habitTargetPeriod: Long, goalId: Long ) : Long {
        return withContext(Dispatchers.IO) {
            db.habitDataQueries.insertOneHabit(
                null,
                habitName,
                habitDescription,
                habitTargetCount,
                habitTargetPeriod,
                goalId
            )
            db.habitDataQueries
                .lastInsertRowId().executeAsOneOrNull() ?: Constants.IGNORE_ID_AS_LONG
        }
    }

    suspend fun putAsUnfinished(habitData: HabitData) {
        return withContext(Dispatchers.IO) {
            db.habitDataQueries.insertOneEditUnfinishedHabit(
                habitData.habitId,
                habitData.habitName,
                habitData.habitDescription,
                habitData.habitTargetCount,
                habitData.habitTargetPeriod,
                habitData.goalId,
                habitData.habitDoneWellCount,
                habitData.habitDoneNotWellCount,
                habitData.habitTotalCount,
                habitData.habitTargetCompleted
            )
        }
    }

    suspend fun putAllHabits(habits: ArrayList<MutableLiveData<HabitData>>) {
        return withContext(Dispatchers.IO) {
            db.taskDataQueries.transaction {
                for (ht in habits) ht.value?.run {
                    db.habitDataQueries.insertOneHabit(
                        null,
                        this.habitName,
                        this.habitDescription,
                        this.habitTargetCount,
                        this.habitTargetPeriod,
                        this.goalId
                    )
                }
            }
        }
    }

    override suspend fun updateTexts(id: Long, firstText: String, secondText: String) {
        return withContext(Dispatchers.IO) {
            db.habitDataQueries.updateHabitTexts(firstText, secondText, id)
        }
    }

    suspend fun updateTargets(id: Long, targetCount: Long, targetPeriod: Long) {
        return withContext(Dispatchers.IO) {
            db.habitDataQueries.updateHabitTargets(targetCount,targetPeriod, id)
        }
    }

    override suspend fun markEditing(id: Long, isUnfinished: Boolean) {
        return withContext(Dispatchers.IO) {
            db.habitDataQueries.markHabitEdit(isUnfinished, id)
        }
    }

    suspend fun skipHabit(id: Long) {
        return withContext(Dispatchers.IO) {
            db.habitDataQueries.skipHabit(id)
        }
    }

    suspend fun markHabitDoneWell(id: Long) {
        return withContext(Dispatchers.IO) {
            db.habitDataQueries.markHabitAsDoneWell(id)
        }
    }

    suspend fun markHabitDoneNotWell(id: Long) {
        return withContext(Dispatchers.IO) {
            db.habitDataQueries.markHabitAsNotDoneWell(id)
        }
    }

    override suspend fun getOneById(id: Long): HabitData?{
        return withContext(Dispatchers.IO) {
            val unfinished = db.habitDataQueries.getHabitUnfinished(id).executeAsList().last()
            if (unfinished) {
                val uhd = db.habitDataQueries.selectGivenUnfinishedHabit(id).executeAsOneOrNull()
                if (uhd == null) null
                else HabitData(
                    uhd.habitId,
                    true,
                    uhd.habitName,
                    uhd.habitDescription,
                    uhd.habitTargetCount,
                    uhd.habitTargetPeriod,
                    uhd.goalId,
                    uhd.habitDoneWellCount,
                    uhd.habitDoneNotWellCount,
                    uhd.habitTotalCount,
                    uhd.habitTargetCompleted
                )
            }
            else {
                db.habitDataQueries.selectGivenHabit(id).executeAsOneOrNull()
            }
        }
    }

    override suspend fun deleteById(id: Long) {
        return withContext(Dispatchers.IO) {
            db.habitDataQueries.deleteHabit(id)
        }
    }
}