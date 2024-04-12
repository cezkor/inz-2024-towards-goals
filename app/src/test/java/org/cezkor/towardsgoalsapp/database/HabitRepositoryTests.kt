package org.cezkor.towardsgoalsapp.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.towardsgoalsapp.database.GoalData
import com.example.towardsgoalsapp.database.HabitData
import com.example.towardsgoalsapp.database.TGDatabase
import com.example.towardsgoalsapp.database.repositories.GoalRepository
import com.example.towardsgoalsapp.database.repositories.HabitRepository
import com.google.common.truth.Truth.*
import kotlinx.coroutines.runBlocking
import org.cezkor.towardsgoalsapp.Constants
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class HabitRepositoryTests {

    @Rule
    @JvmField
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).apply {
        TGDatabase.Schema.create(this)
        this.execute(null, "PRAGMA foreign_keys = ON;", 0)
    }

    private val db: TGDatabase = DatabaseObjectFactory.newDatabaseObject(driver)

    private val mainRepo: HabitRepository = HabitRepository(db)

    private val goalRepo: GoalRepository = GoalRepository(db)

    private val arrayOfMutableHabits: ArrayList<MutableLiveData<HabitData>>
        = java.util.ArrayList()

    @Test
    fun `basic database interaction`() {
        runBlocking {
            arrayOfMutableHabits.clear()

            val ownerGoalId = goalRepo.addOneGoal(
                "owner goal name", "descr", 0
            )

            val habId1 = mainRepo.addHabit(
                "habit 1",
                "descr 1",
                14,
                14,
                ownerGoalId
            )
            var habList =  mainRepo.getAllByGoalId(ownerGoalId)
            assertThat(habList.size).isEqualTo(1)
            assertThat(habList[0].goalId).isEqualTo(ownerGoalId)
            assertThat(habList[0].habitId).isEqualTo(habId1)
            assertThat(habList[0].habitName).isEqualTo("habit 1")
            assertThat(habList[0].habitDescription).isEqualTo("descr 1")
            assertThat(habList[0].habitTotalCount).isEqualTo(0)
            assertThat(habList[0].habitTargetCount).isEqualTo(14)
            assertThat(habList[0].habitTargetPeriod).isEqualTo(14)
            assertThat(habList[0].habitTargetCompleted).isEqualTo(false)
            assertThat(habList[0].habitEditUnfinished).isEqualTo(false)
            assertThat(habList[0].habitDoneWellCount).isEqualTo(0)
            assertThat(habList[0].habitDoneNotWellCount).isEqualTo(0)
            assertThat(habList[0].habitMarkCount).isEqualTo(0)
            assertThat(habList[0].habitLastMarkedOn).isNull()

            mainRepo.updateTexts(
                habId1, "super text 1", "super descr 1"
            )
            mainRepo.updateTargets(
                habId1, 100, 365
            )
            val habitOrNull = mainRepo.getOneById(habId1)
            assertThat(habitOrNull).isNotNull()
            habList[0]=habitOrNull!!
            assertThat(habList[0].goalId).isEqualTo(ownerGoalId)
            assertThat(habList[0].habitName).isEqualTo("super text 1")
            assertThat(habList[0].habitDescription).isEqualTo("super descr 1")
            assertThat(habList[0].habitTotalCount).isEqualTo(0)
            assertThat(habList[0].habitTargetCount).isEqualTo(100L)
            assertThat(habList[0].habitTargetPeriod).isEqualTo(365L)
            assertThat(habList[0].habitTargetCompleted).isEqualTo(false)
            assertThat(habList[0].habitEditUnfinished).isEqualTo(false)
            assertThat(habList[0].habitDoneWellCount).isEqualTo(0)
            assertThat(habList[0].habitDoneNotWellCount).isEqualTo(0)
            assertThat(habList[0].habitMarkCount).isEqualTo(0)
            assertThat(habList[0].habitLastMarkedOn).isNull()

            mainRepo.deleteById(habId1)
            habList = mainRepo.getAllByGoalId(ownerGoalId)
            assertThat(habList.size).isEqualTo(0)

            val habId2 = mainRepo.addHabit(
                "habit 2",
                "descr 2",
                1,
                1,
                ownerGoalId
            )
            val hab2OrNull = mainRepo.getOneById(habId2)
            assertThat(hab2OrNull).isNotNull()
            assertThat(hab2OrNull!!.goalId).isEqualTo(ownerGoalId)
            assertThat(hab2OrNull.habitName).isEqualTo("habit 2")
            assertThat(hab2OrNull.habitDescription).isEqualTo("descr 2")
            assertThat(hab2OrNull.habitTotalCount).isEqualTo(0)
            assertThat(hab2OrNull.habitTargetCount).isEqualTo(1)
            assertThat(hab2OrNull.habitTargetPeriod).isEqualTo(1)
            assertThat(hab2OrNull.habitTargetCompleted).isEqualTo(false)
            assertThat(hab2OrNull.habitEditUnfinished).isEqualTo(false)
            assertThat(hab2OrNull.habitDoneWellCount).isEqualTo(0)
            assertThat(hab2OrNull.habitDoneNotWellCount).isEqualTo(0)
            assertThat(hab2OrNull.habitMarkCount).isEqualTo(0)
            assertThat(hab2OrNull.habitLastMarkedOn).isNull()

            arrayOfMutableHabits.addAll(
                arrayListOf(
                    HabitData(
                        Constants.IGNORE_ID_AS_LONG,
                        false,
                        "habit 10",
                        "descr 10",
                        1,
                        7,
                        ownerGoalId,
                        0,
                        0,
                        0,
                        false,
                        0,
                        null
                    ),
                    HabitData(
                        Constants.IGNORE_ID_AS_LONG,
                        false,
                        "habit 11",
                        "descr 11",
                        1,
                        14,
                        ownerGoalId,
                        0,
                        0,
                        0,
                        false,
                        0,
                        null
                    ),
                    HabitData(
                        Constants.IGNORE_ID_AS_LONG,
                        false,
                        "habit 12",
                        "descr 12",
                        2,
                        28,
                        ownerGoalId,
                        0,
                        0,
                        0,
                        false,
                        0,
                        null
                    ),
                    HabitData(
                        Constants.IGNORE_ID_AS_LONG,
                        false,
                        "habit 13",
                        "descr 13",
                        1,
                        365,
                        ownerGoalId,
                        0,
                        0,
                        0,
                        false,
                        0,
                        null
                    )
                ).map { MutableLiveData(it) }
            )

            mainRepo.putAllHabits(arrayOfMutableHabits)

            habList = mainRepo.getAllByOwnerId(ownerGoalId) // should be the same as ...ByGoalId
            assertThat(habList.size).isEqualTo(5)
            assertThat(habList.map { it.habitName }).containsExactly(
                "habit 2", "habit 10", "habit 11", "habit 12", "habit 13",
            )
            assertThat(habList.map { it.habitDescription }).containsExactly(
                "descr 2", "descr 10", "descr 11", "descr 12", "descr 13",
            )
            assertThat(habList.map { it.habitTargetPeriod }).containsExactly(
                1L, 7L, 14L, 28L, 365L
            )
            assertThat(habList.map { it.habitTargetCount }).containsExactly(
                1L, 1L, 1L, 2L, 1L
            )

            for (el in habList) {
                mainRepo.deleteById(el.habitId)
            }
            habList = mainRepo.getAllByOwnerId(ownerGoalId)
            assertThat(habList.size).isEqualTo(0)

            goalRepo.deleteById(ownerGoalId)
        }

    }

    @Test
    fun `unfinished data handled properly`() { runBlocking {

        arrayOfMutableHabits.clear()
        val ownerGoalId = goalRepo.addOneGoal(
            "owner goal name", "descr", 0
        )

        var habList = mainRepo.getAllByOwnerId(ownerGoalId)
        assertThat(habList.size).isEqualTo(0)

        val habId1 = mainRepo.addHabit(
            "habit 1",
            "descr 1",
            7,
            14,
            ownerGoalId
        )
        val habId2 = mainRepo.addHabit(
            "habit 2",
            "descr 2",
            7,
            14,
            ownerGoalId
        )
        habList = mainRepo.getAllByGoalId(ownerGoalId)
        assertThat(habList.map { it.habitEditUnfinished }).containsExactly(false, false)
        assertThat(habList[0].habitId).isEqualTo(habId1)

        val editedHabit = HabitData(
            habId1,
            false, // should be ingored
            "edited h name 1",
            "edited h descr 1",
            2,
            28,
            ownerGoalId,
            habList[0].habitDoneWellCount,
            habList[0].habitDoneNotWellCount,
            habList[0].habitTotalCount,
            habList[0].habitTargetCompleted,
            habList[0].habitMarkCount,
            habList[0].habitLastMarkedOn
        )
        mainRepo.markEditing(habId1, true)
        mainRepo.putAsUnfinished(editedHabit)
        val hopefullyUnfinishedHabit = mainRepo.getOneById(habId1)
        assertThat(hopefullyUnfinishedHabit).isNotNull()
        assertThat(hopefullyUnfinishedHabit!!).isInstanceOf(HabitData::class.java)
        assertThat(hopefullyUnfinishedHabit.habitEditUnfinished).isEqualTo(true)
        assertThat(hopefullyUnfinishedHabit.habitName).isEqualTo("edited h name 1")
        assertThat(hopefullyUnfinishedHabit.habitDescription).isEqualTo("edited h descr 1")
        assertThat(hopefullyUnfinishedHabit.habitTargetPeriod).isEqualTo(28L)
        assertThat(hopefullyUnfinishedHabit.habitTargetCount).isEqualTo(2L)
        assertThat(hopefullyUnfinishedHabit.habitMarkCount).isEqualTo(habList[0].habitMarkCount)
        assertThat(hopefullyUnfinishedHabit.habitLastMarkedOn)
            .isEqualTo(habList[0].habitLastMarkedOn)
        assertThat(hopefullyUnfinishedHabit.habitId).isNotEqualTo(habId2)
        assertThat(hopefullyUnfinishedHabit.habitId).isEqualTo(habId1)

        mainRepo.markEditing(habId1, false)

        assertThat(
            db.habitDataQueries.selectGivenUnfinishedHabit(habId1).executeAsOneOrNull()
        ).isNull()

        val hopefullyFinishedHabit = mainRepo.getOneById(habId1)
        assertThat(hopefullyFinishedHabit).isNotNull()
        assertThat(hopefullyFinishedHabit!!).isInstanceOf(HabitData::class.java)
        assertThat(hopefullyFinishedHabit.habitEditUnfinished).isEqualTo(false)
        assertThat(hopefullyFinishedHabit.habitName).isEqualTo("habit 1")
        assertThat(hopefullyFinishedHabit.habitDescription).isEqualTo("descr 1")
        assertThat(hopefullyFinishedHabit.habitTargetPeriod).isEqualTo(14L)
        assertThat(hopefullyFinishedHabit.habitTargetCount).isEqualTo(7L)
        assertThat(hopefullyFinishedHabit.habitMarkCount).isEqualTo(habList[0].habitMarkCount)
        assertThat(hopefullyFinishedHabit.habitLastMarkedOn)
            .isEqualTo(habList[0].habitLastMarkedOn)
        assertThat(hopefullyFinishedHabit.habitId).isNotEqualTo(habId2)
        assertThat(hopefullyFinishedHabit.habitId).isEqualTo(habId1)

        mainRepo.deleteById(habId1)
        mainRepo.deleteById(habId2)
        assertThat(
            db.habitDataQueries.selectGivenUnfinishedHabit(habId1).executeAsOneOrNull()
        ).isNull()

        goalRepo.deleteById(ownerGoalId)
    } }

    @Test
    fun `goal progress calculated properly`() { runBlocking {

        arrayOfMutableHabits.clear()
        val ownerGoalId = goalRepo.addOneGoal(
            "owner goal name", "descr", 0
        )

        var goal: GoalData? = goalRepo.getOneById(ownerGoalId)
        assertThat(goal).isNotNull()
        assertThat(goal!!.goalProgress).isEqualTo(0.0)

        var habList = mainRepo.getAllByOwnerId(ownerGoalId)
        assertThat(habList.size).isEqualTo(0)

        val habId1 = mainRepo.addHabit(
            "habit 1",
            "descr 1",
            1,
            3,
            ownerGoalId
        )
        val habId2 = mainRepo.addHabit(
            "habit 2",
            "descr 2",
            3,
            3,
            ownerGoalId
        )
        habList = mainRepo.getAllByGoalId(ownerGoalId)
        assertThat(habList.map { it.habitTargetCount }).containsExactly(1L, 3L)
        assertThat(habList.map { it.habitTargetPeriod }).containsExactly(3L, 3L)
        assertThat(habList.map { it.habitDoneWellCount }).containsExactly( 0L, 0L)
        assertThat(habList.map { it.habitDoneNotWellCount }).containsExactly( 0L, 0L)
        assertThat(habList.map { it.habitTotalCount }).containsExactly(0L, 0L)
        assertThat(habList.map { it.habitTargetCompleted }).containsExactly( false, false)
        assertThat(habList.map { it.habitMarkCount }).containsExactly(0L, 0L)
        assertThat(habList.map { it.habitLastMarkedOn }).containsExactly(null, null)
        goal = goalRepo.getOneById(ownerGoalId)
        assertThat(goal!!.goalProgress).isEqualTo(0.0)

        val i1 = Instant.now()
        mainRepo.skipHabit(habId1, i1)

        habList = mainRepo.getAllByGoalId(ownerGoalId)
        assertThat(habList.map { it.habitTargetCount }).containsExactly(1L, 3L)
        assertThat(habList.map { it.habitTargetPeriod }).containsExactly(3L, 3L)
        assertThat(habList.map { it.habitDoneWellCount }).containsExactly( 0L, 0L)
        assertThat(habList.map { it.habitDoneNotWellCount }).containsExactly( 0L, 0L)
        assertThat(habList.map { it.habitTotalCount }).containsExactly(1L, 0L)
        assertThat(habList.map { it.habitTargetCompleted }).containsExactly( false, false)
        assertThat(habList.map { it.habitMarkCount }).containsExactly(0L, 0L)
        assertThat(habList.map { it.habitMarkCount }).containsExactly(1L, 0L)
        assertThat(habList.map { it.habitLastMarkedOn }).containsExactly(i1, null)
        goal = goalRepo.getOneById(ownerGoalId)
        assertThat(goal!!.goalProgress).isEqualTo(0.0)

        val i2 = Instant.now()
        mainRepo.markHabitDoneNotWell(habId1, i2)

        habList = mainRepo.getAllByGoalId(ownerGoalId)
        assertThat(habList.map { it.habitTargetCount }).containsExactly(1L, 3L)
        assertThat(habList.map { it.habitTargetPeriod }).containsExactly(3L, 3L)
        assertThat(habList.map { it.habitDoneWellCount }).containsExactly( 0L, 0L)
        assertThat(habList.map { it.habitDoneNotWellCount }).containsExactly( 1L, 0L)
        assertThat(habList.map { it.habitTotalCount }).containsExactly(2L, 0L)
        assertThat(habList.map { it.habitTargetCompleted }).containsExactly( false, false)
        assertThat(habList.map { it.habitMarkCount }).containsExactly(2L, 0L)
        assertThat(habList.map { it.habitLastMarkedOn }).containsExactly(i2, null)
        goal = goalRepo.getOneById(ownerGoalId)
        assertThat(goal!!.goalProgress).isEqualTo(0.0)

        val i3 = Instant.now()
        mainRepo.markHabitDoneWell(habId1, i3)

        habList = mainRepo.getAllByGoalId(ownerGoalId)
        assertThat(habList.map { it.habitTargetCount }).containsExactly(1L, 3L)
        assertThat(habList.map { it.habitTargetPeriod }).containsExactly(3L, 3L)
        assertThat(habList.map { it.habitDoneWellCount }).containsExactly( 0L, 0L)
        assertThat(habList.map { it.habitDoneNotWellCount }).containsExactly( 0L, 0L)
        assertThat(habList.map { it.habitTotalCount }).containsExactly(0L, 0L)
        assertThat(habList.map { it.habitTargetCompleted }).containsExactly( true, false)
        assertThat(habList.map { it.habitMarkCount }).containsExactly(3L, 0L)
        assertThat(habList.map { it.habitLastMarkedOn }).containsExactly(i3, null)
        goal = goalRepo.getOneById(ownerGoalId)
        assertThat(goal!!.goalProgress).isEqualTo(0.5)

        mainRepo.markHabitDoneWell(habId1, Instant.now())
        mainRepo.markHabitDoneWell(habId1, Instant.now())
        mainRepo.markHabitDoneWell(habId1, Instant.now())
        mainRepo.markHabitDoneWell(habId1, Instant.now())

        habList = mainRepo.getAllByGoalId(ownerGoalId)
        assertThat(habList.map { it.habitTargetCount }).containsExactly(1L, 3L)
        assertThat(habList.map { it.habitTargetPeriod }).containsExactly(3L, 3L)
        assertThat(habList.map { it.habitDoneWellCount }).containsExactly( 4L, 0L)
        assertThat(habList.map { it.habitDoneNotWellCount }).containsExactly( 0L, 0L)
        assertThat(habList.map { it.habitTotalCount }).containsExactly(4L, 0L)
        assertThat(habList.map { it.habitTargetCompleted }).containsExactly( true, false)
        assertThat(habList.map { it.habitMarkCount }).containsExactly(7L, 0L)
        goal = goalRepo.getOneById(ownerGoalId)
        assertThat(goal!!.goalProgress).isEqualTo(0.5)

        mainRepo.markHabitDoneNotWell(habId2, Instant.now())
        mainRepo.markHabitDoneNotWell(habId2, Instant.now())
        mainRepo.markHabitDoneNotWell(habId2, Instant.now())
        mainRepo.markHabitDoneNotWell(habId2, Instant.now())

        habList = mainRepo.getAllByGoalId(ownerGoalId)
        assertThat(habList.map { it.habitTargetCount }).containsExactly(1L, 3L)
        assertThat(habList.map { it.habitTargetPeriod }).containsExactly(3L, 3L)
        assertThat(habList.map { it.habitDoneWellCount }).containsExactly( 4L, 0L)
        assertThat(habList.map { it.habitDoneNotWellCount }).containsExactly( 0L, 1L)
        assertThat(habList.map { it.habitTotalCount }).containsExactly(4L, 1L)
        assertThat(habList.map { it.habitTargetCompleted }).containsExactly( true, false)
        assertThat(habList.map { it.habitMarkCount }).containsExactly(7L, 4L)
        goal = goalRepo.getOneById(ownerGoalId)
        assertThat(goal!!.goalProgress).isEqualTo(0.5)

        mainRepo.skipHabit(habId2, Instant.now())
        mainRepo.skipHabit(habId2, Instant.now())
        mainRepo.skipHabit(habId2, Instant.now())

        habList = mainRepo.getAllByGoalId(ownerGoalId)
        assertThat(habList.map { it.habitTargetCount }).containsExactly(1L, 3L)
        assertThat(habList.map { it.habitTargetPeriod }).containsExactly(3L, 3L)
        assertThat(habList.map { it.habitDoneWellCount }).containsExactly( 4L, 0L)
        assertThat(habList.map { it.habitDoneNotWellCount }).containsExactly( 0L, 0L)
        assertThat(habList.map { it.habitTotalCount }).containsExactly(4L, 1L)
        assertThat(habList.map { it.habitTargetCompleted }).containsExactly( true, false)
        assertThat(habList.map { it.habitMarkCount }).containsExactly(7L, 7L)
        goal = goalRepo.getOneById(ownerGoalId)
        assertThat(goal!!.goalProgress).isEqualTo(0.5)

        mainRepo.markHabitDoneWell(habId2, Instant.now())
        mainRepo.markHabitDoneWell(habId2, Instant.now())
        mainRepo.markHabitDoneWell(habId2, Instant.now())
        mainRepo.markHabitDoneWell(habId2, Instant.now())
        mainRepo.markHabitDoneWell(habId2, Instant.now())
        mainRepo.markHabitDoneWell(habId2, Instant.now())

        habList = mainRepo.getAllByGoalId(ownerGoalId)
        assertThat(habList.map { it.habitTargetCount }).containsExactly(1L, 3L)
        assertThat(habList.map { it.habitTargetPeriod }).containsExactly(3L, 3L)
        assertThat(habList.map { it.habitDoneWellCount }).containsExactly( 4L, 1L)
        assertThat(habList.map { it.habitDoneNotWellCount }).containsExactly( 0L, 0L)
        assertThat(habList.map { it.habitTotalCount }).containsExactly(4L, 1L)
        assertThat(habList.map { it.habitTargetCompleted }).containsExactly( true, true)
        assertThat(habList.map { it.habitMarkCount }).containsExactly(7L, 13L)
        goal = goalRepo.getOneById(ownerGoalId)
        assertThat(goal!!.goalProgress).isEqualTo(1.0)

        mainRepo.deleteById(habId1)
        mainRepo.deleteById(habId2)

        goalRepo.deleteById(ownerGoalId)
    } }

}