package org.cezkor.towardsgoalsapp.stats.models

import org.cezkor.towardsgoalsapp.stats.questions.Question
import org.junit.Test
import com.google.common.truth.Truth.*
import org.cezkor.towardsgoalsapp.database.HabitParameter
import org.cezkor.towardsgoalsapp.database.HabitParameterValue
import org.cezkor.towardsgoalsapp.stats.questions.RangedDoubleQuestion
import java.time.Instant

class DataConversionTests {

    private fun createParamVal(day: Long, v: Double) = HabitParameterValue(
        -100,
        -100,
        v,
        Instant.now(),
        day
    )

    val ar1 = arrayListOf(
        createParamVal(1, 0.0),
        createParamVal(2, 1.0),
        createParamVal(3, 0.0),
        createParamVal(4, 1.0),
    )

    val ar2 = arrayListOf(
        createParamVal(1, 0.0),
        createParamVal(3, 5.0),
        createParamVal(2, 1.0),
        createParamVal(3, 2.0),
        createParamVal(3, 1.0),
        createParamVal(4, 1.0),
    )

    val ar3 = arrayListOf(
        createParamVal(7, 1.0),
        createParamVal(4, 1.0),
        createParamVal(1, 0.0),
        createParamVal(5, 0.0),
    )

    @Test
    fun `2D Double array test`() {

        var arr = DataConversion.fromHabitParameterValueArrayTo2DDoubleArray(ar1)

        assertThat(arr.map { it[0].toLong() })
            .containsExactly(
                1L, 2L, 3L, 4L
            )
        assertThat(arr.map { it[1] })
            .containsExactly(
                1.0, 0.0, 1.0, 0.0
            )

        arr = DataConversion.fromHabitParameterValueArrayTo2DDoubleArray(
            ar1.reversed() as ArrayList<HabitParameterValue>)
        assertThat(arr.map { it[0].toLong() })
            .containsExactly(
                1L, 2L, 3L, 4L
            )
        assertThat(arr.map { it[1] })
            .containsExactly(
                1.0, 0.0, 1.0, 0.0
            )

        arr = DataConversion.fromHabitParameterValueArrayTo2DDoubleArray(ar2)
        assertThat(arr.map { it[0].toLong() })
            .containsExactly(
                1L, 2L, 3L, 4L
            )
        assertThat(arr.map { it[1] })
            .containsExactly(
                0.0, 1.0, 1.0, 1.0
            )

        arr = DataConversion.fromHabitParameterValueArrayTo2DDoubleArray(ar3)
        assertThat(arr.map { it[0].toLong() })
            .containsExactly(
                1L, 4L, 5L, 7L
            )
        assertThat(arr.map { it[1] })
            .containsExactly(
                0.0, 1.0, 0.0, 1.0
            )

    }

    @Test
    fun `singnalflo test`() {

        var ts = DataConversion.fromHabitParameterValueArrayToOrderedSignalfloTimeSeries(ar1)
        assertThat(ts.asList()).containsExactly(
            1.0, 0.0, 1.0, 0.0
        )
        ts = DataConversion.fromHabitParameterValueArrayToOrderedSignalfloTimeSeries(ar1, true)
        assertThat(ts.asList()).containsExactly(
            1.0, 0.0, 1.0, 0.0
        )
        ts = DataConversion.fromHabitParameterValueArrayToOrderedSignalfloTimeSeries(
            ar1.reversed() as ArrayList<HabitParameterValue>, true)
        assertThat(ts.asList()).containsExactly(
            1.0, 0.0, 1.0, 0.0
        )

        ts = DataConversion.fromHabitParameterValueArrayToOrderedSignalfloTimeSeries(ar2)
        assertThat(ts.asList()).containsExactly(
            0.0, 1.0, 1.0, 1.0
        )

        ts = DataConversion.fromHabitParameterValueArrayToOrderedSignalfloTimeSeries(ar3)
        assertThat(ts.asList()).containsExactly(
            0.0, 1.0, 0.0, 1.0
        )

        ts = DataConversion.fromHabitParameterValueArrayToOrderedSignalfloTimeSeries(ar3, true)
        assertThat(ts.asList()).containsExactly(
            0.0, 0.5, 0.5, 1.0, 0.0, 0.5, 1.0
        )
    }

}