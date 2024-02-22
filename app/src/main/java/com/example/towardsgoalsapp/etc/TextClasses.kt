package com.example.towardsgoalsapp.etc

import com.example.towardsgoalsapp.Constants

abstract class TextFixer {
    abstract fun fix(textToFix: String?) : String

    fun upTo(text: String?, maxLength: Int): Int {
        if (text.isNullOrEmpty()) return 0
        return if (text.length > maxLength) maxLength
        else text.length
    }

}

val NameFixer = object : TextFixer() {

    override fun fix(textToFix: String?) : String {
        val textTrimmed = textToFix?.trim()
        val upTo = upTo(textTrimmed, Constants.NAME_LENGTH)
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

