package com.example.towardsgoalsapp.goals

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import com.example.towardsgoalsapp.R

class AddOrEditGoal : AppCompatActivity() {

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.title_only_menu, menu)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_add_edit)

        val toolbar:  androidx.appcompat.widget.Toolbar= findViewById(R.id.goalAddEditToolbar)
        // set title to goal title
        setSupportActionBar(toolbar)
    }
}