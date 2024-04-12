package org.cezkor.towardsgoalsapp.habits

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class HabitLogic private constructor(){

    companion object {
        fun checkIfHabitIsMarkable(lastMarkedOn: Instant?) : Boolean {
            // we can't mark habit on the same day it was marked (if it was recorded)
            return if (lastMarkedOn == null) true
            else {
                val now = LocalDateTime.now()
                val lastMarkedOnDT = LocalDateTime.ofInstant(
                    lastMarkedOn,
                    ZoneId.systemDefault()
                )
                if (now <= lastMarkedOnDT) false
                else {
                    val lMODate = lastMarkedOnDT.toLocalDate()
                    val nowDate = now.toLocalDate()
                    nowDate > lMODate
                }
            }
        }
    }

}