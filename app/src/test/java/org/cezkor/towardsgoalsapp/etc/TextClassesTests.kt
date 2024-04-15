package org.cezkor.towardsgoalsapp.etc

import org.cezkor.towardsgoalsapp.R
import com.google.common.truth.Truth.assertThat
import org.cezkor.towardsgoalsapp.Constants
import org.junit.Test

class TextClassesTests {

    private fun testBehaviourOfNameFixer(tf: TextFixer, maxAllowedLength: Int) {

        assertThat(maxAllowedLength).isGreaterThan(4) // otherwise this test cannot be done

        val remainUnchanged1 = "a"
        val fixed1 = tf.fix(remainUnchanged1)
        assertThat(fixed1).isEqualTo(remainUnchanged1)

        // fixer should not remove spaces in text...
        val remainUnchanged2 = "a   b"
        val fixed2 = tf.fix(remainUnchanged2)
        assertThat(fixed2).isEqualTo(remainUnchanged2)

        // ...unless they're leading or trailing

        val removeTrailing = "a    "
        val fixed3 = tf.fix(removeTrailing)
        assertThat(fixed3).isEqualTo("a")

        val removeLeading = "    a"
        val fixed4 = tf.fix(removeLeading)
        assertThat(fixed4).isEqualTo("a")

        val removeBoth = "  a  "
        val fixed5 = tf.fix(removeBoth)
        assertThat(fixed5).isEqualTo("a")

        val removeBothLeaveMiddle = " a b "
        val fixed6 = tf.fix(removeBothLeaveMiddle)
        assertThat(fixed6).isEqualTo("a b")

        val removeEndline = "\na\n\n\n\n\n\n\nb\n\n"
        val fixed7 = tf.fix(removeEndline)
        assertThat(fixed7).isEqualTo("ab")

        assertThat(tf.fix(org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING)).isEqualTo(org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING)
        assertThat(tf.fix(null)).isEqualTo(org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING)

        val maxString = "a".repeat(maxAllowedLength)
        assertThat(tf.fix(maxString)).isEqualTo(maxString)

        val maxStringWithoutOne = maxString.substring(0, maxString.length - 1)
        val maxStringWithoutTwo = maxString.substring(0, maxString.length - 2)
        val maxStringWithoutThree = maxString.substring(0, maxString.length - 3)
        val maxStringWithoutFour = maxString.substring(0, maxString.length - 4)

        assertThat(tf.fix(maxStringWithoutOne)).isEqualTo(maxStringWithoutOne)
        assertThat(tf.fix(maxStringWithoutTwo)).isEqualTo(maxStringWithoutTwo)
        assertThat(tf.fix(maxStringWithoutThree)).isEqualTo(maxStringWithoutThree)
        assertThat(tf.fix(maxStringWithoutFour)).isEqualTo(maxStringWithoutFour)

        val maxStringPlusOne = maxString + "b"
        val maxStringPlusTwo = maxString + "cc"
        val maxStringTimesTwo = maxString + maxString
        assertThat(tf.fix(maxStringPlusOne)).isEqualTo(maxString)
        assertThat(tf.fix(maxStringPlusTwo)).isEqualTo(maxString)
        assertThat(tf.fix(maxStringTimesTwo)).isEqualTo(maxString)
        assertThat(tf.fix("   $maxString")).isEqualTo(maxString)
        assertThat(tf.fix("$maxString   ")).isEqualTo(maxString)
        assertThat(tf.fix("   $maxString   ")).isEqualTo(maxString)
        assertThat(tf.fix("   $maxStringWithoutTwo")).isEqualTo(maxStringWithoutTwo)
        assertThat(tf.fix("   $maxStringWithoutTwo   ")).isEqualTo(maxStringWithoutTwo)
        assertThat(tf.fix("$maxStringWithoutTwo   ")).isEqualTo(maxStringWithoutTwo)
        assertThat(tf.fix("   $maxStringTimesTwo")).isEqualTo(maxString)
        assertThat(tf.fix("   $maxStringTimesTwo   ")).isEqualTo(maxString)
        assertThat(tf.fix("$maxStringTimesTwo   ")).isEqualTo(maxString)
        assertThat(tf.fix(" \n\n  $maxString")).isEqualTo(maxString)
        assertThat(tf.fix("$maxString \n\n  ")).isEqualTo(maxString)
        assertThat(tf.fix("   $maxString  \n\n\n ")).isEqualTo(maxString)
        assertThat(tf.fix(" \n\n\n  $maxStringWithoutTwo")).isEqualTo(maxStringWithoutTwo)
        assertThat(tf.fix(" \n\n  $maxStringWithoutTwo  \n ")).isEqualTo(maxStringWithoutTwo)
        assertThat(tf.fix("$maxStringWithoutTwo \n  ")).isEqualTo(maxStringWithoutTwo)
        assertThat(tf.fix(" \n\n  $maxStringTimesTwo")).isEqualTo(maxString)
        assertThat(tf.fix(" \n\n  $maxStringTimesTwo \n\n\n\n\n  ")).isEqualTo(maxString)
        assertThat(tf.fix("$maxStringTimesTwo \n\n  ")).isEqualTo(maxString)
        // cut off to first maxAllowedLength characters, without leading spaces
        assertThat(tf.fix("$maxString   $maxString")).isEqualTo(maxString)
        assertThat(tf.fix("             $maxString   $maxString")).isEqualTo(maxString)
        assertThat(tf.fix("             $maxStringWithoutOne   $maxString"))
            .isEqualTo(maxStringWithoutOne)
        assertThat(tf.fix("\n\n\n\n\n\n\n\n\n\n\n\n$maxString")).isEqualTo(maxString)

    }

