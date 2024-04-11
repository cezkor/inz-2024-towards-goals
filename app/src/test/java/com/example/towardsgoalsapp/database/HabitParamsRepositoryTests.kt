package com.example.towardsgoalsapp.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.towardsgoalsapp.OwnerType
import com.example.towardsgoalsapp.database.repositories.GoalRepository
import com.example.towardsgoalsapp.database.repositories.HabitParamsRepository
import com.example.towardsgoalsapp.database.repositories.HabitRepository
import com.google.common.truth.Truth.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class HabitParamsRepositoryTests {

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
    fun `basic database interaction`() { runBlocking {

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

        assertThat(hParRepo.getParamCountOf(habId1)).isEqualTo(0)
        assertThat(hParRepo.getParamCountOf(habId2)).isEqualTo(0)

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

        var params = hParRepo.getAllByOwnerId(habId1)

        assertThat(hParRepo.getParamCountOf(habId2)).isEqualTo(0)
        assertThat(params.map { it.paramId }).containsExactly(
            parId1, parId2
        )
        assertThat(params.map { it.unit }).containsExactly(
            null, "u"
        )
        assertThat(params.map { it.targetVal }).containsExactly(
            0.1, 0.0
        )
        assertThat(params.map { it.name }).containsExactly(
            "par 1", "par 2"
        )
        assertThat(hParRepo.getAllValuesOfParam(parId1)).isEmpty()
        assertThat(hParRepo.getAllValuesOfParam(parId2)).isEmpty()
        assertThat(hParRepo.getParamValueCountOf(parId1)).isEqualTo(0)
        assertThat(hParRepo.getParamValueCountOf(parId2)).isEqualTo(0)

        val updated = HabitParameter (
            parId1,
            false,
            habId1,
            "par 1 edited",
            0.5,
            "s"
        )

        hParRepo.updateHabitParamsBasedOn(arrayListOf(MutableLiveData(updated)), false)

        params = hParRepo.getAllByOwnerId(habId1)
        assertThat(hParRepo.getParamCountOf(habId2)).isEqualTo(0)
        assertThat(params.map { it.paramId }).containsExactly(
            parId1, parId2
        )
        assertThat(params.map { it.unit }).containsExactly(
            "s" , "u"
        )
        assertThat(params.map { it.targetVal }).containsExactly(
            0.5, 0.0
        )
        assertThat(params.map { it.name }).containsExactly(
            "par 1 edited", "par 2"
        )
        assertThat(hParRepo.getAllValuesOfParam(parId1)).isEmpty()
        assertThat(hParRepo.getAllValuesOfParam(parId2)).isEmpty()
        assertThat(hParRepo.getParamValueCountOf(parId1)).isEqualTo(0)
        assertThat(hParRepo.getParamValueCountOf(parId2)).isEqualTo(0)

        val now = Instant.now()
        hParRepo.putValueOfParam(
            parId1,
            1.0,
            now
        )
        hParRepo.putValueOfParam(
            parId1,
            3.0,
            now.plus(1, ChronoUnit.DAYS)
        )
        hRepo.markHabitDoneWell(habId1, Instant.now())
        hParRepo.putValueOfParam(
            parId1,
            -3.0,
            now.plus(2, ChronoUnit.DAYS)
        )

        val values = hParRepo.getAllValuesOfParam(parId1)
        assertThat(values.size).isEqualTo(3)
        assertThat(hParRepo.getAllValuesOfParam(parId2)).isEmpty()
        assertThat(hParRepo.getParamValueCountOf(parId1)).isEqualTo(3)
        assertThat(hParRepo.getParamValueCountOf(parId2)).isEqualTo(0)

        assertThat(values.map { it.paramId  }).containsExactly(parId1, parId1, parId1)
        assertThat(values.map { it.value_  }).containsExactly(1.0, 3.0, -3.0)
        assertThat(values.map { it.addedOn  })
            .containsExactly(
                now,
                now.plus(1, ChronoUnit.DAYS),
                now.plus(2, ChronoUnit.DAYS)
            )
        val habitDayC = hRepo.getOneById(habId1)!!.habitMarkCount
        // habit should be marked done well, not well or skipped by app
        // it will increase habit day count
        // THEN values of parameters should be added
        // in this case we have added three values for the same parameter
        assertThat(values.map { it.habitDayNumber })
            .containsExactly(habitDayC-1, habitDayC-1, habitDayC)

        gRepo.deleteById(goalId)

        assertThat(gRepo.getAllGoals()).isEmpty()
        assertThat(hRepo.getAllByGoalId(goalId)).isEmpty()

        assertThat(hParRepo.getParamCountOf(habId1)).isEqualTo(0)
        assertThat(hParRepo.getParamCountOf(habId2)).isEqualTo(0)
        assertThat(hParRepo.getAllValuesOfParam(parId1)).isEmpty()
        assertThat(hParRepo.getAllValuesOfParam(parId2)).isEmpty()
        assertThat(hParRepo.getParamValueCountOf(parId1)).isEqualTo(0)
        assertThat(hParRepo.getParamValueCountOf(parId2)).isEqualTo(0)

    } }

    @Test
    fun `unfinished data handled properly`() { runBlocking {
        val goalId = gRepo.addOneGoal(
            "goal1",
            "",
            2
        )

        val habId1 = hRepo.addHabit(
            "habit 1",
            "",
            1,
            1,
            goalId
        )
        
        var hpList: ArrayList<HabitParameter>
                = hParRepo.getAllByOwnerId(habId1)
        assertThat(hpList.size).isEqualTo(0)

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
        hpList = hParRepo.getAllByOwnerId(habId1)
        assertThat(hpList.map { it.paramId }).containsExactly(
            parId1, parId2
        )
        assertThat(hpList.map {it.hParEditUnfinished}).containsExactly( false, false)


        val edited = HabitParameter(
            parId1,
            false, // should be ignored
            habId1,
            "par 1 edited",
            5.0,
            null
        )
        val ar1: ArrayList<MutableLiveData<HabitParameter>> = arrayListOf(MutableLiveData(edited))
        hParRepo.updateHabitParamsBasedOn(ar1, true)
        val hopefullyUnfinishedHabitParameter = hParRepo.getOneById(parId1)
        assertThat(hopefullyUnfinishedHabitParameter).isNotNull()
        assertThat(hopefullyUnfinishedHabitParameter!!).isInstanceOf(HabitParameter::class.java)
        assertThat(hopefullyUnfinishedHabitParameter.hParEditUnfinished).isEqualTo(true)
        assertThat(hopefullyUnfinishedHabitParameter.name).isEqualTo("par 1 edited")
        assertThat(hopefullyUnfinishedHabitParameter.unit).isNull()
        assertThat(hopefullyUnfinishedHabitParameter.targetVal).isEqualTo(5.0)
        assertThat(hopefullyUnfinishedHabitParameter.paramId).isNotEqualTo(parId2)
        assertThat(hopefullyUnfinishedHabitParameter.paramId).isEqualTo(parId1)

        ar1[0].value = HabitParameter(
            parId1,
            true, // should be ignored
            habId1,
            "new par 1",
            7.0,
            null
        )

        hParRepo.updateHabitParamsBasedOn(ar1, false)

        assertThat(
            db.habitParametersQueries.selectGivenUnfinishedHabitParam(parId1).executeAsOneOrNull()
        ).isNull()

        val hopefullyFinishedHabitParameter = hParRepo.getOneById(parId1)
        assertThat(hopefullyFinishedHabitParameter).isNotNull()
        assertThat(hopefullyFinishedHabitParameter!!).isInstanceOf(HabitParameter::class.java)
        assertThat(hopefullyFinishedHabitParameter.hParEditUnfinished).isEqualTo(false)
        assertThat(hopefullyFinishedHabitParameter.name).isEqualTo("new par 1")
        assertThat(hopefullyFinishedHabitParameter.unit).isNull()
        assertThat(hopefullyFinishedHabitParameter.targetVal).isEqualTo(7.0)
        assertThat(hopefullyFinishedHabitParameter.paramId).isNotEqualTo(parId2)
        assertThat(hopefullyFinishedHabitParameter.paramId).isEqualTo(parId1)

        hParRepo.deleteById(parId2)
        hParRepo.deleteById(parId1)
        assertThat(
            db.habitParametersQueries.selectGivenUnfinishedHabitParam(parId1).executeAsOneOrNull()
        ).isNull()
        assertThat(
            db.habitParametersQueries.selectGivenUnfinishedHabitParam(parId2).executeAsOneOrNull()
        ).isNull()
        gRepo.deleteById(goalId)
        assertThat(gRepo.getAllGoals()).isEmpty()
        assertThat(hRepo.getAllByGoalId(goalId)).isEmpty()
        assertThat(hRepo.getOneById(habId1)).isNull()
    } }

}