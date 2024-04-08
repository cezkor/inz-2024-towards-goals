package com.example.towardsgoalsapp.stats.questions

import android.os.LocaleList
import com.example.towardsgoalsapp.Constants
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale

class UnitFormatter : ValueFormatter() {

    var unit: String = Constants.EMPTY_STRING
    override fun getFormattedValue(value: Float): String {
        val locale = Locale(LocaleList.getDefault().get(0).language)
        val formatter = DecimalFormat.getNumberInstance(locale)
        formatter.maximumFractionDigits = 2
        return formatter.format(value) + " " + unit
    }

}