package com.example.towardsgoalsapp.goals

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.main.MainActivity

class GoalSynopsisViewModel: ViewModel() {

    // meanings of enum entries in order:
    // not ready to show
    // readied when activity view created: with goal data, without it
    // data changed (in case of page with GoalSynopsis)
    // populated with GoalData (to make GoalSynopsis page)
    // populated with AddGoalSuggestion
    enum class MutableGoalDataStates {
        NOT_READY, INITIALIZED_POPULATED, INITIALIZED_EMPTY , REFRESHED, POPULATED, EMPTIED
    }

    val arrayOfGoalData: Array<MutableLiveData<GoalData?>> =
        Array(Constants.MAX_GOALS_AMOUNT) { MutableLiveData<GoalData?>() }

    val arrayOfGoalDataStates: Array<MutableLiveData<MutableGoalDataStates>> =
        Array(Constants.MAX_GOALS_AMOUNT) {MutableLiveData(MutableGoalDataStates.NOT_READY)}

    val gidToPosition = HashMap<Long, Int>()

}

class GoalSynopsis: Fragment() {

    private lateinit var goalDetailsOpener: ActivityResultLauncher<Long>

    companion object {
        const val LOG_TAG = "GoalSynopsis"
        private val acceptedGoalDataStates =
            setOf(
                GoalSynopsisViewModel.MutableGoalDataStates.INITIALIZED_POPULATED,
                GoalSynopsisViewModel.MutableGoalDataStates.POPULATED
            )
    }

    private lateinit var pageViewModel: GoalSynopsisViewModel

    private var pageNumber: Int = Constants.IGNORE_PAGE_AS_INT



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // todo: generating page from this fragment causes frame skips - fix it

        val synopsisTitle: TextView = view.findViewById(R.id.goalSynopsisTitle)
        val synopsisDescription: TextView = view.findViewById(R.id.goalSynopsisDescription)
        val synopsisProgress: ProgressBar = view.findViewById(R.id.goalSynopsisProgressBar)

        val goalDetailsButton: Button = view.findViewById(R.id.expandGoalButton)

        fun updateUI(data: GoalData) {
            synopsisTitle.text = data.goalName
            synopsisDescription.text = data.goalDescription
            synopsisProgress.progress = (100 * data.progress).toInt()
        }


        pageViewModel = ViewModelProvider(requireActivity())[GoalSynopsisViewModel::class.java]

        pageNumber = requireArguments().getInt(MainActivity.PAGE_NUMBER)

        val updater = Observer<GoalData?> {

            if (pageViewModel.arrayOfGoalDataStates[pageNumber].value in acceptedGoalDataStates)
                it?.run { updateUI(this) }
            // else ignore -> we're being replaced by no goal or there was no goal at all
        }

        pageViewModel.arrayOfGoalData[pageNumber].observe(
            viewLifecycleOwner, updater
        )

        goalDetailsOpener = registerForActivityResult(MainActivityRefreshIntentContract()){

            if (it == Constants.IGNORE_ID_AS_LONG) return@registerForActivityResult
            // get goal from given id
            val pos = pageViewModel.gidToPosition[it]

        }

        goalDetailsButton.setOnClickListener {

            val goalId: Long? = pageViewModel.arrayOfGoalData[pageNumber].value?.goalId
            Log.i(LOG_TAG, "launching intent for goalid $goalId")
            goalId?.run {
                goalDetailsOpener.launch(this)
            }
        }

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
