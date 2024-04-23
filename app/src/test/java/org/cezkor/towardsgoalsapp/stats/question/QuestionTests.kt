package org.cezkor.towardsgoalsapp.stats.question

import org.cezkor.towardsgoalsapp.stats.questions.Question
import org.junit.Test
import com.google.common.truth.Truth.*
import org.cezkor.towardsgoalsapp.stats.questions.RangedDoubleQuestion

class QuestionTests {

    @Test
    fun `question tests`() {

        val q1 = Question<Int>("")
        val q2 = Question<Int>("", 5)

        assertThat(q1.answer).isNull()
        assertThat(q2.answer).isNull()

        q1.answerWith(null)
        assertThat(q1.answer).isNull()
        q1.answerWith(5)
        assertThat(q1.answer).isEqualTo(5)
        q1.answerWith(null, false)
        assertThat(q1.answer).isNull()

        q2.answerWith(null, false)
        assertThat(q2.answer).isNull()
        q2.answerWith(1)
        assertThat(q2.answer).isEqualTo(1)
        q2.answerWith(null, true)
        assertThat(q2.answer).isEqualTo(5)

    }

    @Test
    fun `ranged question tests`() {

        val q1 = RangedDoubleQuestion("", 3.0, 5.0,  4.0)
        val q2 = RangedDoubleQuestion("", 5.0, 3.0)
        val q3 = RangedDoubleQuestion("", 5.0, 5.0)

        assertThat(q2.lower).isLessThan(q2.upper)

        assertThat(q1.answer).isNull()
        assertThat(q2.answer).isNull()
        assertThat(q3.answer).isNull()

        q1.answerWith(null)
        assertThat(q1.answer).isEqualTo(4.0)
        q2.answerWith(null)
        assertThat(q2.answer).isNull()
        q3.answerWith(null)
        assertThat(q3.answer).isNull()

        q1.answerWith(4.5)
        assertThat(q1.answer).isEqualTo(4.5)
        q2.answerWith(4.5)
        assertThat(q2.answer).isEqualTo(4.5)
        q3.answerWith(4.5)
        assertThat(q3.answer).isEqualTo(5.0)

        q1.answerWith(7.5)
        assertThat(q1.answer).isEqualTo(5.0)
        q2.answerWith(7.5)
        assertThat(q2.answer).isEqualTo(5.0)
        q3.answerWith(7.5)
        assertThat(q3.answer).isEqualTo(5.0)

        q1.answerWith(1.5)
        assertThat(q1.answer).isEqualTo(3.0)
        q2.answerWith(1.5)
        assertThat(q2.answer).isEqualTo(3.0)
        q3.answerWith(1.5)
        assertThat(q3.answer).isEqualTo(5.0)

    }

}