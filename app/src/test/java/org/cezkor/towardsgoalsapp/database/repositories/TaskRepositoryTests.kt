package org.cezkor.towardsgoalsapp.database.repositories

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.cezkor.towardsgoalsapp.database.GoalData
import org.cezkor.towardsgoalsapp.database.TGDatabase
import org.cezkor.towardsgoalsapp.database.TaskData
import com.google.common.truth.Truth.*
import kotlinx.coroutines.runBlocking
import org.cezkor.towardsgoalsapp.Constants
import org.cezkor.towardsgoalsapp.database.DatabaseObjectFactory
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
                0,
                0
            )
            val taskId1 = taskIdAndIsDepthBadPair1.first
            assertThat(taskIdAndIsDepthBadPair1.second).isEqualTo(true)
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
            assertThat(taskList[0].taskPriority).isEqualTo(0)

            mainRepo.deleteById(taskId1)
            taskList = mainRepo.getAllByGoalId(ownerGoalId)
            assertThat(taskList.size).isEqualTo(0)

            val taskIdAndIsDepthBadPair2 = mainRepo.addTask(
                "task 2",
                "descr 2",
                ownerGoalId,
                null,
                0,
                1
            )
            val taskId2 = taskIdAndIsDepthBadPair2.first
            assertThat(taskIdAndIsDepthBadPair2.second).isEqualTo(true)
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
            assertThat(hab2OrNull.taskPriority).isEqualTo(1)

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
                        taskFailed = false,
                        0
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
                        taskFailed = false ,
                        2
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
                        taskFailed = false,
                        3
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
                        taskFailed = false,
                        1
                    )
                ).map { MutableLiveData(it) }
            )

            val failedToPutAll = mainRepo.putAllTasks(arrayOfMutableTasks)
            assertThat(failedToPutAll).isEqualTo(false)

            taskList = mainRepo.getAllByTaskOwnerId(taskId2)
            assertThat(taskList.size).isEqualTo(4)
            assertThat(taskList.map { it.taskName }).containsExactly(
                "subtask 11", "subtask 12", "subtask 13", "subtask 14"
            )
            assertThat(taskList.map { it.taskDescription }).containsExactly(
                "descr 11", "descr 12", "descr 13", "descr 14"
            )
            assertThat(taskList.map { it.subtasksCount }).containsExactly(
                0L, 0L, 0L, 0L
            )
            assertThat(taskList.map { it.taskOwnerId }).containsExactly(
                taskId2, taskId2, taskId2, taskId2
            )
            assertThat(mainRepo.getOneById(taskId2)!!.subtasksCount).isEqualTo(4L)

            assertThat(taskList.map {it.taskPriority}).containsExactly(
                0, 2, 3, 1
            )

            val taskIdAndIsDepthBadPair3 = mainRepo.addTask(
                "subsubtask 15", "descr 15",
                ownerGoalId, taskList[3].taskId, 2, 0
            )
            assertThat(taskIdAndIsDepthBadPair3.second).isEqualTo(true)

            val taskId3 = taskList[3].taskId

            val shouldNotBeMade = mainRepo.addTask(
                "_____", "_____",
                ownerGoalId, taskList[3].taskId, Constants.MAX_TASK_DEPTH, 0
            )
            assertThat(shouldNotBeMade.second).isEqualTo(false)

            taskList = mainRepo.getAllByTaskOwnerId(taskId2)
            assertThat(taskList.size).isEqualTo(4)
            assertThat(taskList.map { it.taskName }).containsExactly(
                "subtask 11", "subtask 12", "subtask 13", "subtask 14"
            )
            assertThat(taskList.map { it.taskDescription }).containsExactly(
                "descr 11", "descr 12", "descr 13", "descr 14"
            )
            assertThat(taskList.map { it.subtasksCount }).containsExactly(
                0L, 0L, 1L, 0L
            )
            assertThat(taskList.map { it.taskOwnerId }).containsExactly(
                taskId2, taskId2, taskId2, taskId2
            )

            taskList = mainRepo.getAllByTaskOwnerId(taskId3)
            assertThat(taskList.size).isEqualTo(1)
            assertThat(taskList.map { it.taskName }).containsExactly(
                "subsubtask 15"
            )
            assertThat(taskList.map { it.taskDescription }).containsExactly(
                "descr 15"
            )
            assertThat(taskList.map { it.subtasksCount }).containsExactly(
                0L
            )
            assertThat(taskList.map { it.taskOwnerId }).containsExactly(
                taskId3
            )

            mainRepo.updatePriority(taskId2, 100) // database should not correct this value
                // as it is applications duty to interpret and correct it
            val taskWithUpdatedPriority = mainRepo.getOneById(taskId2)
            assertThat(taskWithUpdatedPriority).isNotNull()
            assertThat(taskWithUpdatedPriority!!.taskPriority).isEqualTo(100)

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
            0,
            0
        )
        val taskId1 = taskIdAndIsDepthBadPair1.first
        assertThat(taskIdAndIsDepthBadPair1.second).isEqualTo(true)
        val taskIdAndIsDepthBadPair2 = mainRepo.addTask(
            "task 2",
            "descr 2",
            ownerGoalId,
            null,
            0,
            0
        )
        val taskId2 = taskIdAndIsDepthBadPair2.first
        assertThat(taskIdAndIsDepthBadPair2.second).isEqualTo(true)

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
            taskList[0].taskPriority,
            taskList[0].subtasksCount,
            taskList[0].taskDone,
            taskList[0].taskFailed,
            taskList[0].taskPriority
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
            ownerGoalId, null, 0, 0
        )
        assertThat(hTaskPair1.second).isEqualTo(true)

        val hTaskPair2 = mainRepo.addTask(
            "highest task 2", "descr 2",
            ownerGoalId, null, 0, 0
        )
        assertThat(hTaskPair2.second).isEqualTo(true)

        val sTaskPair1 = mainRepo.addTask(
            "sub task 3", "descr 3",
            ownerGoalId, hTaskPair1.first, 0 + 1, 0
        )
        assertThat(sTaskPair1.second).isEqualTo(true)

        val sTaskPair2 = mainRepo.addTask(
            "sub task 4", "descr 4",
            ownerGoalId, hTaskPair1.first, 0 + 1, 0
        )
        assertThat(sTaskPair2.second).isEqualTo(true)

        val ssTaskPair1 = mainRepo.addTask(
            "sub task 5", "descr 5",
            ownerGoalId, sTaskPair1.first, 0 + 1 + 1, 0
        )
        assertThat(ssTaskPair1.second).isEqualTo(true)

        val ssTaskPair2 = mainRepo.addTask(
            "sub task 6", "descr 6",
            ownerGoalId, sTaskPair1.first, 0 + 1 + 1, 0
        )
        assertThat(ssTaskPair2.second).isEqualTo(true)

        suspend fun getAll() : ArrayList<TaskData> {
            val zeroth = mainRepo.getAllByOwnerId(ownerGoalId)
            val first = mainRepo.getAllByTaskOwnerId(hTaskPair1.first)
            val second = mainRepo.getAllByTaskOwnerId(sTaskPair1.first)
            val arr = ArrayList<TaskData>()
            arr.addAll(zeroth)
            arr.addAll(first)
            arr.addAll(second)
            return arr
        }

        taskList = getAll()
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
        taskList = getAll()
        assertThat(taskList.map{ it.taskProgress }).containsExactly(
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        )
        mainRepo.markTaskCompletion(hTaskPair1.first, false)
        goal = goalRepo.getOneById(ownerGoalId)
        assertThat(goal).isNotNull()
        assertThat(goal!!.goalProgress).isEqualTo(0.0)
        taskList = getAll()
        assertThat(taskList.map{ it.taskProgress }).containsExactly(
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        )


        mainRepo.markTaskCompletion(hTaskPair2.first, false)
        taskList = getAll()
        assertThat(taskList.map{ it.taskProgress }).containsExactly(
            0.0, 1.0, 0.0, 0.0, 0.0, 0.0
        )
        goal = goalRepo.getOneById(ownerGoalId)
        assertThat(goal).isNotNull()
        assertThat(goal!!.goalProgress).isEqualTo(0.5)

        mainRepo.markTaskCompletion(ssTaskPair2.first, false)
        taskList = getAll()
        assertThat(taskList.map{ it.taskProgress }).containsExactly(
            0.25, 1.0, 0.5, 0.0, 0.0, 1.0
        )
        goal = goalRepo.getOneById(ownerGoalId)
        assertThat(goal).isNotNull()
        assertThat(goal!!.goalProgress).isEqualTo((0.25 + 1.0)/2)

        mainRepo.markTaskCompletion(sTaskPair2.first, false)
        taskList = getAll()
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
        taskList = getAll()
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