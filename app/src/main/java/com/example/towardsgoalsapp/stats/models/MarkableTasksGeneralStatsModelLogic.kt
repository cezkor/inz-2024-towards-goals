package com.example.towardsgoalsapp.stats.models

import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.database.MarkableTaskStatsData
import com.example.towardsgoalsapp.etc.Translation
import com.github.mikephil.charting.data.Entry
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant

// Markable task is a task that has no subtasks (is directly doable -> is marked by the user)

class MarkableTasksGeneralStatsTextAndExtraData(
    val periodBeginning: Instant,
    val periodEnd: Instant,
    val maxTaskPriorityPlotValue: Int,
    val doneAndNotFailedCount: Int,
    val doneAndFailedCount: Int,
): StatsOfData {
    override fun getString(translation: Translation): String {
        val total = doneAndFailedCount + doneAndNotFailedCount
        return translation.getString(R.string.tasks_general_stats_text).format(
            total.toString(),
            doneAndNotFailedCount.toString()
        )
    }

}

class MarkableTasksGeneralStatsModelLogic(
    translation: Translation
) : ModelLogic<Pair<ArrayList<Entry>, ArrayList<Entry>>>(translation) {
                   // doneWell, doneNotWell

    private var trueData: ArrayList<MarkableTaskStatsData>? = null
    private var remappedData: ArrayList<Pair<Pair<Boolean, Long>, Instant>> = ArrayList()
    companion object {
        const val PERIOD = 0

        private const val ONE_DAY = 24*60*60L
    }

    init {
        // one week
        modelSettings[PERIOD] = ShowDataFrom(translation).apply { setChoice(1) }
    }


    override suspend fun setData(dataArrayOfOtherKind: Any) = calculateMutex.withLock {
        if (dataArrayOfOtherKind is ArrayList<*>) {
            if (dataArrayOfOtherKind.firstOrNull() is MarkableTaskStatsData?) {
                trueData = dataArrayOfOtherKind as ArrayList<MarkableTaskStatsData>
                remappedData.clear()
                remappedData.addAll(
                    trueData!!.map {
                        Pair(
                            Pair(it.taskFailed, it.taskPriority),
                            it.addedOn
                        )
                    }
                )
            }
        }
    }

    private fun getPeriod(): Duration? {
        val setting = modelSettings[PERIOD] as ShowDataFrom
        return setting.value as Duration?
    }

    private fun getLowestTimeInData() : Duration {
        val lowest = if (remappedData.isEmpty()) ONE_DAY
                     else remappedData.minBy { p -> p.second }.second.epochSecond
        return Duration.ofSeconds(Instant.now().epochSecond - lowest)
    }

    private fun remapPriority(v: Long): Float {
        if (v< 0) return Constants.MIN_PRIORITY.toFloat()
        if (v>Constants.MAX_PRIORITY) return Constants.MAX_PRIORITY.toFloat()
        return v.toFloat()
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

        val markedNotFailed = dataMatchingThisPeriod.filter {
            ( ! it.first.first )
        }
        val markedFailed = dataMatchingThisPeriod.filter {
            ( it.first.first )
        }

        val sod = MarkableTasksGeneralStatsTextAndExtraData (
            beg,
            end,
            Constants.MAX_PRIORITY + 1,
            markedNotFailed.size,
            markedFailed.size
        )

        val begToSeconds = beg.epochSecond
        val remappedMarkedNotFailed = markedNotFailed.map { p -> Entry(
            // values are shifted by lowest period (beginning) so to keep values of x small
            (p.second.epochSecond - begToSeconds).toFloat(),
            remapPriority(p.first.second) + 1f
        ) }.toCollection(ArrayList())
        val remappedMarkedFailed = markedFailed.map { p -> Entry(
            (p.second.epochSecond - begToSeconds).toFloat(),
            remapPriority(p.first.second) + 1f
        ) }.toCollection(ArrayList())
        // this array will contain only one element
        val ar : ArrayList<Pair<ArrayList<Entry>, ArrayList<Entry>>> = arrayListOf()
        ar.add(
            Pair(
                remappedMarkedNotFailed,
                remappedMarkedFailed
            )
        )
        return Pair(ar, sod)
    }
}