package com.example.towardsgoalsapp.goals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.main.MainActivity

class AddGoalSuggestion(): Fragment() {

    companion object {
        const val LOG_TAG = "AddGoalSugg"

        fun newInstance(position: Int): AddGoalSuggestion{
            return AddGoalSuggestion().apply {
                arguments = Bundle().apply {
                    putInt(MainActivity.PAGE_NUMBER, position)
                }
            }
        }
    }

    private lateinit var pageViewModel: GoalSynopsisesViewModel

    private var pageNumber: Int = Constants.IGNORE_PAGE_AS_INT

    private lateinit var goalAdder: GoalRefreshRequesterResultLauncher


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pageViewModel = ViewModelProvider(requireActivity())[GoalSynopsisesViewModel::class.java]

        pageNumber = requireArguments().getInt(MainActivity.PAGE_NUMBER)

        goalAdder = registerForActivityResult(GoalRefreshRequesterContract()) {
            if (it == Constants.IGNORE_ID_AS_LONG) return@registerForActivityResult
            // if returned an id -> an goal of such id was probably created
            // -> tell VM to get it
            pageViewModel.getOrUpdateOneGoal(it)
        }

        val plusButton: ImageButton = view.findViewById(R.id.addGoalPlusButton)
        plusButton.setOnClickListener {
            goalAdder.launch(Triple(Constants.IGNORE_ID_AS_LONG, true, pageNumber))
        }

        val textView: TextView = view.findViewById(R.id.addGoalTextView)
        textView.text = getString(R.string.add_goal_suggestion_text, pageNumber + 1)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_goal_add_suggestion, container, false)
    }

}