    private fun testBehaviourOfNameFixerWithEllipsis(tf: TextFixer,
                                                     maxAllowedLengthWithoutEllipsis: Int) {
        val maxStringWithoutEllipsis = "a".repeat(maxAllowedLengthWithoutEllipsis)
        val maxStringWithoutEllipsisWithoutOne = "a".repeat(maxAllowedLengthWithoutEllipsis - 1)

        assertThat(maxAllowedLengthWithoutEllipsis).isGreaterThan(4)

        assertThat(tf.fix(null))
            .isEqualTo(org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING)
        assertThat(tf.fix(org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING))
            .isEqualTo(org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING)

        assertThat(tf.fix("ababa")).isEqualTo("ababa")
        assertThat(tf.fix("    ababa")).isEqualTo("ababa")
        assertThat(tf.fix("ababa    ")).isEqualTo("ababa")
        assertThat(tf.fix("    ababa    ")).isEqualTo("ababa")

        assertThat(tf.fix(maxStringWithoutEllipsis)).isEqualTo(maxStringWithoutEllipsis)
        assertThat(tf.fix(maxStringWithoutEllipsisWithoutOne))
            .isEqualTo(maxStringWithoutEllipsisWithoutOne)
        assertThat(tf.fix(maxStringWithoutEllipsis + "a"))
            .isEqualTo("$maxStringWithoutEllipsis…")
        assertThat(tf.fix("$maxStringWithoutEllipsisWithoutOne   "))
            .isEqualTo(maxStringWithoutEllipsisWithoutOne)
        assertThat(tf.fix("    $maxStringWithoutEllipsisWithoutOne   "))
            .isEqualTo(maxStringWithoutEllipsisWithoutOne)
        assertThat(tf.fix("    $maxStringWithoutEllipsisWithoutOne"))
            .isEqualTo(maxStringWithoutEllipsisWithoutOne)

        // it should indicate that that name is long even if it has spaces leading
        assertThat(tf.fix("$maxStringWithoutEllipsisWithoutOne   abab"))
            .isEqualTo("$maxStringWithoutEllipsisWithoutOne …")
        assertThat(tf.fix("    $maxStringWithoutEllipsisWithoutOne   abab"))
            .isEqualTo("$maxStringWithoutEllipsisWithoutOne …")
        assertThat(tf.fix("    $maxStringWithoutEllipsisWithoutOne   abab    "))
            .isEqualTo("$maxStringWithoutEllipsisWithoutOne …")
    }

