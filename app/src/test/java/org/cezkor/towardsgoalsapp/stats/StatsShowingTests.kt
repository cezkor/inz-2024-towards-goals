package org.cezkor.towardsgoalsapp.stats

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.cezkor.towardsgoalsapp.database.HabitParameter
import org.cezkor.towardsgoalsapp.database.TGDatabase
import org.cezkor.towardsgoalsapp.database.repositories.GoalRepository
import org.cezkor.towardsgoalsapp.database.repositories.HabitParamsRepository
import org.cezkor.towardsgoalsapp.database.repositories.HabitRepository
import com.google.common.truth.Truth.*
import kotlinx.coroutines.runBlocking
import org.cezkor.towardsgoalsapp.Constants
import org.cezkor.towardsgoalsapp.database.DatabaseObjectFactory
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class StatsShowingTests {

    @Rule
    @JvmField
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).apply {
        TGDatabase.Schema.create(this)
        this.execute(null, "PRAGMA foreign_keys = ON;", 0)
    }

    private val db: TGDatabase = DatabaseObjectFactory.newDatabaseObject(driver)


    private val hRepo = HabitRepository(db)
    private val hParRepo = HabitParamsRepository(db)
    private val gRepo = GoalRepository(db)

    @Test
    fun `can show param stats test`() {
        runBlocking {

            val goalId = gRepo.addOneGoal(
                "goal1",
                "",
                5
            )

            val habId1 = hRepo.addHabit(
                "habit 1",
                "",
                1,
                1,
                goalId
            )

            val habId2 = hRepo.addHabit(
                "habit 2",
                "",
                1,
                1,
                goalId
            ) // this habit will have no parameters

            val parId1 = hParRepo.addHabitParam(
                habId1,
                "par 1",
                0.1,
                null
            )

            val parId2 = hParRepo.addHabitParam(
                habId1,
                "par 2",
                0.0,
                "u"
            )


            val now = Instant.now()

            assertThat(StatsShowing.canShowHabitParamsStats(hParRepo, habId1)).isFalse()
            assertThat(StatsShowing.canShowHabitParamsStats(hParRepo, habId2)).isFalse()

            hParRepo.putValueOfParam(
                parId1,
                100.0,
                now
            )

            assertThat(StatsShowing.canShowHabitParamsStats(hParRepo, habId1)).isFalse()
            assertThat(StatsShowing.canShowHabitParamsStats(hParRepo, habId2)).isFalse()

            for (i in 2..<Constants.MINIMUM_SAMPLE) {
                hParRepo.putValueOfParam(
                    parId1,
                    100.0,
                    now
                )
                assertThat(StatsShowing.canShowHabitParamsStats(hParRepo, habId1)).isFalse()
                assertThat(StatsShowing.canShowHabitParamsStats(hParRepo, habId2)).isFalse()
            }

            hParRepo.putValueOfParam(
                parId1,
                100.0,
                now
            )
            assertThat(StatsShowing.canShowHabitParamsStats(hParRepo, habId1)).isTrue()
            assertThat(StatsShowing.canShowHabitParamsStats(hParRepo, habId2)).isFalse()

            gRepo.deleteById(goalId)
    } }
}