package com.example.towardsgoalsapp.goals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.towardsgoalsapp.R

class GoalSynopsisViewModel: ViewModel() {

    public val mutableGoalData: MutableLiveData<GoalData> by lazy {
        MutableLiveData<GoalData>()
    }

    public val mutablePageNumber: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }


}

class GoalSynopsis(pageNum: Int): Fragment() {

    private val pageNumber: Int = pageNum

    private lateinit var pageViewModel: GoalSynopsisViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pageViewModel = ViewModelProvider(
            this,
            ViewModelProvider.NewInstanceFactory()
        ) [GoalSynopsisViewModel::class.java]

        val pageNumberTextView: TextView = view.findViewById(R.id.goalTextview)

        val pageNumObserver = Observer<Int> {
            var text: String = pageNumberTextView.text.toString()
            text += "strona $it"
            pageNumberTextView.text = text
        }

        pageViewModel.mutablePageNumber.observe(viewLifecycleOwner, pageNumObserver)

        pageViewModel.mutablePageNumber.value = pageNumber
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_goal_synopsis, container, true)

    }




}
