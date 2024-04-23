package org.cezkor.towardsgoalsapp.stats.models

import org.cezkor.towardsgoalsapp.stats.questions.Question
import org.junit.Test
import com.google.common.truth.Truth.*
import org.cezkor.towardsgoalsapp.database.HabitParameter
import org.cezkor.towardsgoalsapp.database.HabitParameterValue
import org.cezkor.towardsgoalsapp.etc.Translation
import org.cezkor.towardsgoalsapp.stats.questions.RangedDoubleQuestion
import java.time.Instant

class SettingsTests {

    private val translation = object : Translation() {
        override fun getString(resId: Int): String = "not important"
    }

    private fun choicesMatch(set: ModelSettingWithChoices<*>) : Boolean
        = ( set.choices.size == set.choicesNames.size )

    @Test
    fun `choices match by size`() {
        assertThat(choicesMatch(PredictSize(translation))).isTrue()
        assertThat(choicesMatch(SampleSize(translation))).isTrue()
        assertThat(choicesMatch(ShowDataFrom(translation))).isTrue()
        assertThat(choicesMatch(IntegrationRank(translation))).isTrue()
        assertThat(choicesMatch(ARRank(translation))).isTrue()
        assertThat(choicesMatch(SeasonSize(translation))).isTrue()
        assertThat(choicesMatch(MARank(translation))).isTrue()
    }

    @Test
    fun `choice test`() {
        class TestClass(tr: Translation) : ModelSettingWithChoices<Int>(tr, 0) {
            override val settingName = "not important"
            override val choices = arrayOf(1, 2, 3)
            override val choicesNames = arrayOf("one" , "two", "three")
        }
        val testObj = TestClass(translation)
        assertThat(testObj.choice).isNull()
        testObj.setChoice(1)
        assertThat(testObj.choice).isEqualTo(2)
        testObj.setChoice(-100)
        assertThat(testObj.choice).isEqualTo(2)
        testObj.setChoice(100)
        assertThat(testObj.choice).isEqualTo(2)
    }


}