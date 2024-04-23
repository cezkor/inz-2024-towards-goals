package org.cezkor.towardsgoalsapp.stats.models

import com.github.mikephil.charting.data.Entry
import com.github.signaflo.timeseries.TimeSeries
import com.github.signaflo.timeseries.model.arima.Arima
import com.github.signaflo.timeseries.model.arima.ArimaOrder
import org.junit.Test
import com.google.common.truth.Truth.*
import kotlinx.coroutines.runBlocking
import org.apache.commons.math3.stat.regression.SimpleRegression
import org.cezkor.towardsgoalsapp.database.HabitParameterValue
import org.cezkor.towardsgoalsapp.etc.Translation
import java.time.Instant

class ARIMATests {

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
            val model = OneVariableARIMAModel(translation)
            val fillSet =
                model.modelSettings[OneVariableARIMAModel.FILL_MISSING_WITH_AVERAGE]!!
                        as BooleanModelSetting

            assertThat(fillSet.value).isEqualTo(false)
            assertThat(model.calculateModelData().first).isEmpty()
            val sSizeSet = model.modelSettings[OneVariableARIMAModel.SAMPLE_SIZE]!!
                    as ARIMASampleDaySize
            assertThat(sSizeSet.choice).isEqualTo(28)

            var expect : Int
            var first : Int
            var last: Int

            model.setData(data)
            for (i in sSizeSet.choices.indices) {
                sSizeSet.setChoice(i)

                expect = sSizeSet.choice!!
                last = 1024
                first = last - expect + 1

                fillSet.setValue(false)
                var modelData = model.calculateModelData().first
                assertThat(modelData.size).isEqualTo(expect)
                assertThat(modelData.map { e -> e.x })
                    .containsAtLeast(first.toFloat(), last.toFloat())

                fillSet.setValue(true)
                modelData = model.calculateModelData().first
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

                fillSet.setValue(false)
                var modelData = model.calculateModelData().first
                assertThat(modelData.size).isEqualTo(expect)
                assertThat(modelData.map { e -> e.x })
                    .containsAtLeast(first.toFloat(), last.toFloat())

                fillSet.setValue(true)
                modelData = model.calculateModelData().first
                assertThat(modelData.size).isEqualTo(expect)
                assertThat(modelData.map { e -> e.x })
                    .containsAtLeast(first.toFloat(), last.toFloat())

            }

        }
    }

    @Test
    fun `model does not deviate from library behaviour`() {

        val data1 = ArrayList<HabitParameterValue>()
        for (i in 1..3*28) {
            val random = kotlin.random.Random
            data1.add(
                createParamVal((2*i + 1).toLong(),
                    10 + random.nextDouble(-7.0, 7.0) + 30 * ((2*i - 1)/(6.0 * 28))
                )
            )
        }

        val data2 = ArrayList<HabitParameterValue>()
        for (i in 1..6*28) {
            val random = kotlin.random.Random
            data2.add(
                createParamVal(i.toLong(),
                    10 + random.nextDouble(-7.0, 7.0) + 2 * (i*i/(6*28*6*28.0))
                )
            )
        }

        suspend fun testWithData(data: ArrayList<HabitParameterValue>) {
            val model = OneVariableARIMAModel(translation)
            var arima: Arima? = null
            val alpha = OneVariableARIMAModel.ALPHA

            val fillSet = model.modelSettings[OneVariableARIMAModel.FILL_MISSING_WITH_AVERAGE]!!
                    as BooleanModelSetting
            val iRankSet = model.modelSettings[OneVariableARIMAModel.I_RANK]!! as IntegrationRank
            val arRankSet = model.modelSettings[OneVariableARIMAModel.AR_RANK]!! as ARRank
            val maRankSet = model.modelSettings[OneVariableARIMAModel.MA_RANK]!! as MARank
            val pSet = model.modelSettings[OneVariableARIMAModel.PREDICT_SIZE]!! as PredictSize
            val sSet = model.modelSettings[OneVariableARIMAModel.SAMPLE_SIZE]!! as ARIMASampleDaySize
            val seasonSett = model.modelSettings[OneVariableARIMAModel.SEASONALITY]!! as SeasonSize
            val cSet = model.modelSettings[OneVariableARIMAModel.INCLUDE_CONSTANT_IF_POSSIBLE]!!
                    as BooleanModelSetting

            sSet.setChoice(2)
            assertThat(sSet.choice).isEqualTo(6 * 28)
            pSet.setChoice(3)
            assertThat(pSet.choice).isEqualTo(14)
            arRankSet.setChoice(1)
            assertThat(arRankSet.choice).isEqualTo(1)
            maRankSet.setChoice(1)
            assertThat(maRankSet.choice).isEqualTo(1)

            seasonSett.setChoice(0)
            assertThat(seasonSett.choice).isNull()
            cSet.setValue(false)
            assertThat(cSet.value).isEqualTo(false)
            fillSet.setValue(false)
            assertThat(fillSet.value).isEqualTo(false)

            model.setData(data)

            suspend fun check() {
                val fData = arima!!.forecast(14, alpha).pointEstimates()
                    .asList().map { d -> Entry(0f, d.toFloat()).y }
                val pData = model.calculatePredictionData().first.map { e -> e.y }
                assertThat(pData).containsExactly( *(fData.toTypedArray()) )
            }

            iRankSet.setChoice(3)
            val iRank = iRankSet.choice!!
            var dataRemapped = DataConversion.fromHabitParameterValueArrayToOrderedSignalfloTimeSeries(
                data
            )
            fillSet.setValue(false)
            cSet.setValue(false)
            seasonSett.setChoice(0)
            arima = Arima.model(
                dataRemapped,
                ArimaOrder.order(
                    1, iRank, 1, Arima.Constant.EXCLUDE
                )
            )
            check()
            fillSet.setValue(false)
            seasonSett.setChoice(0)
            arima = Arima.model(
                dataRemapped,
                ArimaOrder.order(
                    1, iRank, 1, Arima.Constant.EXCLUDE
                )
            )
            check()

            seasonSett.setChoice(1)
            fillSet.setValue(false)
            val season = seasonSett.choice!!
            arima = Arima.model(
                dataRemapped,
                ArimaOrder.order(
                    0, 0, 0, 1, iRank, 1, Arima.Constant.EXCLUDE
                ),
                season
            )
            check()

            dataRemapped = DataConversion.fromHabitParameterValueArrayToOrderedSignalfloTimeSeries(
                data, true
            )
            fillSet.setValue(true)
            seasonSett.setChoice(0)
            arima = Arima.model(
                dataRemapped,
                ArimaOrder.order(
                    1, iRank, 1, Arima.Constant.EXCLUDE
                )
            )
            check()

        }

        runBlocking {
            testWithData(data1)
            testWithData(data2)
        }


    }



}