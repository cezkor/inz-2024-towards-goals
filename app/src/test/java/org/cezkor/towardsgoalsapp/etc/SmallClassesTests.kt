package org.cezkor.towardsgoalsapp.etc

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SmallClassesTests {

    @Test
    fun `one time event classes tests`() {

        val ote = OneTimeEvent()

        assertThat(ote.handled).isEqualTo(false)

        var num = 1
        ote.handleIfNotHandledWith {
            num += 1
        }
        assertThat(num).isEqualTo(2)
        assertThat(ote.handled).isTrue()

        ote.handleIfNotHandledWith {
            num += 1
        }

        assertThat(num).isEqualTo(2)
        assertThat(ote.handled).isTrue()

        val otev = OneTimeEventWithValue(0)
        assertThat(otev.value).isEqualTo(0)

        num = 1
        otev.handleIfNotHandledWith {
            num = otev.value
        }
        assertThat(num).isEqualTo(0)
        assertThat(ote.handled).isTrue()

        ote.handleIfNotHandledWith {
            num += 1
        }

        assertThat(num).isEqualTo(0)
        assertThat(ote.handled).isTrue()

    }

    @Test
    fun `one time handlable test`() {

        var num = 1

        val oth = OneTimeHandleable { num += 1 }
        assertThat(num).isEqualTo(1)
        assertThat(oth.handled).isFalse()

        oth.handle()

        assertThat(num).isEqualTo(2)
        assertThat(oth.handled).isTrue()

        oth.handle()

        assertThat(num).isEqualTo(2)
        assertThat(oth.handled).isTrue()

    }

}