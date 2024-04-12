package org.cezkor.towardsgoalsapp.stats.questions

import android.text.InputType
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import org.cezkor.towardsgoalsapp.R
import kotlin.math.roundToInt


class QuestionOfDoubleAnswersItemListAdapter(
    private val questionList: ArrayList<Question<Double>?>,
    private val showAsInteger: Boolean = false
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val LOG_TAG = "QILAdapter"
    }

    private val savingRunnables: ArrayList<Runnable> = ArrayList()

    fun forceSavingQuestions() : Boolean{
        Log.i(LOG_TAG, "force saving questions")
        savingRunnables.map { it.run() }
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return QuestionWithEditTextViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.value_question_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        Log.i(LOG_TAG, "bind view called, position $position")

        val question: Question<Double>? = questionList[position]
        question?.run {
            (holder as QuestionWithEditTextViewHolder).bind(this)
        }
    }

    override fun getItemCount(): Int = questionList.size

    inner class QuestionWithEditTextViewHolder(private val viewOfItem: View) : RecyclerView.ViewHolder(viewOfItem) {

        private val questionEditText: EditText = viewOfItem.findViewById(R.id.questionEditText)
        private val questionTextView: TextView = viewOfItem.findViewById(R.id.questionTextView)

        fun bind(question: Question<Double>) {
            questionTextView.text = question.questionText

            if (showAsInteger)
                questionEditText.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED
                        or InputType.TYPE_CLASS_NUMBER)

            val textToSet =
            if (showAsInteger)
                question.answer?.roundToInt()?.toString()
                    ?: question.defaultValue?.roundToInt()?.toString()
                    ?: org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING
            else
                question.answer?.toString()
                    ?: question.defaultValue?.toString()
                    ?: org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING

            questionEditText.setText(textToSet)

            fun saveAnswerIfCorrect() {
                var possiblyAnswer = questionEditText.text.toString().toDoubleOrNull()
                possiblyAnswer?.run {
                    if (showAsInteger) possiblyAnswer = possiblyAnswer!!.roundToInt().toDouble()
                }
                question.answerWith(possiblyAnswer)
                val answer = question.answer
                val defaultVal = question.defaultValue
                val text = if (showAsInteger) {
                    answer?.roundToInt()?.toString() ?: defaultVal?.roundToInt()?.toString()
                }
                else {
                    answer?.toString() ?: defaultVal?.toString()
                }
                questionEditText.setText(text)
            }

            questionEditText.setOnFocusChangeListener { v, hasFocus ->
                if (! hasFocus) saveAnswerIfCorrect()
            }
            questionEditText.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    saveAnswerIfCorrect()
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }

            savingRunnables.add {
                saveAnswerIfCorrect()
            }
        }

    }

}