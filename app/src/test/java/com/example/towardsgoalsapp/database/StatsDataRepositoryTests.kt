package com.example.towardsgoalsapp.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.towardsgoalsapp.OwnerType
import com.example.towardsgoalsapp.database.repositories.GoalRepository
import com.example.towardsgoalsapp.database.repositories.HabitRepository
import com.example.towardsgoalsapp.database.repositories.ReminderRepository
import com.example.towardsgoalsapp.database.repositories.StatsDataRepository
import com.example.towardsgoalsapp.database.repositories.TaskRepository
import com.google.common.truth.Truth.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class StatsDataRepositoryTests {

    @Rule
    @JvmField
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).apply {
        TGDatabase.Schema.create(this)
        this.execute(null, "PRAGMA foreign_keys = ON;", 0)
    }

    private val db: TGDatabase = DatabaseObjectFactory.newDatabaseObject(driver)

    private val gRepo = GoalRepository(db)
    private val tRepo = TaskRepository(db)
    private val hRepo = HabitRepository(db)
    private val sRepo = StatsDataRepository(db)

    @Test
    fun `basic database interaction`() { runBlocking {

        val goalId1 = gRepo.addOneGoal(
            "goal1",
            "",
            5
        )
        val goalId2 = gRepo.addOneGoal(
            "goal2",
            "",
            2
        )

        val tasksIds = arrayOf(
            tRepo.addTask(
                "task1",
                "",
                goalId1,
                null,
                0,
                0
            ).first,
            tRepo.addTask(
                "task2",
                "",
                goalId1,
                null,
                0,
                1
            ).first,
            tRepo.addTask(
                "task3",
                "",
                goalId1,
                null,
                0,
                3
            ).first,
            tRepo.addTask(
                "task4",
                "",
                goalId1,
                null,
                0,
                1
            ).first,
            tRepo.addTask(
                "task1",
                "",
                goalId1,
                null,
                0,
                2
            ).first
        )
        var gotTasks = tRepo.getAllByGoalId(goalId1)
        assertThat(tRepo.getAllByGoalId(goalId2)).isEmpty()
        assertThat(gotTasks.map { it.taskFailed })
            .containsExactly(false, false, false, false, false)
        assertThat(gotTasks.map { it.taskPriority })
            .containsExactly(0, 1, 3, 1, 2)
        assertThat(sRepo.getAllMarkableTaskStatsDataByGoal(goalId1)).isEmpty()

        val now = Instant.now()
        tRepo.markTaskCompletion(tasksIds[0], true)
        sRepo.putNewMarkableTaskStatsData(
            tasksIds[0],
            goalId1,
            true,
            now
        )
        tRepo.markTaskCompletion(tasksIds[1], true)
        sRepo.putNewMarkableTaskStatsData(
            tasksIds[1],
            goalId1,
            true,
            now.plus(1, ChronoUnit.DAYS)
        )
        tRepo.markTaskCompletion(tasksIds[2], false)
        sRepo.putNewMarkableTaskStatsData(
            tasksIds[2],
            goalId1,
            false,
            now.plus(2, ChronoUnit.DAYS)
        )
        tRepo.markTaskCompletion(tasksIds[4], true)
        sRepo.putNewMarkableTaskStatsData(
            tasksIds[4],
            goalId1,
            true,
            now.plus(3, ChronoUnit.DAYS)
        ) // we skipped task4

        gotTasks = tRepo.getAllByGoalId(goalId1)
        assertThat(gotTasks.map { it.taskFailed })
            .containsExactly(true, true, false, false, true)
        assertThat(gotTasks.map { it.taskPriority })
            .containsExactly(0, 1, 3, 1, 2)
        val tData = sRepo.getAllMarkableTaskStatsDataByGoal(goalId1)
        assertThat(sRepo.getAllMarkableTaskStatsDataByGoal(goalId2)).isEmpty()
        assertThat(tData.map { it.taskPriority })
            .containsExactly(0L, 1L, 3L, 2L)
        assertThat(tData.map { it.goalId }.toSet()).containsExactly(goalId1)
        assertThat(tData.map { it.taskId })
            .containsExactly(tasksIds[0], tasksIds[1], tasksIds[2], tasksIds[4])
        assertThat(tData.map { it.taskFailed })
            .containsExactly(true, true, false, true)
        assertThat(tData.map { it.addedOn })
            .containsExactly(
                now,
                now.plus(1, ChronoUnit.DAYS),
                now.plus(2, ChronoUnit.DAYS),
                now.plus(3, ChronoUnit.DAYS)
            )

        for (tid in tasksIds) {
            tRepo.deleteById(tid)
        }
        gotTasks = tRepo.getAllByGoalId(goalId1)
        assertThat(gotTasks).isEmpty()
        assertThat(sRepo.getAllMarkableTaskStatsDataByGoal(goalId1)).isEmpty()

        val habitId1 = hRepo.addHabit(
            "habit",
            "",
            1,
            1,
            goalId1
        )

        assertThat(sRepo.getAllHabitStatsDataByHabit(habitId1)).isEmpty()
        sRepo.putNewHabitStatsData(
            habitId1,
            goalId1,
            true,
            false,
            now
        )
        sRepo.putNewHabitStatsData(
            habitId1,
            goalId1,
            true,
            false,
            now.plus(1, ChronoUnit.DAYS)
        )
        sRepo.putNewHabitStatsData(
            habitId1,
            goalId1,
            false,
            true,
            now.plus(2, ChronoUnit.DAYS)
        )
        sRepo.putNewHabitStatsData(
            habitId1,
            goalId1,
            true,
            false,
            now.plus(3, ChronoUnit.DAYS)
        )
        val hData = sRepo.getAllHabitStatsDataByHabit(habitId1)
        assertThat(hData).isNotEmpty()
        assertThat(hData.map { it.goalId }.toSet()).containsExactly(goalId1)
        assertThat(hData.map { it.habitId }.toSet()).containsExactly(habitId1)
        assertThat(hData.map { it.addedOn }).containsExactly(
            now,
            now.plus(1, ChronoUnit.DAYS),
            now.plus(2, ChronoUnit.DAYS),
            now.plus(3, ChronoUnit.DAYS)
        )
        assertThat(hData.map { it.doneNotWell }).containsExactly(
            false, false, true, false
        )
        assertThat(hData.map { it.doneWell }).containsExactly(
            true, true, false, true
        )

        hRepo.deleteById(habitId1)
        assertThat(sRepo.getAllHabitStatsDataByHabit(habitId1)).isEmpty()

        gRepo.deleteById(goalId2)
        gRepo.deleteById(goalId1)

    } }

}