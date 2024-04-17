package org.cezkor.towardsgoalsapp.stats.questions

open class Question<AT>(
    val questionText: String,
    val defaultValue: AT? = null
) {

    var answer: AT? = null
        protected set

    open fun answerWith(value: AT?, fillWithDefault: Boolean = true) {
        if (value == null && fillWithDefault) answer = defaultValue
        answer = value
    }

}
open class RangedDoubleQuestion(questionText: String, lower: Double,
                                upper: Double, defaultValue: Double? = null)
    : Question<Double>(questionText, defaultValue) {

    var lower: Double = lower
        protected set

    var upper: Double = upper
        protected set
    init {
        if (lower > upper) {
            val l = upper
            this.upper = lower
            this.lower = l
        }
    }

    override fun answerWith(value: Double?, fillWithDefault: Boolean) {
        if (value == null) {
            if (fillWithDefault) answer = defaultValue
            return
        }
        if (value < lower) {
            answer = lower
            return
        }
        if (value > upper) {
            answer = upper
            return
        }
        answer = value
    }
}