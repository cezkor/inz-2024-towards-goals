package com.example.towardsgoalsapp.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.towardsgoalsapp.OwnerType
import com.example.towardsgoalsapp.database.repositories.ReminderRepository
import com.google.common.truth.Truth.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class ReminderRepositoryTests {

    @Rule
    @JvmField
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).apply {
        TGDatabase.Schema.create(this)
        this.execute(null, "PRAGMA foreign_keys = ON;", 0)
    }

    private val db: TGDatabase = DatabaseObjectFactory.newDatabaseObject(driver)

    private val repo: ReminderRepository = ReminderRepository(db)

    @Test
    fun `basic database interaction`() {
        runBlocking {
            var remList: ArrayList<ReminderData> =
                repo.getAll().toCollection(ArrayList<ReminderData>())
            assertThat(remList.size).isEqualTo(0)

            val instant1 = Instant.now()
            val remId1 = repo.addReminder(
                instant1,
                OwnerType.TYPE_TASK,
                100 // foreign keys are not enforced !!!
            )
            remList = repo.getAll().toCollection(ArrayList<ReminderData>())
            assertThat(remList.size).isEqualTo(1)
            assertThat(remList[0].remId).isEqualTo(remId1)

            val remOrNull = repo.getOneById(remId1)
            assertThat(remOrNull).isNotNull()
            remList[0] = remOrNull!!
            assertThat(remList.size).isEqualTo(1)
            assertThat(remList[0].remId).isEqualTo(remId1)
            assertThat(remList[0].remindOn).isEqualTo(instant1)
            assertThat(remList[0].ownerId).isEqualTo(100)
            assertThat(remList[0].ownerType).isEqualTo(OwnerType.TYPE_TASK)

            repo.deleteById(remId1)
            remList = repo.getAll().toCollection(ArrayList<ReminderData>())
            assertThat(remList.size).isEqualTo(0)

            val instant2 = Instant.now()
            val remId2 = repo.addReminder(
                instant2,
                OwnerType.TYPE_HABIT,
                100 // foreign keys are not enforced !!!
            )
            remList = repo.getAll().toCollection(ArrayList<ReminderData>())
            assertThat(remList.size).isEqualTo(1)
            assertThat(remList[0].remId).isEqualTo(remId2)

            val rem2OrNull = repo.getOneById(remId2)
            assertThat(rem2OrNull).isNotNull()
            remList[0] = rem2OrNull!!
            assertThat(remList.size).isEqualTo(1)
            assertThat(remList[0].remId).isEqualTo(remId2)
            assertThat(remList[0].remindOn).isEqualTo(instant2)
            assertThat(remList[0].ownerId).isEqualTo(100)
            assertThat(remList[0].ownerType).isEqualTo(OwnerType.TYPE_HABIT)

            val remId3 = repo.addReminder(
                instant1, // !!!!
                OwnerType.TYPE_TASK,
                100 // foreign keys are not enforced !!!
            )
            remList = repo.getAll().toCollection(ArrayList<ReminderData>())
            assertThat(remList.size).isEqualTo(2)
            assertThat(remList.map { it -> it.remindOn }).containsExactly(instant1, instant2)
            assertThat(remList.map { it -> it.ownerType })
                .containsExactly(OwnerType.TYPE_TASK, OwnerType.TYPE_HABIT)

            val onlyHabitRem = repo.getAllByOwnerTypeAndId(OwnerType.TYPE_HABIT, 100L)
            assertThat(onlyHabitRem).isNotNull()
            assertThat(onlyHabitRem!!.ownerType).isEqualTo(OwnerType.TYPE_HABIT)
            val onlyTaskRem = repo.getAllByOwnerTypeAndId(OwnerType.TYPE_TASK, 100L)
            assertThat(onlyTaskRem).isNotNull()
            assertThat(onlyTaskRem!!.ownerType).isEqualTo(OwnerType.TYPE_TASK)

            val now = Instant.now()
            var rem2 = repo.getOneById(remId2)
            assertThat(rem2).isNotNull()
            assertThat(rem2!!.remindOn).isNotNull()

            repo.updateRemindOn(remId2, now)
            rem2 = repo.getOneById(remId2)
            assertThat(rem2).isNotNull()
            assertThat(rem2!!.remindOn).isEqualTo(now)

            val now2 = Instant.now()
            rem2 = repo.getOneById(remId2)
            assertThat(rem2).isNotNull()
            assertThat(rem2!!.lastReminded).isNull()

            repo.updateLastReminded(remId2, now2)

            rem2 = repo.getOneById(remId2)
            assertThat(rem2).isNotNull()
            assertThat(rem2!!.lastReminded).isEqualTo(now2)

            repo.deleteById(remId2)
            repo.deleteById(remId3)
            assertThat(repo.getAll().size).isEqualTo(0)
        }

    }

}