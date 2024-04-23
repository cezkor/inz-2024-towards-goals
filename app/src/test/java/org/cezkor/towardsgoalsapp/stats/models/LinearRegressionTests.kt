package org.cezkor.towardsgoalsapp.stats.models

import com.github.mikephil.charting.data.Entry
import org.junit.Test
import com.google.common.truth.Truth.*
import kotlinx.coroutines.runBlocking
import org.apache.commons.math3.stat.regression.SimpleRegression
import org.cezkor.towardsgoalsapp.database.HabitParameterValue
import org.cezkor.towardsgoalsapp.etc.Translation
import java.time.Instant

class LinearRegressionTests {

    companion object {
        val inst = Instant.now()
    }

    private val translation = object : Translation() {
        override fun getString(resId: Int): String = "not important"
    }

    private fun createParamVal(day: Long, v: Double) = HabitParameterValue(
        -100,
        -100,
        v,
        inst,
        day
    )

    @Test
    fun `model sample size correctly calculated`() {
        val data = ArrayList<HabitParameterValue>(1024)
        data.addAll(Array(1024) { createParamVal((it + 1).toLong(), 1.0) })

        val smallData = arrayListOf( createParamVal(1, 1.0), createParamVal(2, 2.0) )

        runBlocking {
            val model = OneVariableLinearRegressionModel(translation)

            assertThat(model.calculateModelData().first).isEmpty()
            val sSizeSet = model.modelSettings[OneVariableLinearRegressionModel.SAMPLE_SIZE]!!
                    as SampleSize
            assertThat(sSizeSet.choice).isEqualTo(20)

            var expect : Int
            var first : Int
            var last: Int

            model.setData(data)
            for (i in sSizeSet.choices.indices) {
                sSizeSet.setChoice(i)

                expect = if (i == 0) 1024 else sSizeSet.choice!!
                last = 1024
                first = last - expect + 1

                val modelData = model.calculateModelData().first
                assertThat(modelData.size).isEqualTo(expect)
                assertThat(modelData.map { e -> e.x })
                    .containsAtLeast(first.toFloat(), last.toFloat())

            }

            expect = 2
            last = 2
            first = 1

            model.setData(smallData)

            for (i in sSizeSet.choices.indices) {
                sSizeSet.setChoice(i)

                val modelData = model.calculateModelData().first
                assertThat(modelData.size).isEqualTo(expect)
                assertThat(modelData.map { e -> e.x })
                    .containsAtLeast(first.toFloat(), last.toFloat())

            }

        }
    }

    @Test
    fun `model does not deviate from library behaviour`() {

        val data = ArrayList<HabitParameterValue>()
        for (i in 1..200) {
            val random = kotlin.random.Random
            data.add(
                createParamVal(i.toLong(),
                    10 + random.nextDouble(-7.0, 7.0) + 30 * (i/200.0)
                )
            )
        }
        val dataRemapped = DataConversion.fromHabitParameterValueArrayTo2DDoubleArray(data)

        val reg = SimpleRegression()

        runBlocking {

            val model = OneVariableLinearRegressionModel(translation)
            model.setData(data)

            val pSizeSet = model.modelSettings[OneVariableLinearRegressionModel.PREDICT_SIZE]!!
                    as PredictSize
            val sSizeSet = model.modelSettings[OneVariableLinearRegressionModel.SAMPLE_SIZE]!!
                    as SampleSize
            sSizeSet.setChoice(0)
            assertThat(sSizeSet.choice).isEqualTo(SampleSize.ALL_SAMPLES)
            pSizeSet.setChoice(3)
            assertThat(pSizeSet.choice).isEqualTo(14)

            reg.addData(dataRemapped)
            reg.regress()

            val predictionsHere = ArrayList<Float>()
            for (i in 1..14) {
                val entry = Entry(0f, reg.predict(200.0 + i).toFloat())
                predictionsHere.add(entry.y)
            }

            val modelData = model.calculateModelData().first
            assertThat(modelData.size).isEqualTo(200)
            val predictionsFromModel = model.calculatePredictionData().first
            // first two points are for sketching the line
            val predictionsFromModelWithoutFirstTwo = predictionsFromModel.subList(2,
                predictionsFromModel.size)

            assertThat(predictionsFromModelWithoutFirstTwo.map { e -> e.y })
                .containsExactly(*predictionsHere.toArray())

        }


    }



}