    @Test
    fun `name fixers tests`() {

        testBehaviourOfNameFixer(NameFixer, org.cezkor.towardsgoalsapp.Constants.NAME_LENGTH)
        testBehaviourOfNameFixer(ParamUnitFixer,
            org.cezkor.towardsgoalsapp.Constants.UNIT_NAME_LENGTH)
        testBehaviourOfNameFixer(VeryShortNameFixer,
            org.cezkor.towardsgoalsapp.Constants.VERY_SHORT_NAME_LENGTH)

        testBehaviourOfNameFixerWithEllipsis(EisenhowerTaskNameFixer,
            Constants.EISENHOWER_MATRIX_NAME_LENGTH)
        testBehaviourOfNameFixerWithEllipsis(
            ShortenedNameFixer,
            Constants.SHORTENED_NAME_LENGTH
        )
    }

    @Test
    fun `description fixer test`() {
        val maxLen = org.cezkor.towardsgoalsapp.Constants.DESCRIPTION_LENGTH
        val tf1 = DescriptionFixer

        assertThat(maxLen).isGreaterThan(4)

        val remainUnchanged1 = "a"
        val fixed1 = tf1.fix(remainUnchanged1)
        assertThat(fixed1).isEqualTo(remainUnchanged1)

        // fixer should not remove spaces in text...
        val remainUnchanged2 = "a   b"
        val fixed2 = tf1.fix(remainUnchanged2)
        assertThat(fixed2).isEqualTo(remainUnchanged2)

        // ...unless they're leading or trailing

        val removeTrailing = "a    "
        val fixed3 = tf1.fix(removeTrailing)
        assertThat(fixed3).isEqualTo("a")

        val removeLeading = "    a"
        val fixed4 = tf1.fix(removeLeading)
        assertThat(fixed4).isEqualTo("a")

        val removeBoth = "  a  "
        val fixed5 = tf1.fix(removeBoth)
        assertThat(fixed5).isEqualTo("a")

        val removeBothLeaveMiddle = " a b "
        val fixed6 = tf1.fix(removeBothLeaveMiddle)
        assertThat(fixed6).isEqualTo("a b")

        // description should allow endline characters (lines in description)
        // but not as trailing or leading
        val allowEndline = "\na\nb\n"
        val fixed7 = tf1.fix(allowEndline)
        assertThat(fixed7).isEqualTo("a\nb")

        val maxString = "a".repeat(maxLen)
        assertThat(tf1.fix(maxString)).isEqualTo(maxString)

        val maxStringWithoutOne = maxString.substring(0, maxString.length - 1)
        val maxStringWithoutTwo = maxString.substring(0, maxString.length - 2)
        val maxStringWithoutThree = maxString.substring(0, maxString.length - 3)
        val maxStringWithoutFour = maxString.substring(0, maxString.length - 4)

        assertThat(tf1.fix(maxStringWithoutOne)).isEqualTo(maxStringWithoutOne)
        assertThat(tf1.fix(maxStringWithoutTwo)).isEqualTo(maxStringWithoutTwo)
        assertThat(tf1.fix(maxStringWithoutThree)).isEqualTo(maxStringWithoutThree)
        assertThat(tf1.fix(maxStringWithoutFour)).isEqualTo(maxStringWithoutFour)

        val maxStringPlusOne = maxString + "b"
        val maxStringPlusTwo = maxString + "cc"
        val maxStringTimesTwo = maxString + maxString
        assertThat(tf1.fix(maxStringPlusOne)).isEqualTo(maxString)
        assertThat(tf1.fix(maxStringPlusTwo)).isEqualTo(maxString)
        assertThat(tf1.fix(maxStringTimesTwo)).isEqualTo(maxString)
        assertThat(tf1.fix("\n\n\n$maxStringTimesTwo"))
            .isEqualTo(maxString)
        assertThat(tf1.fix("\n\n\n$maxStringTimesTwo\n\n\n"))
            .isEqualTo(maxString)
        assertThat(tf1.fix("\n \n$maxStringTimesTwo"))
            .isEqualTo(maxString)
        assertThat(tf1.fix("a \n$maxStringTimesTwo"))
            .isEqualTo("a \n${maxStringWithoutThree}")
        assertThat(tf1.fix("$maxStringWithoutTwo\n$maxStringTimesTwo"))
            .isEqualTo("${maxStringWithoutTwo}\na")
        assertThat(tf1.fix("$maxStringWithoutTwo\n "))
            .isEqualTo(maxStringWithoutTwo)
        assertThat(tf1.fix("$maxStringWithoutTwo\n \n"))
            .isEqualTo(maxStringWithoutTwo)

        // see EisenhowerMatrixTaskName test
        val tf2 = ShortenedDescriptionFixer
        val maxAllowedLengthWithoutEllipsis = org.cezkor.towardsgoalsapp.Constants.SHORTENED_DESCRIPTION_LENGTH
        val maxStringWithoutEllipsis = "a".repeat(maxAllowedLengthWithoutEllipsis)
        val maxStringWithoutEllipsisWithoutOne = "a".repeat(maxAllowedLengthWithoutEllipsis - 1)

        assertThat(maxAllowedLengthWithoutEllipsis).isGreaterThan(4)

        assertThat(tf2.fix(null)).isEqualTo(org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING)
        assertThat(tf2.fix(org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING)).isEqualTo(org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING)

        assertThat(tf2.fix("ababa")).isEqualTo("ababa")
        assertThat(tf2.fix("    ababa")).isEqualTo("ababa")
        assertThat(tf2.fix("ababa    ")).isEqualTo("ababa")
        assertThat(tf2.fix("    ababa    ")).isEqualTo("ababa")

        assertThat(tf2.fix(maxStringWithoutEllipsis)).isEqualTo(maxStringWithoutEllipsis)
        assertThat(tf2.fix(maxStringWithoutEllipsisWithoutOne))
            .isEqualTo(maxStringWithoutEllipsisWithoutOne)
        assertThat(tf2.fix(maxStringWithoutEllipsis + "a"))
            .isEqualTo("$maxStringWithoutEllipsis…")
        assertThat(tf2.fix("$maxStringWithoutEllipsisWithoutOne   "))
            .isEqualTo(maxStringWithoutEllipsisWithoutOne)
        assertThat(tf2.fix("    $maxStringWithoutEllipsisWithoutOne   "))
            .isEqualTo(maxStringWithoutEllipsisWithoutOne)
        assertThat(tf2.fix("    $maxStringWithoutEllipsisWithoutOne"))
            .isEqualTo(maxStringWithoutEllipsisWithoutOne)

        // it should indicate that that name is long even if it has spaces leading
        assertThat(tf2.fix("$maxStringWithoutEllipsisWithoutOne   abab"))
            .isEqualTo("$maxStringWithoutEllipsisWithoutOne …")
        assertThat(tf2.fix("    $maxStringWithoutEllipsisWithoutOne   abab"))
            .isEqualTo("$maxStringWithoutEllipsisWithoutOne …")
        assertThat(tf2.fix("    $maxStringWithoutEllipsisWithoutOne   abab    "))
            .isEqualTo("$maxStringWithoutEllipsisWithoutOne …")
    }

    @Test
    fun `seconds formatter test`() {
        val mockTranslation = object : Translation() {
            override fun getString(resId: Int): String {
                return if (resId ==  R.string.tasks_time_format)
                    "%s:%s"
                else org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING
            }

        }

        fun format(seconds: Long) : String
            = SecondsFormatting.formatSeconds(mockTranslation, seconds)

        assertThat(format(0L)).isEqualTo("00:00")
        assertThat(format(-10000L)).isEqualTo("00:00")
        assertThat(format(10)).isEqualTo("00:10")
        assertThat(format(-10L)).isEqualTo("00:00")
        assertThat(format(59L)).isEqualTo("00:59")
        assertThat(format(60L)).isEqualTo("01:00")
        assertThat(format(69L)).isEqualTo("01:09")
        assertThat(format(30*60 + 37)).isEqualTo("30:37")
        assertThat(format(30*60 + 87)).isEqualTo("31:27")
                                // one hour minus one second
        assertThat(format(3599L)).isEqualTo("59:59")
        assertThat(format(3600L)).isEqualTo("60:00") // no conversion to hours
        assertThat(format(100*60+59L)).isEqualTo("100:59")
    }

}