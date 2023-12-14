package com.example.towardsgoalsapp.goals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
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

    private lateinit var pageViewModel: GoalSynopsisViewModel

    private var pageNumber: Int = Constants.IGNORE_PAGE_AS_INT

    private lateinit var goalAdder: ActivityResultLauncher<Triple<Boolean, String, String>>


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pageViewModel = ViewModelProvider(requireActivity())[GoalSynopsisViewModel::class.java]

        pageNumber = requireArguments().getInt(MainActivity.PAGE_NUMBER)

        goalAdder = registerForActivityResult(GoalTextsContract()) {
            if (it.third) {

                // update database, update pageViewModel
            }
        }

        val plusButton: ImageButton = view.findViewById(R.id.addGoalPlusButton)
        plusButton.setOnClickListener {

        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_goal_add_suggestion, container, true)
    }

}
