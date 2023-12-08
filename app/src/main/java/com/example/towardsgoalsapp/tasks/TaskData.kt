package com.example.towardsgoalsapp.tasks

import com.example.towardsgoalsapp.Constants

data class TaskData(
    val taskId: Long = Constants.IGNORE_ID_AS_LONG,
    val taskName: String = Constants.EMPTY_STRING,
    val taskDescription: String = Constants.EMPTY_STRING,
    val taskProgress: Double = .0,
    val taskOverId: Long = Constants.IGNORE_ID_AS_LONG,
    val taskDone: Boolean = false,
    val subtasksCount: Long = 0
)
