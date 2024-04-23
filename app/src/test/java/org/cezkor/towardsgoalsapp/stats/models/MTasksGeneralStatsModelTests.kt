package org.cezkor.towardsgoalsapp.stats.models

import org.junit.Test
import com.google.common.truth.Truth.*
import kotlinx.coroutines.runBlocking
import org.cezkor.towardsgoalsapp.database.HabitStatsData
import org.cezkor.towardsgoalsapp.database.MarkableTaskStatsData
import org.cezkor.towardsgoalsapp.etc.Translation
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class MTasksGeneralStatsModelTests {

    private val translation = object : Translation() {
        override fun getString(resId: Int): String = "not important"
    }

    private fun createTSD(addedOn: Instant, fail: Boolean, p: Long) = MarkableTaskStatsData(
        -1,
        -1,
        -1,
        addedOn,
        fail,
        p
    )
    @Test
    fun `model sample size correctly calculated`() {
        val data = ArrayList<MarkableTaskStatsData>(1024)
        val now = Instant.now()
        val aDay = now.minus(1024, ChronoUnit.DAYS)
        data.addAll(Array(1024) {
            createTSD(aDay.plus(it+1L, ChronoUnit.DAYS), false, 1)
        } )

        val smallData = arrayListOf(
            createTSD(now.minus(1L, ChronoUnit.DAYS), true, 1),
            createTSD(now.plus(3L, ChronoUnit.DAYS), false, 1)
        )

        runBlocking {
            val model = MarkableTasksGeneralStatsModelLogic(translation)

            assertThat(model.calculateModelData().first).isEmpty()
            val sSizeSet = model.modelSettings[MarkableTasksGeneralStatsModelLogic.PERIOD]!!
                    as ShowDataFrom
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
                val extraData = model.getExtraData()
                assertThat(modelData.size).isEqualTo(0)
                assertThat(extraData.size).isEqualTo(1)

            }

        }
    }

    @Test
    fun `proper separation`() {

        val now = Instant.now()
        val aDay = now.minus(7, ChronoUnit.DAYS)
        val arr = arrayListOf(
            createTSD(aDay.plus(1, ChronoUnit.DAYS), true, 1),
            createTSD(aDay.plus(2, ChronoUnit.DAYS), true, 2),
            createTSD(aDay.plus(3, ChronoUnit.DAYS), false, 3),
            createTSD(aDay.plus(4, ChronoUnit.DAYS), false, 2)
        )

        val model = MarkableTasksGeneralStatsModelLogic(translation)
        val sSizeSet = model.modelSettings[MarkableTasksGeneralStatsModelLogic.PERIOD]!!
                as ShowDataFrom
        sSizeSet.setChoice(2)
        assertThat(sSizeSet.choice).isEqualTo(Duration.ofDays(14))
        runBlocking {
            model.setData(arr)
            val calc = model.calculateModelData()
            val extra = model.getExtraData()
            val data = calc.first

            assertThat(data.size).isEqualTo(2) // well done
            assertThat(data.map { e -> e.y }).containsExactly(4.0f, 3.0f)
            assertThat(extra.size).isEqualTo(2) // not well done
            assertThat(extra.map { e -> e.y }).containsExactly(2.0f, 3.0f)
        }

    }



}