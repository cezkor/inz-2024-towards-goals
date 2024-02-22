package com.example.towardsgoalsapp.database.repositories

import androidx.lifecycle.MutableLiveData
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.database.HabitData
import com.example.towardsgoalsapp.database.TGDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HabitRepository(private val db: TGDatabase): OwnedByOneTypeOnlyOwnerUserData {
    override suspend fun getAllByOwnerId(ownerId: Long, allowUnfinished: Boolean): ArrayList<HabitData> {
        return withContext(Dispatchers.IO) {
            val all = db.habitDataQueries.selectAllOf(ownerId).executeAsList()
                .toCollection(ArrayList())
            val filtered =
                if (allowUnfinished) all else all.filter { hd -> !hd.habitEditUnfinished }
            filtered.toCollection(ArrayList())
        }
    }

    suspend fun getAllByGoalId(goalId: Long, allowUnfinished: Boolean = true): ArrayList<HabitData>
        = getAllByOwnerId(goalId, allowUnfinished)

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

    override suspend fun getOneById(id: Long, allowUnfinished: Boolean ): HabitData?{
        return withContext(Dispatchers.IO) {
            val unfinished = db.habitDataQueries.getHabitUnfinished(id).executeAsOneOrNull()
                ?: return@withContext null
            if (unfinished) {
                if (!allowUnfinished) return@withContext null
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
                val x = db.habitDataQueries.selectGivenHabit(id).executeAsOneOrNull()
                x
            }
        }
    }

    override suspend fun deleteById(id: Long) {
        return withContext(Dispatchers.IO) {
            db.habitDataQueries.deleteHabit(id)
        }
    }
}