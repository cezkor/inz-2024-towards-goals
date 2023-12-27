package com.example.towardsgoalsapp.database

import android.app.ActivityManager.TaskDescription
import androidx.lifecycle.MutableLiveData
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.OwnerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaskRepository(private val db: TGDatabase): OwnedByOneTypeOnlyOwnerUserData {
    override suspend fun getAllByOwnerId(ownerId: Long): ArrayList<TaskData> {
        return withContext(Dispatchers.IO) {
            db.taskDataQueries.selectAllOfGoal(ownerId).executeAsList()
                .toCollection(ArrayList())
        }
    }

    suspend fun getAllByGoalId(goalId: Long): ArrayList<TaskData> = getAllByOwnerId(goalId)

    suspend fun getAllByTaskOwnerId(ownerId: Long): ArrayList<TaskData> {
        return withContext(Dispatchers.IO) {
            db.taskDataQueries.selectAllOfOwnerTask(ownerId).executeAsList()
                .toCollection(ArrayList())
        }
    }

    suspend fun addTask(taskName: String, taskDescription: String,
                        goalId: Long, taskOwnerId: Long?, taskDepth: Int) : Pair<Long, Boolean> {
        return withContext(Dispatchers.IO) {

            if (taskDepth >= Constants.MAX_TASK_DEPTH)
                return@withContext Pair<Long, Boolean>(Constants.IGNORE_ID_AS_LONG, true)

            db.taskDataQueries.insertOneTask(
                null,
                taskName,
                taskDescription,
                taskOwnerId,
                goalId,
                taskDepth
            )
            return@withContext Pair<Long, Boolean>(
                db.taskDataQueries.
                lastInsertRowId().executeAsOneOrNull()?: Constants.IGNORE_ID_AS_LONG,
                false
            )
        }
    }

    suspend fun putAsUnfinished(taskData: TaskData) {
        return withContext(Dispatchers.IO) {
            db.taskDataQueries.insertOneEditUnfinishedTask(
                taskData.taskId,
                taskData.taskName,
                taskData.taskDescription,
                taskData.taskProgress,
                taskData.taskOwnerId,
                taskData.goalId,
                taskData.taskDepth,
                taskData.subtasksCount,
                taskData.taskDone,
                taskData.taskFailed
            )
        }
    }

    suspend fun putAllTasks(tasks: ArrayList<MutableLiveData<TaskData>>): Boolean {
        return withContext(Dispatchers.IO) {
            var retVal = false

            db.taskDataQueries.transaction {
                for (t in tasks) { t.value?.run {
                    if (taskDepth >= Constants.MAX_TASK_DEPTH) {
                        retVal = true
                        rollback()
                    }

                    db.taskDataQueries.insertOneTask(
                        null,
                        taskName,
                        taskDescription,
                        taskOwnerId,
                        goalId,
                        taskDepth
                    )
                } }
            }

            return@withContext retVal
        }
    }

    override suspend fun updateTexts(id: Long, firstText: String, secondText: String) {
        return withContext(Dispatchers.IO) {
            db.taskDataQueries.updateTaskTexts(firstText, secondText, id)
        }
    }

    suspend fun markTaskCompletion(id: Long, taskFailed: Boolean) {
        return withContext(Dispatchers.IO) {
            db.taskDataQueries.transaction {
                if (taskFailed) db.taskDataQueries.markTaskFailed(id)
                else db.taskDataQueries.markTaskDoneWell(id)

                // since i am unable to use recursive triggers, i have to resort tu
                // manually triggering recalculation of progress
                // of all parents of a given task
                val ids = db.taskDataQueries.__getIdsOfAllOwnersOfTask(id).executeAsList()
                for ( idObj in ids ) {
                    db.taskDataQueries.__triggerProgressRecalcForTask(idObj)
                }
            }
        }
    }

    override suspend fun markEditing(id: Long, isUnfinished: Boolean) {
        return withContext(Dispatchers.IO) {
            db.taskDataQueries.markTaskEdit(isUnfinished, id)
        }
    }

    override suspend fun getOneById(id: Long): TaskData? {
        return withContext(Dispatchers.IO) {
            val unfinished = db.taskDataQueries.getTaskUnfinished(id).executeAsList().last()
            if (unfinished) {
                val utd = db.taskDataQueries.selectGivenUnfinishedTask(id).executeAsOneOrNull()
                if (utd == null) null
                else TaskData(
                    utd.taskId,
                    true,
                    utd.taskName,
                    utd.taskDescription,
                    utd.taskProgress,
                    utd.taskOwnerId,
                    utd.goalId,
                    utd.taskDepth,
                    utd.subtasksCount,
                    utd.taskDone,
                    utd.taskFailed
                )
            }
            else {
                db.taskDataQueries.selectGivenTask(id).executeAsOneOrNull()
            }
        }
    }

    override suspend fun deleteById(id: Long) {
        return withContext(Dispatchers.IO) {
            db.taskDataQueries.transaction {
                val ids = db.taskDataQueries.__getIdsOfAllOwnersOfTask(id).executeAsList()
                db.taskDataQueries.deleteTask(id)
                for ( idObj in ids ) {
                    db.taskDataQueries.__triggerProgressRecalcForTask(idObj)
                }
            }
        }
    }
}