package com.example.towardsgoalsapp.stats.models

import com.example.towardsgoalsapp.database.HabitParameterValue
import com.github.signaflo.timeseries.TimePeriod
import com.github.signaflo.timeseries.TimeSeries

class DataAdapter {

    companion object {

        fun fromHabitParameterValueArrayTo2DDoubleArray(array: ArrayList<HabitParameterValue>)
        : Array<DoubleArray>{
            val toRemove : MutableSet<HabitParameterValue> = mutableSetOf()
            // remove observations with the same day
            var orderedArray: List<HabitParameterValue> =
                array.sortedBy { hpv -> hpv.habitDayNumber }
            // remove observations with the same day, leaving only one such observation
            for (i in orderedArray.indices) {
                val nextIdx = i + 1
                if (nextIdx == orderedArray.size) break
                val current = orderedArray[i]
                val next = orderedArray[nextIdx]
                if (current.habitDayNumber == next.habitDayNumber)
                    toRemove.add(current) // in this case, the next observation matters
            }
            orderedArray = orderedArray - toRemove
            val ar: Array<DoubleArray> = Array(orderedArray.size) { idx ->
                val hp = orderedArray[idx]
                doubleArrayOf( hp.habitDayNumber.toDouble(), hp.value_ )
            }
            return ar
        }

        fun fromHabitParameterValueArrayToOrderedSignalfloTimeSeries(
            array: ArrayList<HabitParameterValue>, fillMissingWithAverage : Boolean = false) :
            TimeSeries{

            val period = TimePeriod.oneDay()
            var orderedMappedArray: List<Pair<Long, Double>>
                = array.sortedBy { t -> t.habitDayNumber }
                    .map { hpv -> Pair(hpv.habitDayNumber, hpv.value_) }
            val toRemove : MutableSet<Pair<Long,Double>> = mutableSetOf()
            val toAdd: MutableSet<Pair<Long,Double>> = mutableSetOf()
            // remove observations with the same day, leaving only one such observation
            for (i in orderedMappedArray.indices) {
                val nextIdx = i + 1
                if (nextIdx == orderedMappedArray.size) break
                val current = orderedMappedArray[i]
                val next = orderedMappedArray[nextIdx]
                if (current.first == next.first)
                    toRemove.add(current) // in this case, the next observation matters
            }
            orderedMappedArray = orderedMappedArray - toRemove
            orderedMappedArray = orderedMappedArray + toAdd
            if (fillMissingWithAverage) {
                toAdd.clear()
                val size = if (orderedMappedArray.isNotEmpty()) orderedMappedArray.size else 0
                val average: Double = orderedMappedArray.sumOf { p -> p.second } / size
                for (i in orderedMappedArray.indices) {
                    val nextIdx = i + 1
                    if (nextIdx == orderedMappedArray.size) break
                    val current = orderedMappedArray[i]
                    val next = orderedMappedArray[nextIdx]
                    // if there is a gap between observed parameter values of more than one day
                    if (current.first - next.first > 1) {
                        // fill it with the average value in the days in between
                        val differ = next.first - current.first - 1
                        for (x in 1..differ)
                            toAdd.add(Pair(current.first, average))
                    }
                }
                orderedMappedArray + orderedMappedArray + toAdd
                orderedMappedArray = orderedMappedArray.sortedBy { p -> p.first }
            }
            val dArray = orderedMappedArray.map { p -> p.second }.toDoubleArray()
            return TimeSeries.from(period, *dArray)
        }

    }

}