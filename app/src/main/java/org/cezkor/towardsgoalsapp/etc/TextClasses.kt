package org.cezkor.towardsgoalsapp.etc

import org.cezkor.towardsgoalsapp.R

abstract class TextFixer {
    abstract fun fix(textToFix: String?) : String

    protected fun upTo(text: String?, maxLength: Int): Int {
        if (text.isNullOrEmpty()) return 0
        return if (text.length > maxLength) maxLength
        else text.length
    }

}

val NameFixer = object : TextFixer() {

    override fun fix(textToFix: String?) : String {
        val textTrimmed = textToFix?.trim()?.replace("\n","")
        val upTo = upTo(textTrimmed, org.cezkor.towardsgoalsapp.Constants.NAME_LENGTH)
        return textTrimmed?.substring(0, upTo)?.trim()
            ?: org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING
    }

}

val VeryShortNameFixer = object : TextFixer() {

    override fun fix(textToFix: String?) : String {
        val textTrimmed = textToFix?.trim()?.replace("\n","")
        val upTo = upTo(textTrimmed, org.cezkor.towardsgoalsapp.Constants.VERY_SHORT_NAME_LENGTH)
        return textTrimmed?.substring(0, upTo)?.trim()
            ?: org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING
    }

}

val ParamUnitFixer = object : TextFixer() {

    override fun fix(textToFix: String?) : String {
        val textTrimmed = textToFix?.trim()?.replace("\n","")
        val upTo = upTo(textTrimmed, org.cezkor.towardsgoalsapp.Constants.UNIT_NAME_LENGTH)
        return textTrimmed?.substring(0, upTo)?.trim()
            ?: org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING
    }

}

val ShortenedDescriptionFixer = object : TextFixer() {

    override fun fix(textToFix: String?) : String {
        val textTrimmed = textToFix?.trim()?.replace("\n"," ")
        val upTo = upTo(textTrimmed, org.cezkor.towardsgoalsapp.Constants.SHORTENED_DESCRIPTION_LENGTH)
        if (textTrimmed == null) return org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING
        val addition = if (upTo < textTrimmed.length) "…" else ""
        return textTrimmed.substring(0, upTo) + addition
    }

}

val DescriptionFixer = object : TextFixer() {

    override fun fix(textToFix: String?) : String {
        val textTrimmed = textToFix?.trim()
        val upTo = upTo(textTrimmed, org.cezkor.towardsgoalsapp.Constants.DESCRIPTION_LENGTH)
        return textTrimmed?.substring(0, upTo)?.trim()
            ?: org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING
    }

}

val EisenhowerTaskNameFixer = object : TextFixer() {

    override fun fix(textToFix: String?) : String {
        val textTrimmed = textToFix?.trim()?.replace("\n","")
        val upTo = upTo(textTrimmed, org.cezkor.towardsgoalsapp.Constants.EISENHOWER_MATRIX_NAME_LENGTH)
        if (textTrimmed == null) return org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING
        val addition = if (upTo < textTrimmed.length) "…" else ""
        return textTrimmed.substring(0, upTo) + addition
    }

}

val ShortenedNameFixer = object : TextFixer() {

    override fun fix(textToFix: String?) : String {
        val textTrimmed = textToFix?.trim()?.replace("\n","")
        val upTo = upTo(textTrimmed, org.cezkor.towardsgoalsapp.Constants.SHORTENED_NAME_LENGTH)
        if (textTrimmed == null) return org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING
        val addition = if (upTo < textTrimmed.length) "…" else ""
        return textTrimmed.substring(0, upTo) + addition
    }

}

class SecondsFormatting private constructor() {
    companion object {

        // formats non-negative seconds number to MM:SS
        fun formatSeconds(translation: Translation, seconds: Long) : String {
            if (seconds < 0L) return "00:00"
            val lMM = seconds / 60
            val lSS = seconds % 60
            return translation.getString(
                R.string.tasks_time_format).format(
                String.format("%02d", lMM),
                String.format("%02d", lSS)
            )
        }
    }
}