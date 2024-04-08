package com.example.towardsgoalsapp.database.repositories

import androidx.lifecycle.MutableLiveData
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.OwnerType
import com.example.towardsgoalsapp.database.TGDatabase
import com.example.towardsgoalsapp.database.TaskData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaskRepository(private val db: TGDatabase): OwnedByOneTypeOnlyOwnerUserData {
    override suspend fun getAllByOwnerId(ownerId: Long, allowUnfinished: Boolean): ArrayList<TaskData> {
        return withContext(Dispatchers.IO) {
            val all = db.taskDataQueries.selectAllOfGoal(ownerId).executeAsList()
                .toCollection(ArrayList())
            val filtered =
                if (allowUnfinished) all else all.filter { td -> !td.taskEditUnfinished }
            filtered.toCollection(ArrayList())
        }
    }

    suspend fun getAllByGoalId(goalId: Long, allowUnfinished: Boolean = true): ArrayList<TaskData>
            = getAllByOwnerId(goalId, allowUnfinished)

    suspend fun getAllByTaskOwnerId(ownerId: Long): ArrayList<TaskData> {
        return withContext(Dispatchers.IO) {
            db.taskDataQueries.selectAllOfOwnerTask(ownerId).executeAsList()
                .toCollection(ArrayList())
        }
    }

    suspend fun addTask(taskName: String, taskDescription: String,
                        goalId: Long, taskOwnerId: Long?, taskDepth: Int, taskPriority: Int) : Pair<Long, Boolean> {

        return withContext(Dispatchers.IO) {

            if (taskDepth >= Constants.MAX_TASK_DEPTH)
                return@withContext Pair<Long, Boolean>(Constants.IGNORE_ID_AS_LONG, false)

            db.taskDataQueries.insertOneTask(
                null,
                taskName,
                taskDescription,
                taskOwnerId,
                goalId,
                taskDepth,
                taskPriority
            )
            return@withContext Pair<Long, Boolean>(
                db.taskDataQueries.
                lastInsertRowId().executeAsOneOrNull()?: Constants.IGNORE_ID_AS_LONG,
                true
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
                taskData.taskFailed,
                taskData.taskPriority
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
                        taskDepth,
                        taskPriority
                    )
                } }
            }

            return@withContext retVal
        }
    }

    suspend fun updatePriority(id: Long, priority: Int) {
        return withContext(Dispatchers.IO) {
            db.taskDataQueries.updateTaskPriority(priority, id)
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

                // since i am unable to use recursive triggers, i have to resort to
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

    override suspend fun getOneById(id: Long, allowUnfinished: Boolean): TaskData? {
        return withContext(Dispatchers.IO) {
            val unfinished = db.taskDataQueries.getTaskUnfinished(id).executeAsOneOrNull()
                ?: return@withContext null
            if (unfinished) {
                if (!allowUnfinished) return@withContext null
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
                    utd.taskFailed,
                    utd.taskPriority
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
                val reminder = db.reminderDataQueries.selectOf(id, OwnerType.TYPE_TASK)
                    .executeAsOneOrNull()
                reminder?.run { db.reminderDataQueries.deleteReminder(reminder.remId) }
                db.taskDataQueries.deleteTask(id)
                for ( idObj in ids ) {
                    db.taskDataQueries.__triggerProgressRecalcForTask(idObj)
                }
            }
        }
    }
}