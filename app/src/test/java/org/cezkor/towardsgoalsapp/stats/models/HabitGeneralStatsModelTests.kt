package org.cezkor.towardsgoalsapp.stats.models

import org.junit.Test
import com.google.common.truth.Truth.*
import kotlinx.coroutines.runBlocking
import org.cezkor.towardsgoalsapp.database.HabitStatsData
import org.cezkor.towardsgoalsapp.etc.Translation
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class HabitGeneralStatsModelTests {

    private val translation = object : Translation() {
        override fun getString(resId: Int): String = "not important"
    }

    private fun createHSD(addedOn: Instant, dW: Boolean, nDW: Boolean) = HabitStatsData(
        -1,
        -1,
        -1,
        addedOn,
        dW,
        nDW
    )
    @Test
    fun `model sample size correctly calculated`() {
        val data = ArrayList<HabitStatsData>(1024)
        val now = Instant.now()
        val aDay = now.minus(1024, ChronoUnit.DAYS)
        data.addAll(Array(1024) {
            createHSD(aDay.plus(it+1L, ChronoUnit.DAYS), true, false)
        } )

        val smallData = arrayListOf(
            createHSD(now.minus(1L, ChronoUnit.DAYS), true, false),
            createHSD(now.plus(3L, ChronoUnit.DAYS), false, true)
        )

        runBlocking {
            val model = HabitGeneralStatsModelLogic(translation)

            assertThat(model.calculateModelData().first).isEmpty()
            val sSizeSet = model.modelSettings[HabitGeneralStatsModelLogic.PERIOD]!! as ShowDataFrom
            assertThat(sSizeSet.choice).isEqualTo(Duration.ofDays(7))

            var expect : Long
            model.setData(data)
            for (i in sSizeSet.choices.indices) {
                sSizeSet.setChoice(i)

                expect = if (i == 0) 1024 else sSizeSet.choice!!.toDays()

                val modelData = model.calculateModelData().first
                assertThat(modelData.size).isEqualTo(expect)

            }

            model.setData(smallData)

            for (i in sSizeSet.choices.indices) {
                sSizeSet.setChoice(i)

                val modelData = model.calculateModelData().first
                assertThat(modelData.size).isEqualTo(1)

            }

        }
    }

    @Test
    fun `proper separation`() {

        val now = Instant.now()
        val aDay = now.minus(7, ChronoUnit.DAYS)
        val arr = arrayListOf(
            createHSD(aDay.plus(1, ChronoUnit.DAYS), true, false),
            createHSD(aDay.plus(2, ChronoUnit.DAYS), true, false),
            createHSD(aDay.plus(3, ChronoUnit.DAYS), false, false),
            createHSD(aDay.plus(4, ChronoUnit.DAYS), false, false),
            createHSD(aDay.plus(5, ChronoUnit.DAYS), false, true),
            createHSD(aDay.plus(6, ChronoUnit.DAYS), false, true)
        )

        val model = HabitGeneralStatsModelLogic(translation)
        val sSizeSet = model.modelSettings[HabitGeneralStatsModelLogic.PERIOD]!! as ShowDataFrom
        sSizeSet.setChoice(2)
        assertThat(sSizeSet.choice).isEqualTo(Duration.ofDays(14))
        runBlocking {
            model.setData(arr)
            val calc = model.calculateModelData()
            val extra = model.getExtraData()
            val data = calc.first

            assertThat(data.size).isEqualTo(2) // well done
            assertThat(extra.size).isEqualTo(2) // not well done
        }

    }



}