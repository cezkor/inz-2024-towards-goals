package com.example.towardsgoalsapp.stats

import android.os.LocaleList
import com.example.towardsgoalsapp.Constants
import com.github.mikephil.charting.formatter.ValueFormatter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale

class EpochFormatter : ValueFormatter() {

    // because float type has low precision, ideally values from chart
    // should be small numbers
    // if one wants them to represent epoch seconds
    // it may be necessary to shift them by lowest time
    // and for the value formatting purposes, shift them back
    var pushForwardBySeconds: Long = 0L

    // format - date
    private val formatterBuilder = DateTimeFormatterBuilder()
        .appendLocalized(FormatStyle.SHORT, null)
        .appendLiteral('\n')
        .appendLocalized(null, FormatStyle.SHORT)


    // this method assumes value is seconds provided by Instant's .epochSecond()
    override fun getFormattedValue(value: Float): String {
        val locale = Locale(LocaleList.getDefault().get(0).language)
        val formatter = formatterBuilder.toFormatter(locale)
        val valueToLong = value.toLong() + pushForwardBySeconds
        val localDateTime = LocalDateTime.ofEpochSecond(valueToLong, 0, ZoneOffset.UTC)
        return formatter.format(localDateTime)
    }

}