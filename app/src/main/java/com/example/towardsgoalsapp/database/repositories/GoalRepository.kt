package com.example.towardsgoalsapp.database.repositories

import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.database.GoalData
import com.example.towardsgoalsapp.database.TGDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoalRepository(private val db: TGDatabase) : UserDataInterface {

    override suspend fun updateTexts(id:Long, firstText: String, secondText: String) {
        return withContext(Dispatchers.IO) {
            db.goalDataQueries.updateGoalTexts(firstText, secondText, id)
        }
    }

    suspend fun getAllGoals(): ArrayList<GoalData> {
        return withContext(Dispatchers.IO) {
            db.goalDataQueries.selectAll().executeAsList().toCollection(ArrayList())
        }
    }

    override suspend fun getOneById(id: Long, allowUnfinished: Boolean): GoalData? {
        // allowUnfinished is ignored for goals as currently
        // there is no other mechanism that would allow user to
        // edit unfinished goal
        return withContext(Dispatchers.IO) {
            val unfinished = db.goalDataQueries.getGoalUnfinished(id).executeAsOneOrNull()
                ?: return@withContext null
            if (unfinished) {
                val ugd = db.goalDataQueries.selectGivenUnfinishedGoal(id).executeAsOneOrNull()
                if (ugd == null) null
                else GoalData(
                    ugd.goalId,
                    true,
                    ugd.goalName,
                    ugd.goalDescription,
                    ugd.goalProgress,
                    ugd.pageNumber
                )
            }
            else {
                db.goalDataQueries.selectGivenGoal(id).executeAsOneOrNull()
            }
        }
    }

    override suspend fun deleteById(id: Long) {
        return withContext(Dispatchers.IO) {
            db.goalDataQueries.deleteGoal(id)
        }
    }

    suspend fun addOneGoal(goalName: String,
                           goalDescription: String,
                           pageNum: Int) : Long {
        return withContext(Dispatchers.IO) {
            db.goalDataQueries.insertOneGoal(
                null,
                      goalName,
                    goalDescription,
                pageNum
            )
            db.goalDataQueries.
            lastInsertRowId().executeAsOneOrNull()?: Constants.IGNORE_ID_AS_LONG
        }
    }

    override suspend fun markEditing(id: Long, isUnfinished: Boolean) {
        return withContext(Dispatchers.IO) {
            db.goalDataQueries.markGoalEdit(isUnfinished, id)
        }
    }

    suspend fun putAsUnfinished(goalData: GoalData) {
        return withContext(Dispatchers.IO) {
            db.goalDataQueries.insertOneEditUnfinishedGoal(
                goalData.goalId,
                goalData.goalName,
                goalData.goalDescription,
                goalData.goalProgress,
                goalData.pageNumber
            )
        }
    }
}