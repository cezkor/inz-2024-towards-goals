package org.cezkor.towardsgoalsapp.stats

import android.os.LocaleList
import com.github.mikephil.charting.formatter.ValueFormatter
import org.cezkor.towardsgoalsapp.Constants
import java.text.DecimalFormat
import java.util.Locale

class UnitFormatter(private val language: String) : ValueFormatter() {

    var unit: String = Constants.EMPTY_STRING
    override fun getFormattedValue(value: Float): String {
        val locale = Locale(language)
        val formatter = DecimalFormat.getNumberInstance(locale)
        formatter.maximumFractionDigits = 2
        return formatter.format(value) +
                if (unit.isNotEmpty()) (" $unit") else Constants.EMPTY_STRING
    }

}