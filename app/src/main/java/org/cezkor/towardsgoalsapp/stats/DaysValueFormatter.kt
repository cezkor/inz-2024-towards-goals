package org.cezkor.towardsgoalsapp.stats

import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.etc.Translation
import com.github.mikephil.charting.formatter.ValueFormatter

class DaysValueFormatter(private val translation: Translation) : ValueFormatter() {

    var current: Float = 0f

    override fun getFormattedValue(value: Float): String {
        val v = (current - value).toInt() - 1
        return when (v) {
            1 -> translation.getString(R.string.stats_one_day_ago)
            0 -> translation.getString(R.string.stats_today)
            -1 -> translation.getString(R.string.stats_next_day)
            else -> if (v > 0)
                        translation.getString(R.string.stats_days_ago).format(v)
                    else
                        translation.getString(R.string.stats_in_x_days).format(-1 * v)
        }

    }


}