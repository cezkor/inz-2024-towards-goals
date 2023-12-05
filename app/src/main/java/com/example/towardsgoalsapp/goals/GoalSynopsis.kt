package com.example.towardsgoalsapp.goals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.main.MainActivity

class GoalSynopsisViewModel: ViewModel() {

    val arrayOfGoalData: Array<MutableLiveData<GoalData?>> =
        Array(Constants.MAX_GOALS_AMOUNT) { MutableLiveData<GoalData?>() }

    val isGoalDataPresent = Array(Constants.MAX_GOALS_AMOUNT) {false}

}

class GoalSynopsis: Fragment() {

    companion object {
        const val LOG_TAG = "GoalSynopsis"
    }

    private lateinit var pageViewModel: GoalSynopsisViewModel

    private var pageNumber: Int = Constants.IGNORE_PAGE_AS_INT

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // todo: generating page from this fragment causes frame skips - fix it

        val synopsisTitle: TextView = view.findViewById(R.id.goalSynopsisTitle)
        val synopsisDescription: TextView = view.findViewById(R.id.goalSynopsisDescription)
        val synopsisProgress: ProgressBar = view.findViewById(R.id.goalSynopsisProgressBar)

        fun updateUI(data: GoalData) {
            synopsisTitle.text = data.goalName
            synopsisDescription.text = data.goalDescription
            synopsisProgress.progress = (100 * data.progress).toInt()
        }

        pageViewModel = ViewModelProvider(requireActivity())[GoalSynopsisViewModel::class.java]

        pageNumber = requireArguments().getInt(MainActivity.PAGE_NUMBER)

        val updater = Observer<GoalData?> {

            if (pageViewModel.isGoalDataPresent[pageNumber]) {
                it?.run { updateUI(this) }
            }
            // else ignore -> we're being replaced by no goal
        }

        pageViewModel.arrayOfGoalData[pageNumber].observe(
            viewLifecycleOwner, updater
        )

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_goal_synopsis, container, true)
    }

    override fun onResume() {
        super.onResume()
        // to do
    }



}
