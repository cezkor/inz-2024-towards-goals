package com.example.towardsgoalsapp.stats.models

import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.database.HabitStatsData
import com.example.towardsgoalsapp.etc.Translation
import com.github.mikephil.charting.data.Entry
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant

class HabitGeneralStatsTextAndExtraData(
    val periodBeginning: Instant,
    val periodEnd: Instant,
    val skippedCount: Int,
    val markedWellCount: Int,
    val markedNotWellCount: Int
): StatsOfData {
    override fun getString(translation: Translation): String {
        var total = skippedCount + markedWellCount + markedNotWellCount
        if (total == 0) total = 1
        val percentageOfWell = BigDecimal.valueOf(
            100*markedWellCount / total.toDouble()
        ).setScale(2, RoundingMode.HALF_EVEN)
        val percentageOfSkipped = BigDecimal.valueOf(
            100*skippedCount / total.toDouble()
        ).setScale(2, RoundingMode.HALF_EVEN)

        return translation.getString(R.string.habits_general_stats_text).format(
            total.toString(),
            skippedCount.toString(),
            markedWellCount.toString(),
            markedNotWellCount.toString(),
            percentageOfWell.toString(),
            percentageOfSkipped.toString()
        )
    }

}

class HabitGeneralStatsModelLogic(
    translation: Translation
) : ModelLogic<Pair<ArrayList<Entry>, ArrayList<Entry>>>(translation) {
                   // doneWell, doneNotWell

    private var trueData: ArrayList<HabitStatsData>? = null
    private var remappedData: ArrayList<Pair<Pair<Boolean, Boolean>, Instant>> = ArrayList()
    companion object {
        const val PERIOD = 0

        private const val ONE = 1f
        private const val ONE_DAY = 24*60*60L
    }

    init {
        // one week
        modelSettings[PERIOD] = ShowDataFrom(translation).apply { setChoice(1) }
    }


    override suspend fun setData(dataArrayOfOtherKind: Any) = calculateMutex.withLock {
        if (dataArrayOfOtherKind is ArrayList<*>) {
            if (dataArrayOfOtherKind.firstOrNull() is HabitStatsData?) {
                trueData = dataArrayOfOtherKind as ArrayList<HabitStatsData>
                remappedData.clear()
                remappedData.addAll(
                    trueData!!.map {
                        Pair(
                            Pair(it.doneWell, it.doneNotWell),
                            it.addedOn
                        )
                    }
                )
            }
        }
    }

    private fun getPeriod() : Duration? {
        val setting = modelSettings[PERIOD] as ShowDataFrom
        return setting.value as Duration?
    }

    private fun getLowestTimeInData() : Duration {
        val lowest = if (remappedData.isEmpty()) ONE_DAY
                     else remappedData.minBy { p -> p.second }.second.epochSecond
        return Duration.ofSeconds(Instant.now().epochSecond - lowest)
    }

    override suspend fun calculateModelData()
    : Pair<ArrayList<Pair<ArrayList<Entry>, ArrayList<Entry>>>, StatsOfData> =
    calculateMutex.withLock {
        val showDataFrom = getPeriod() ?: getLowestTimeInData()
        val end = Instant.now()
        val beg = end.minus(showDataFrom)

        val dataMatchingThisPeriod = remappedData.filter { ppp ->
            beg <= ppp.second && ppp.second <= end
        }

        val markedWell = dataMatchingThisPeriod.filter {
            ( it.first.first && ! it.first.second )
        }
        val markedNotWell = dataMatchingThisPeriod.filter {
            ( ! it.first.first && it.first.second )
        }
        val skipped = dataMatchingThisPeriod.filter {
            ( ! it.first.first && ! it.first.second )
        }

        val sod = HabitGeneralStatsTextAndExtraData (
            beg,
            end,
            skipped.size,
            markedWell.size,
            markedNotWell.size
        )

        val begToSeconds = beg.epochSecond
        // this array will contain only one element
        val ar : ArrayList<Pair<ArrayList<Entry>, ArrayList<Entry>>> = arrayListOf()
        // values are shifted by lowest period (beginning) so to keep values of x small
        ar.add(
            Pair(
                markedWell.map { p -> Entry(
                    (p.second.epochSecond - begToSeconds).toFloat(),
                    ONE
                ) }.toCollection(ArrayList()),
                markedNotWell.map { p -> Entry(
                    (p.second.epochSecond - begToSeconds).toFloat(),
                    ONE
                ) }.toCollection(ArrayList())
            )
        )
        return Pair(ar, sod)
    }
}