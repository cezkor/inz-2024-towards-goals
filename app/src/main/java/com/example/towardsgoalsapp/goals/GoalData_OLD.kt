package com.example.towardsgoalsapp.goals

import com.example.towardsgoalsapp.Constants

data class GoalData_OLD(
    val goalId: Long = Constants.IGNORE_ID_AS_LONG,
    val goalName: String = Constants.EMPTY_STRING,
    val goalDescription: String = Constants.EMPTY_STRING,
    val progress: Double = .0
    )
