package com.example.towardsgoalsapp.etc

import com.example.towardsgoalsapp.Constants

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
        val upTo = upTo(textTrimmed, Constants.NAME_LENGTH)
        return textTrimmed?.substring(0, upTo)
            ?: Constants.EMPTY_STRING
    }

}

val VeryShortNameFixer = object : TextFixer() {

    override fun fix(textToFix: String?) : String {
        val textTrimmed = textToFix?.trim()?.replace("\n","")
        val upTo = upTo(textTrimmed, Constants.VERY_SHORT_NAME_LENGTH)
        return textTrimmed?.substring(0, upTo)
            ?: Constants.EMPTY_STRING
    }

}

val ParamUnitFixer = object : TextFixer() {

    override fun fix(textToFix: String?) : String {
        val textTrimmed = textToFix?.trim()
        val upTo = upTo(textTrimmed, Constants.UNIT_NAME_LENGTH)
        return textTrimmed?.substring(0, upTo)
            ?: Constants.EMPTY_STRING
    }

}

val DescriptionFixer = object : TextFixer() {

    override fun fix(textToFix: String?) : String {
        val textTrimmed = textToFix?.trim()
        val upTo = upTo(textTrimmed, Constants.DESCRIPTION_LENGTH)
        return textTrimmed?.substring(0, upTo)
            ?: Constants.EMPTY_STRING
    }

}

val EisenhowerTaskNameFixer = object : TextFixer() {

    override fun fix(textToFix: String?) : String {
        val textTrimmed = textToFix?.trim()?.replace("\n","")
        val upTo = upTo(textTrimmed, Constants.EISENHOWER_MATRIX_NAME_LENGTH)
        if (textTrimmed == null) return Constants.EMPTY_STRING
        val addition = if (upTo < textTrimmed.length) "…" else ""
        return textTrimmed.substring(0, upTo) + addition
    }

}
