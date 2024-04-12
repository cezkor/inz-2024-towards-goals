package org.cezkor.towardsgoalsapp.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.towardsgoalsapp.database.GoalData
import com.example.towardsgoalsapp.database.TGDatabase
import com.example.towardsgoalsapp.database.repositories.GoalRepository
import com.google.common.truth.Truth.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class GoalRepositoryTests {

    @Rule
    @JvmField
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).apply {
        TGDatabase.Schema.create(this)
        this.execute(null, "PRAGMA foreign_keys = ON;", 0)
    }

    private val db: TGDatabase = DatabaseObjectFactory.newDatabaseObject(driver)

    private val repo: GoalRepository =  GoalRepository(db)

    private val arrayOfMutableGoalData: ArrayList<MutableLiveData<GoalData>>
        = java.util.ArrayList()

    @Test
    fun `basic database interaction`() {
        runBlocking {
            var goalList: ArrayList<GoalData> = repo.getAllGoals()
            assertThat(goalList.size).isEqualTo(0)

            val goalId1 = repo.addOneGoal(
                "goal 1",
                "descr 1",
                0
            )
            goalList =  repo.getAllGoals()
            assertThat(goalList.size).isEqualTo(1)
            assertThat(goalList[0].goalId).isEqualTo(goalId1)
            assertThat(goalList[0].goalName).isEqualTo("goal 1")
            assertThat(goalList[0].goalDescription).isEqualTo("descr 1")
            assertThat(goalList[0].pageNumber).isEqualTo(0)
            assertThat(goalList[0].goalProgress).isEqualTo(0.0)
            assertThat(goalList[0].goalEditUnfinished).isEqualTo(false)

            repo.updateTexts(
                goalId1, "super text 1", "super descr 1"
            )
            val goalOrNull = repo.getOneById(goalId1)
            assertThat(goalOrNull).isNotNull()
            goalList[0]=goalOrNull!!
            assertThat(goalList.size).isEqualTo(1)
            assertThat(goalList[0].goalId).isEqualTo(goalId1)
            assertThat(goalList[0].goalName).isEqualTo("super text 1")
            assertThat(goalList[0].goalDescription).isEqualTo("super descr 1")
            assertThat(goalList[0].pageNumber).isEqualTo(0)
            assertThat(goalList[0].goalProgress).isEqualTo(0.0)
            assertThat(goalList[0].goalEditUnfinished).isEqualTo(false)

            repo.deleteById(goalId1)
            goalList = repo.getAllGoals()
            assertThat(goalList.size).isEqualTo(0)

            val goalId2 = repo.addOneGoal(
                "goal 2", "descr 2", 1
            )
            val goal2OrNull = repo.getOneById(goalId2)
            assertThat(goal2OrNull).isNotNull()
            assertThat(goal2OrNull!!.goalId).isEqualTo(goalId2)
            assertThat(goal2OrNull.goalName).isEqualTo("goal 2")
            assertThat(goal2OrNull.goalDescription).isEqualTo("descr 2")
            assertThat(goal2OrNull.pageNumber).isEqualTo(1)
            assertThat(goal2OrNull.goalProgress).isEqualTo(0.0)
            assertThat(goal2OrNull.goalEditUnfinished).isEqualTo(false)

            val goalId3 = repo.addOneGoal(
                "goal 3",
                "descr 3",
                0
            )
            val goal3OrNull = repo.getOneById(goalId3)
            assertThat(goal3OrNull).isNotNull()

            goalList =  repo.getAllGoals()
            assertThat(goalList.size).isEqualTo(2)
            assertThat(goalList.map { it -> it.pageNumber }).isInOrder()
            assertThat(goalList.map { it -> it.pageNumber }).containsExactly(0, 1)

            repo.deleteById(goalId2)
            repo.deleteById(goalId3)
            goalList = repo.getAllGoals()
            assertThat(goalList.size).isEqualTo(0)
        }

    }

    @Test
    fun `unfinished data handled properly`() { runBlocking {

        var goalList: ArrayList<GoalData> = repo.getAllGoals()
        assertThat(goalList.size).isEqualTo(0)

        val goalId1 = repo.addOneGoal(
            "goal 1",
            "descr 1",
            0
        )
        val goalId2 = repo.addOneGoal(
            "goal 2",
            "descr 2",
            1
        )
        goalList = repo.getAllGoals()
        assertThat(goalList.map { it.goalEditUnfinished }).containsExactly(false, false)
        assertThat(goalList[0].goalId).isEqualTo(goalId1)

        val editedGoal = GoalData(
            goalId1,
            false, // should be ignored
            "edited name 1",
            "edited description 1",
            goalList[0].goalProgress,
            goalList[0].pageNumber
        )
        repo.markEditing(goalId1, true)
        repo.putAsUnfinished(editedGoal)
        val hopefullyUnfinishedGoal = repo.getOneById(goalId1)
        assertThat(hopefullyUnfinishedGoal).isNotNull()
        assertThat(hopefullyUnfinishedGoal!!).isInstanceOf(GoalData::class.java)
        assertThat(hopefullyUnfinishedGoal.goalEditUnfinished).isEqualTo(true)
        assertThat(hopefullyUnfinishedGoal.goalName).isEqualTo("edited name 1")
        assertThat(hopefullyUnfinishedGoal.goalDescription).isEqualTo("edited description 1")
        assertThat(hopefullyUnfinishedGoal.goalId).isNotEqualTo(goalId2)
        assertThat(hopefullyUnfinishedGoal.goalId).isEqualTo(goalId1)
        assertThat(hopefullyUnfinishedGoal.goalProgress).isEqualTo(0.0)
        assertThat(hopefullyUnfinishedGoal.pageNumber).isEqualTo(0)

        repo.markEditing(goalId1, false)

        assertThat(
            db.goalDataQueries.selectGivenUnfinishedGoal(goalId1).executeAsOneOrNull()
        ).isNull()

        val hopefullyFinishedGoal = repo.getOneById(goalId1)
        assertThat(hopefullyFinishedGoal).isNotNull()
        assertThat(hopefullyFinishedGoal!!).isInstanceOf(GoalData::class.java)
        assertThat(hopefullyFinishedGoal.goalEditUnfinished).isEqualTo(false)
        assertThat(hopefullyFinishedGoal.goalName).isEqualTo("goal 1")
        assertThat(hopefullyFinishedGoal.goalDescription).isEqualTo("descr 1")
        assertThat(hopefullyFinishedGoal.goalProgress).isEqualTo(0.0)
        assertThat(hopefullyFinishedGoal.pageNumber).isEqualTo(0)

        repo.deleteById(goalId2)
        repo.deleteById(goalId1)
        assertThat(
            db.goalDataQueries.selectGivenUnfinishedGoal(goalId1).executeAsOneOrNull()
        ).isNull()
    } }

}