package com.example.towardsgoalsapp.stats.models

import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.etc.Translation
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import java.math.BigDecimal
import java.math.RoundingMode

interface StatsOfData {
    fun getString(translation: Translation) : String

}
class GeneralStats(

    val mean: Double,
    val standardDeviation: Double,
    val median: Double,
    val max: Double,
    val min: Double

) : StatsOfData {

    companion object {

        @JvmStatic
        fun fromDoubleData(data: DoubleArray) : GeneralStats {
            val dStats = DescriptiveStatistics()
            for (d in data) dStats.addValue(d)
            return GeneralStats(
                dStats.mean,
                dStats.standardDeviation,
                dStats.getPercentile(50.0),
                dStats.max,
                dStats.min
            )
        }

    }

    override fun getString(translation: Translation) : String {
        return translation.getString(R.string.stats_general_stats).format(
            BigDecimal(mean).setScale(2, RoundingMode.HALF_EVEN).toString(),
            BigDecimal(standardDeviation).setScale(2, RoundingMode.HALF_EVEN).toString(),
            BigDecimal(median).setScale(2, RoundingMode.HALF_EVEN).toString(),
            BigDecimal(max).setScale(2, RoundingMode.HALF_EVEN).toString(),
            BigDecimal(min).setScale(2, RoundingMode.HALF_EVEN).toString()
        )
    }
}