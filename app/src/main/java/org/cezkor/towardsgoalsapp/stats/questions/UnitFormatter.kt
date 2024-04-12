package org.cezkor.towardsgoalsapp.stats.questions

import android.os.LocaleList
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat
import java.util.Locale

class UnitFormatter : ValueFormatter() {

    var unit: String = org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING
    override fun getFormattedValue(value: Float): String {
        val locale = Locale(LocaleList.getDefault().get(0).language)
        val formatter = DecimalFormat.getNumberInstance(locale)
        formatter.maximumFractionDigits = 2
        return formatter.format(value) + " " + unit
    }

}