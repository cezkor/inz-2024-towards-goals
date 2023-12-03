package com.example.towardsgoalsapp.goals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.example.towardsgoalsapp.R
class AddGoalSuggestion(): Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val plusButton: ImageButton = view.findViewById(R.id.addGoalPlusButton)
        plusButton.setOnClickListener {
            // to do
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
