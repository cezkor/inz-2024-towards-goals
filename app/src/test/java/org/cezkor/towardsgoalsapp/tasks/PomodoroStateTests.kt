package org.cezkor.towardsgoalsapp.tasks

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.cezkor.towardsgoalsapp.tasks.ongoing.PomodoroState
import org.cezkor.towardsgoalsapp.tasks.ongoing.TaskOngoing
import org.junit.Test

class PomodoroStateTests {

    @Test
    fun `pomodoro state switching is correct`() {
        val pState = PomodoroState()
        runBlocking {
            assertThat(pState.isBreak()).isFalse()

            pState.moveTimeBy(30)
            assertThat(pState.isBreak()).isFalse()
            assertThat(pState.isBreakLong()).isFalse()
            pState.switchState()
            assertThat(pState.isBreak()).isTrue()
            assertThat(pState.isBreakLong()).isFalse()
            pState.switchState()
            assertThat(pState.isBreak()).isFalse()
            assertThat(pState.isBreakLong()).isFalse()
            pState.switchState()
            assertThat(pState.isBreak()).isTrue()
            assertThat(pState.isBreakLong()).isFalse()
            pState.switchState()
            assertThat(pState.isBreak()).isFalse()
            assertThat(pState.isBreakLong()).isFalse()
            pState.switchState()
            assertThat(pState.isBreak()).isTrue()
            assertThat(pState.isBreakLong()).isFalse()
            pState.switchState()
            assertThat(pState.isBreak()).isFalse()
            assertThat(pState.isBreakLong()).isFalse()
            pState.switchState()
            assertThat(pState.isBreak()).isTrue()
            assertThat(pState.isBreakLong()).isTrue()
            pState.switchState()
            assertThat(pState.isBreak()).isFalse()
            pState.switchState()
            assertThat(pState.isBreak()).isTrue()
            assertThat(pState.isBreakLong()).isFalse()
        }
    }

    @Test
    fun `pomodoro state changes times correctly`() {
        val pState = PomodoroState()
        runBlocking {
            assertThat(pState.getTotalTime()).isEqualTo(0L)
            assertThat(pState.getTotalTimeOfWork()).isEqualTo(0L)
            assertThat(pState.getTotalTimeOfBreaks()).isEqualTo(0L)
            assertThat(pState.getTimeOfCurrentState()).isEqualTo(0L)

            pState.moveTimeBy(5)
            assertThat(pState.getTotalTime()).isEqualTo(0L)
            assertThat(pState.getTotalTimeOfWork()).isEqualTo(0L)
            assertThat(pState.getTotalTimeOfBreaks()).isEqualTo(0L)
            assertThat(pState.getTimeOfCurrentState()).isEqualTo(5L)

            pState.switchState()
            assertThat(pState.getTotalTime()).isEqualTo(5L)
            assertThat(pState.getTotalTimeOfWork()).isEqualTo(5L)
            assertThat(pState.getTotalTimeOfBreaks()).isEqualTo(0L)
            assertThat(pState.getTimeOfCurrentState()).isEqualTo(0L)

            pState.moveTimeBy(7)
            assertThat(pState.getTotalTime()).isEqualTo(5L)
            assertThat(pState.getTotalTimeOfWork()).isEqualTo(5L)
            assertThat(pState.getTotalTimeOfBreaks()).isEqualTo(0L)
            assertThat(pState.getTimeOfCurrentState()).isEqualTo(7L)

            pState.switchState()
            assertThat(pState.getTotalTime()).isEqualTo(12L)
            assertThat(pState.getTotalTimeOfWork()).isEqualTo(5L)
            assertThat(pState.getTotalTimeOfBreaks()).isEqualTo(7L)
            assertThat(pState.getTimeOfCurrentState()).isEqualTo(0L)
            
            pState.switchState()
            assertThat(pState.getTotalTime()).isEqualTo(12L)
            assertThat(pState.getTotalTimeOfWork()).isEqualTo(5L)
            assertThat(pState.getTotalTimeOfBreaks()).isEqualTo(7L)
            assertThat(pState.getTimeOfCurrentState()).isEqualTo(0L)
        }

    }

}