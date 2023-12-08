package com.example.towardsgoalsapp.habits

import com.example.towardsgoalsapp.Constants

data class HabitData(

    val habitId: Long = Constants.IGNORE_ID_AS_LONG,
    val habitName: String = Constants.EMPTY_STRING,
    val habitDescription: String = Constants.EMPTY_STRING,
    val habitDoneWellCount: Int = Constants.IGNORE_COUNT_AS_INT,
    val habitDoneNotWellCount: Int = Constants.IGNORE_COUNT_AS_INT,
    //val goalId: Long
    //val queryEvery: Interval

)
