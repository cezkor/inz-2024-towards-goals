package com.example.towardsgoalsapp.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.database.repositories.GoalRepository
import com.example.towardsgoalsapp.database.repositories.TaskRepository
import com.google.common.truth.Truth.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class TaskRepositoryTests {

    @Rule
    @JvmField
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).apply {
        TGDatabase.Schema.create(this)
        this.execute(null, "PRAGMA foreign_keys = ON;", 0)
    }

    private val db: TGDatabase = DatabaseObjectFactory.newDatabaseObject(driver)

    private val mainRepo: TaskRepository = TaskRepository(db)

    private val goalRepo: GoalRepository = GoalRepository(db)

    private val arrayOfMutableTasks: ArrayList<MutableLiveData<TaskData>>
        = java.util.ArrayList()

    @Test
    fun `basic database interaction`() {
        runBlocking {
            arrayOfMutableTasks.clear()

            val ownerGoalId = goalRepo.addOneGoal(
                "owner goal name", "descr", 0
            )

            val taskIdAndIsDepthBadPair1 = mainRepo.addTask(
                "task 1",
                "descr 1",
                ownerGoalId,
                null,
                0
            )
            val taskId1 = taskIdAndIsDepthBadPair1.first
            assertThat(taskIdAndIsDepthBadPair1.second).isEqualTo(false)
            var taskList =  mainRepo.getAllByGoalId(ownerGoalId)
            assertThat(taskList.size).isEqualTo(1)
            assertThat(taskList[0].goalId).isEqualTo(ownerGoalId)
            assertThat(taskList[0].taskId).isEqualTo(taskId1)
            assertThat(taskList[0].taskName).isEqualTo("task 1")
            assertThat(taskList[0].taskDescription).isEqualTo("descr 1")
            assertThat(taskList[0].taskEditUnfinished).isEqualTo(false)
            assertThat(taskList[0].taskOwnerId).isNull()
            assertThat(taskList[0].subtasksCount).isEqualTo(0L)
            assertThat(taskList[0].taskDone).isEqualTo(false)
            assertThat(taskList[0].taskFailed).isEqualTo(false)
            assertThat(taskList[0].taskProgress).isEqualTo(0.0)

            mainRepo.updateTexts(
                taskId1, "super text 1", "super descr 1"
            )
            val taskOrNull = mainRepo.getOneById(taskId1)
            assertThat(taskOrNull).isNotNull()
            taskList[0]=taskOrNull!!
            assertThat(taskList[0].goalId).isEqualTo(ownerGoalId)
            assertThat(taskList[0].taskName).isEqualTo("super text 1")
            assertThat(taskList[0].taskDescription).isEqualTo("super descr 1")
            assertThat(taskList[0].taskEditUnfinished).isEqualTo(false)
            assertThat(taskList[0].taskOwnerId).isNull()
            assertThat(taskList[0].subtasksCount).isEqualTo(0L)
            assertThat(taskList[0].taskDone).isEqualTo(false)
            assertThat(taskList[0].taskFailed).isEqualTo(false)
            assertThat(taskList[0].taskProgress).isEqualTo(0.0)

            mainRepo.deleteById(taskId1)
            taskList = mainRepo.getAllByGoalId(ownerGoalId)
            assertThat(taskList.size).isEqualTo(0)

            val taskIdAndIsDepthBadPair2 = mainRepo.addTask(
                "task 2",
                "descr 2",
                ownerGoalId,
                null,
                0
            )
            val taskId2 = taskIdAndIsDepthBadPair2.first
            assertThat(taskIdAndIsDepthBadPair2.second).isEqualTo(false)
            val hab2OrNull = mainRepo.getOneById(taskId2)
            assertThat(hab2OrNull).isNotNull()
            assertThat(hab2OrNull!!.goalId).isEqualTo(ownerGoalId)
            assertThat(hab2OrNull.taskId).isEqualTo(taskId2)
            assertThat(hab2OrNull.taskName).isEqualTo("task 2")
            assertThat(hab2OrNull.taskDescription).isEqualTo("descr 2")
            assertThat(hab2OrNull.taskEditUnfinished).isEqualTo(false)
            assertThat(hab2OrNull.taskOwnerId).isNull()
            assertThat(hab2OrNull.subtasksCount).isEqualTo(0L)
            assertThat(hab2OrNull.taskDone).isEqualTo(false)
            assertThat(hab2OrNull.taskFailed).isEqualTo(false)
            assertThat(hab2OrNull.taskProgress).isEqualTo(0.0)

            arrayOfMutableTasks.addAll(
                arrayListOf(
                    TaskData(
                        Constants.IGNORE_ID_AS_LONG,
                        false,
                        "subtask 11",
                        "descr 11",
                        0.0,
                        taskId2,
                        ownerGoalId,
                        1,
                        0,
                        taskDone = false,
                        taskFailed = false
                    ),
                    TaskData(
                        Constants.IGNORE_ID_AS_LONG,
                        false,
                        "subtask 12",
                        "descr 12",
                        0.0,
                        taskId2,
                        ownerGoalId,
                        1,
                        0,
                        taskDone = false,
                        taskFailed = false
                    ),
                    TaskData(
                        Constants.IGNORE_ID_AS_LONG,
                        false,
                        "subtask 13",
                        "descr 13",
                        0.0,
                        taskId2,
                        ownerGoalId,
                        1,
                        0,
                        taskDone = false,
                        taskFailed = false
                    ),
                    TaskData(
                        Constants.IGNORE_ID_AS_LONG,
                        false,
                        "subtask 14",
                        "descr 14",
                        0.0,
                        taskId2,
                        ownerGoalId,
                        1,
                        0,
                        taskDone = false,
                        taskFailed = false
                    )
                ).map { MutableLiveData(it) }
            )

            val failedToPutAll = mainRepo.putAllTasks(arrayOfMutableTasks)
            assertThat(failedToPutAll).isEqualTo(false)

            taskList = mainRepo.getAllByOwnerId(ownerGoalId) // should be the same as ...ByGoalId
            assertThat(taskList.size).isEqualTo(5)
            assertThat(taskList.map { it.taskName }).containsExactly(
                "task 2", "subtask 11", "subtask 12", "subtask 13", "subtask 14"
            )
            assertThat(taskList.map { it.taskDescription }).containsExactly(
                "descr 2", "descr 11", "descr 12", "descr 13", "descr 14"
            )
            assertThat(taskList.map { it.subtasksCount }).containsExactly(
                4L, 0L, 0L, 0L, 0L
            )
            assertThat(taskList.map { it.taskOwnerId }).containsExactly(
                null, taskId2, taskId2, taskId2, taskId2
            )

            val taskIdAndIsDepthBadPair3 = mainRepo.addTask(
                "subsubtask 15", "descr 15",
                ownerGoalId, taskList[3].taskId, 2
            )
            assertThat(taskIdAndIsDepthBadPair3.second).isEqualTo(false)

            val shouldNotBeMade = mainRepo.addTask(
                "_____", "_____",
                ownerGoalId, taskList[3].taskId, Constants.MAX_TASK_DEPTH
            )
            assertThat(shouldNotBeMade.second).isEqualTo(true)

            taskList = mainRepo.getAllByOwnerId(ownerGoalId) // should be the same as ...ByGoalId
            assertThat(taskList.size).isEqualTo(6)
            assertThat(taskList.map { it.taskName }).containsExactly(
                "task 2", "subtask 11", "subtask 12", "subtask 13", "subtask 14", "subsubtask 15"
            )
            assertThat(taskList.map { it.taskDescription }).containsExactly(
                "descr 2", "descr 11", "descr 12", "descr 13", "descr 14", "descr 15"
            )
            assertThat(taskList.map { it.subtasksCount }).containsExactly(
                5L, 0L, 0L, 1L, 0L, 0L
            )
            assertThat(taskList.map { it.taskOwnerId }).containsExactly(
                null, taskId2, taskId2, taskId2, taskId2, taskList[3].taskId
            )

            mainRepo.deleteById(taskId2) // trigger should delete also all subtasks

            taskList = mainRepo.getAllByOwnerId(ownerGoalId)
            assertThat(taskList.size).isEqualTo(0)

            goalRepo.deleteById(ownerGoalId)
        }

    }

    @Test
    fun `unfinished data handled properly`() { runBlocking {

        arrayOfMutableTasks.clear()
        val ownerGoalId = goalRepo.addOneGoal(
            "owner goal name", "descr", 0
        )

        var taskList = mainRepo.getAllByOwnerId(ownerGoalId)
        assertThat(taskList.size).isEqualTo(0)

        val taskIdAndIsDepthBadPair1 = mainRepo.addTask(
            "task 1",
            "descr 1",
            ownerGoalId,
            null,
            0
        )
        val taskId1 = taskIdAndIsDepthBadPair1.first
        assertThat(taskIdAndIsDepthBadPair1.second).isEqualTo(false)
        val taskIdAndIsDepthBadPair2 = mainRepo.addTask(
            "task 2",
            "descr 2",
            ownerGoalId,
            null,
            0
        )
        val taskId2 = taskIdAndIsDepthBadPair2.first
        assertThat(taskIdAndIsDepthBadPair2.second).isEqualTo(false)

        taskList = mainRepo.getAllByGoalId(ownerGoalId)
        assertThat(taskList.map { it.taskEditUnfinished }).containsExactly(false, false)
        assertThat(taskList[0].taskId).isEqualTo(taskId1)

        val editedTask = TaskData(
            taskList[0].taskId,
            false,
            "edited task name 1",
            "edited task descr 1",
            taskList[0].taskProgress,
            taskList[0].taskOwnerId,
            taskList[0].goalId,
            taskList[0].taskDepth,
            taskList[0].subtasksCount,
            taskList[0].taskDone,
            taskList[0].taskFailed
        )
        mainRepo.markEditing(taskId1, true)
        mainRepo.putAsUnfinished(editedTask)
        val hopefullyUnfinishedTask = mainRepo.getOneById(taskId1)
        assertThat(hopefullyUnfinishedTask).isNotNull()
        assertThat(hopefullyUnfinishedTask!!).isInstanceOf(TaskData::class.java)
        assertThat(hopefullyUnfinishedTask.taskEditUnfinished).isEqualTo(true)
        assertThat(hopefullyUnfinishedTask.taskName).isEqualTo("edited task name 1")
        assertThat(hopefullyUnfinishedTask.taskDescription)
            .isEqualTo("edited task descr 1")
        assertThat(hopefullyUnfinishedTask.taskFailed).isEqualTo(taskList[0].taskFailed)
        assertThat(hopefullyUnfinishedTask.taskDone).isEqualTo(taskList[0].taskDone)
        assertThat(hopefullyUnfinishedTask.taskId).isNotEqualTo(taskId2)
        assertThat(hopefullyUnfinishedTask.taskId).isEqualTo(taskId1)

        mainRepo.markEditing(taskId1, false)

        assertThat(
            db.habitDataQueries.selectGivenUnfinishedHabit(taskId1).executeAsOneOrNull()
        ).isNull()

        val hopefullyFinishedTask = mainRepo.getOneById(taskId1)
        assertThat(hopefullyFinishedTask).isNotNull()
        assertThat(hopefullyFinishedTask!!).isInstanceOf(TaskData::class.java)
        assertThat(hopefullyFinishedTask.taskEditUnfinished).isEqualTo(false)
        assertThat(hopefullyFinishedTask.taskName).isEqualTo("task 1")
        assertThat(hopefullyFinishedTask.taskDescription).isEqualTo("descr 1")
        assertThat(hopefullyFinishedTask.taskFailed).isEqualTo(taskList[0].taskFailed)
        assertThat(hopefullyFinishedTask.taskDone).isEqualTo(taskList[0].taskDone)
        assertThat(hopefullyFinishedTask.taskId).isNotEqualTo(taskId2)
        assertThat(hopefullyFinishedTask.taskId).isEqualTo(taskId1)

        mainRepo.deleteById(taskId1)
        mainRepo.deleteById(taskId2)
        assertThat(
            db.habitDataQueries.selectGivenUnfinishedHabit(taskId1).executeAsOneOrNull()
        ).isNull()

        goalRepo.deleteById(ownerGoalId)
    } }

    @Test
    fun `goal and tasks progresses calculated properly`() { runBlocking {

        arrayOfMutableTasks.clear()
        val ownerGoalId = goalRepo.addOneGoal(
            "owner goal name", "descr", 0
        )

        var goal: GoalData? = goalRepo.getOneById(ownerGoalId)
        assertThat(goal).isNotNull()
        assertThat(goal!!.goalProgress).isEqualTo(0.0)

        var taskList = mainRepo.getAllByOwnerId(ownerGoalId)
        assertThat(taskList.size).isEqualTo(0)

        val hTaskPair1 = mainRepo.addTask(
            "highest task 1", "descr 1",
            ownerGoalId, null, 0
        )
        assertThat(hTaskPair1.second).isEqualTo(false)

        val hTaskPair2 = mainRepo.addTask(
            "highest task 2", "descr 2",
            ownerGoalId, null, 0
        )
        assertThat(hTaskPair2.second).isEqualTo(false)

        val sTaskPair1 = mainRepo.addTask(
            "sub task 3", "descr 3",
            ownerGoalId, hTaskPair1.first, 0 + 1
        )
        assertThat(sTaskPair1.second).isEqualTo(false)

        val sTaskPair2 = mainRepo.addTask(
            "sub task 4", "descr 4",
            ownerGoalId, hTaskPair1.first, 0 + 1
        )
        assertThat(sTaskPair2.second).isEqualTo(false)

        val ssTaskPair1 = mainRepo.addTask(
            "sub task 5", "descr 5",
            ownerGoalId, sTaskPair1.first, 0 + 1 + 1
        )
        assertThat(ssTaskPair1.second).isEqualTo(false)

        val ssTaskPair2 = mainRepo.addTask(
            "sub task 6", "descr 6",
            ownerGoalId, sTaskPair1.first, 0 + 1 + 1
        )
        assertThat(ssTaskPair2.second).isEqualTo(false)

        taskList = mainRepo.getAllByOwnerId(ownerGoalId)
        assertThat(taskList.map{ it.taskProgress }).containsExactly(
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        )
        goal = goalRepo.getOneById(ownerGoalId)
        assertThat(goal).isNotNull()
        assertThat(goal!!.goalProgress).isEqualTo(0.0)

        // task graph should look like this:
        // t1                        t2
        // |            \
        // t3           t4
        // |    \
        // t5    t6

        // marking tasks t1 and t3 should have NO EFFECT - their completion is determined by
        // their subtasks
        mainRepo.markTaskCompletion(sTaskPair1.first, false)
        goal = goalRepo.getOneById(ownerGoalId)
        assertThat(goal).isNotNull()
        assertThat(goal!!.goalProgress).isEqualTo(0.0)
        taskList = mainRepo.getAllByOwnerId(ownerGoalId)
        assertThat(taskList.map{ it.taskProgress }).containsExactly(
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        )
        mainRepo.markTaskCompletion(hTaskPair1.first, false)
        goal = goalRepo.getOneById(ownerGoalId)
        assertThat(goal).isNotNull()
        assertThat(goal!!.goalProgress).isEqualTo(0.0)
        taskList = mainRepo.getAllByOwnerId(ownerGoalId)
        assertThat(taskList.map{ it.taskProgress }).containsExactly(
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        )


        mainRepo.markTaskCompletion(hTaskPair2.first, false)
        taskList = mainRepo.getAllByOwnerId(ownerGoalId)
        assertThat(taskList.map{ it.taskProgress }).containsExactly(
            0.0, 1.0, 0.0, 0.0, 0.0, 0.0
        )
        goal = goalRepo.getOneById(ownerGoalId)
        assertThat(goal).isNotNull()
        assertThat(goal!!.goalProgress).isEqualTo(0.5)

        mainRepo.markTaskCompletion(ssTaskPair2.first, false)
        taskList = mainRepo.getAllByOwnerId(ownerGoalId)
        assertThat(taskList.map{ it.taskProgress }).containsExactly(
            0.25, 1.0, 0.5, 0.0, 0.0, 1.0
        )
        goal = goalRepo.getOneById(ownerGoalId)
        assertThat(goal).isNotNull()
        assertThat(goal!!.goalProgress).isEqualTo((0.25 + 1.0)/2)

        mainRepo.markTaskCompletion(sTaskPair2.first, false)
        taskList = mainRepo.getAllByOwnerId(ownerGoalId)
        assertThat(taskList.map{ it.taskProgress }).containsExactly(
            0.75, 1.0, 0.5, 1.0, 0.0, 1.0
        )
        goal = goalRepo.getOneById(ownerGoalId)
        assertThat(goal).isNotNull()
        assertThat(goal!!.goalProgress).isEqualTo((0.75 + 1.0)/2)
        val hopefullyHalfTask = mainRepo.getOneById(sTaskPair1.first)
        assertThat(hopefullyHalfTask).isNotNull()
        assertThat(hopefullyHalfTask!!.taskProgress).isEqualTo(0.5)
        assertThat(hopefullyHalfTask.taskDone).isEqualTo(false)
        assertThat(hopefullyHalfTask.taskFailed).isEqualTo(false)

        assertThat(taskList[4].taskId).isEqualTo(ssTaskPair1.first)
        assertThat(taskList[4].taskProgress).isEqualTo(0.0)
        mainRepo.deleteById(ssTaskPair1.first)
        taskList = mainRepo.getAllByOwnerId(ownerGoalId)
        assertThat(taskList.map{ it.taskProgress }).containsExactly(
            1.0, 1.0, 1.0, 1.0, 1.0
        )
        val hopefullyCompletedTask = mainRepo.getOneById(hTaskPair1.first)
        assertThat(hopefullyCompletedTask).isNotNull()
        assertThat(hopefullyCompletedTask!!.taskDone).isEqualTo(true)
        assertThat(hopefullyCompletedTask.taskFailed).isEqualTo(false)

        val hopefullyNowCompletedTask = mainRepo.getOneById(sTaskPair1.first)
        assertThat(hopefullyNowCompletedTask).isNotNull()
        assertThat(hopefullyNowCompletedTask!!.taskDone).isEqualTo(true)
        assertThat(hopefullyNowCompletedTask.taskFailed).isEqualTo(false)

        mainRepo.deleteById(hTaskPair1.first)
        mainRepo.deleteById(hTaskPair2.first)
        goalRepo.deleteById(ownerGoalId)
    } }

}