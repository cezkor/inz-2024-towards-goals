package com.example.towardsgoalsapp.stats.questions

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.Constants.Companion.CLASS_NUMBER_NOT_RECOGNIZED
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.etc.OneTimeEventWithValue
import com.example.towardsgoalsapp.habits.questioning.HabitQuestionsViewModel
import com.example.towardsgoalsapp.tasks.ongoing.TaskOngoingViewModel

class QuestionItemListViewModelFactory(private val questionList: ArrayList<Question<Double>?>): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return QuestionItemListViewModel(questionList) as T
    }
}

interface ViewModelWithDoubleValueQuestionList {
    fun getQuestionList() : ArrayList<Question<Double>?>

    fun getQuestionsReadyToSave() : MutableLiveData<OneTimeEventWithValue<Boolean>>
}

class QuestionItemListViewModel(
    val questionList: ArrayList<Question<Double>?>
): ViewModel()

class DoubleValueQuestionItemList : Fragment() {

    companion object {

        const val INHERIT_FROM_CLASS_NUMBER = "ihnclnum"
        const val SHOW_AS_INT = "sai"
        const val LOG_TAG = "QIList"

        @JvmStatic
        fun newInstance(inheritFromClass: Int, showAsInt: Boolean = false) =
            DoubleValueQuestionItemList().apply {
                arguments = Bundle().apply {
                    putInt(INHERIT_FROM_CLASS_NUMBER, inheritFromClass)
                    putBoolean(SHOW_AS_INT, showAsInt)
                }
            }

        val expectedViewModelClasses = setOf(
            Constants.viewModelClassToNumber[HabitQuestionsViewModel::class.java]
                ?: CLASS_NUMBER_NOT_RECOGNIZED,
            Constants.viewModelClassToNumber[TaskOngoingViewModel::class.java]
                ?: CLASS_NUMBER_NOT_RECOGNIZED
        )
    }

    private lateinit var viewModel: QuestionItemListViewModel

    private var showAsInt: Boolean = false

    private var adapter: QuestionOfDoubleAnswersItemListAdapter? = null

    private var mutable: MutableLiveData<OneTimeEventWithValue<Boolean>>? = null

    private fun extractList(classnumber: Int) : ArrayList<Question<Double>?>?{
        if ((classnumber in expectedViewModelClasses) && classnumber != CLASS_NUMBER_NOT_RECOGNIZED) {
            val clazz: Class<out ViewModel>? = Constants.numberOfClassToViewModelClass[classnumber]
            if (clazz == null ) return null
            else {
                val inheritedViewModel = ViewModelProvider(requireActivity())[clazz]
                if (inheritedViewModel is ViewModelWithDoubleValueQuestionList)
                    return inheritedViewModel.getQuestionList()
            }
        }
        return null
    }

    private fun extractMutable(classnumber: Int) {
        if ((classnumber in expectedViewModelClasses) && classnumber != CLASS_NUMBER_NOT_RECOGNIZED) {
            val clazz: Class<out ViewModel>? = Constants.numberOfClassToViewModelClass[classnumber]
            if (clazz == null ) return
            else {
                val inheritedViewModel = ViewModelProvider(requireActivity())[clazz]
                if (inheritedViewModel is ViewModelWithDoubleValueQuestionList)
                    mutable = inheritedViewModel.getQuestionsReadyToSave()
            }
        }
        return
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        try {
            requireActivity()
        }
        catch (e: IllegalStateException) {
            Log.e(LOG_TAG, "unable to get activity", e)
            return null
        }

        arguments?.let {
            val classNumber = it.getInt(INHERIT_FROM_CLASS_NUMBER)
            showAsInt = it.getBoolean(SHOW_AS_INT, showAsInt)
            val sharer = extractList(classNumber)
            extractMutable(classNumber)
            sharer?.run {
                viewModel = ViewModelProvider(viewModelStore,
                    QuestionItemListViewModelFactory(this))[QuestionItemListViewModel::class.java]
            }
        }

        val view = inflater.inflate(R.layout.question_list_fragment, container, false)
        if (view is RecyclerView) { with(view) {

            // for reasons unknown to me
            // animations of recycler view items can cause crashes
            view.itemAnimator = null

            view.setItemViewCacheSize(Constants.RV_ITEM_CACHE_SIZE)
            view.setHasFixedSize(true)

            view.addItemDecoration(
                DividerItemDecoration(view.context, RecyclerView.HORIZONTAL).apply {
                    setDrawable(
                        ContextCompat.getDrawable(view.context, R.drawable.recycler_view_divider)!!
                    )
                }
            )

            adapter = QuestionOfDoubleAnswersItemListAdapter(
                viewModel.questionList,
                showAsInt
            )
            view.adapter = adapter

        } }
        return view
    }

    fun forceSavingQuestions(){
        mutable?.value = OneTimeEventWithValue(adapter?.forceSavingQuestions() ?: false)
    }

    override fun onPause() {
        forceSavingQuestions()
        super.onPause()
    }
}