package com.example.towardsgoalsapp.tasks.ongoing

import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.etc.Translation
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PomodoroState(
    totalWorkTime: Long = 0,
    currentTime: Long = 0,
    totalTime: Long = 0,
    totalBreakTime: Long = 0,
    breakCount: Long = 0,
    private var isBreak: Boolean = false
) {

    private val mutex = Mutex()

    suspend fun determineIndexBasedOnState() : Int = mutex.withLock {
        if (! isBreak) return TaskOngoingViewModel.WORK_TIME_ID
        return if (isBreakLong && isBreak) TaskOngoingViewModel.LONG_BREAK_ID
        else TaskOngoingViewModel.SHORT_BREAK_ID
    }
    private var totalTimeInSeconds: Long = totalTime

    private var timeOfCurrentStateInSeconds: Long = currentTime

    private var totalTimeOfWorkInSeconds: Long = totalWorkTime

    private var totalTimeOfBreaksInSeconds: Long = totalBreakTime

    private var breakCount: Long = 0

    suspend fun getTotalTimeOfBreaks() : Long = mutex.withLock { totalTimeOfBreaksInSeconds }
    suspend fun getTotalTimeOfWork(): Long = mutex.withLock { totalTimeOfWorkInSeconds }

    suspend fun getCountOfBreaks(): Long = mutex.withLock { breakCount }

    suspend fun getTotalTime() : Long = mutex.withLock { totalTimeInSeconds }
    suspend fun isBreak() : Boolean = mutex.withLock { isBreak }

    suspend fun getTimeOfCurrentState() : Long = mutex.withLock { timeOfCurrentStateInSeconds }

    private var isBreakLong: Boolean = if (breakCount > 0) ((breakCount % 4).toInt() == 0) else false

    suspend fun sumTotalAndCurrentTimes() : Long = mutex.withLock {
        totalTimeInSeconds + timeOfCurrentStateInSeconds
    }

    suspend fun switchState() = mutex.withLock {
        totalTimeInSeconds += timeOfCurrentStateInSeconds
        if (isBreak) {
            isBreak = false
            totalTimeOfBreaksInSeconds += timeOfCurrentStateInSeconds
        }
        else {
            isBreak = true
            breakCount += 1
            isBreakLong = ((breakCount % 4).toInt() == 0)
            totalTimeOfWorkInSeconds += timeOfCurrentStateInSeconds
        }
        timeOfCurrentStateInSeconds = 0
    }

    suspend fun moveTimeBy(seconds: Long) = mutex.withLock {
        timeOfCurrentStateInSeconds += seconds
    }
}

class PomodoroSettings(
    var workTimeInMinutes: Int = Constants.DEFAULT_WORK_TIME,
    var shortBreakInMinutes: Int = Constants.DEFAULT_SHORT_BREAK_TIME,
    var longBreakInMinutes: Int = Constants.DEFAULT_LONG_BREAK_TIME
)
