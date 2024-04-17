package org.cezkor.towardsgoalsapp.reminders
import com.google.common.truth.Truth.assertThat
import org.cezkor.towardsgoalsapp.Constants
import org.cezkor.towardsgoalsapp.OwnerType
import org.cezkor.towardsgoalsapp.database.ReminderData
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class ReminderSortingTest {

    class NextLongGenerator private constructor() {

        companion object {

            private var number: Long = 0L
            fun getNext() : Long {
                number += 1
                return number
            }
        }

    }
    private fun getNewReminderData(
        remindOn: Instant,
        ownerType: OwnerType
    ) : ReminderData = ReminderData(
        NextLongGenerator.getNext(),
        remindOn,
        null,
        Constants.IGNORE_ID_AS_LONG,
        ownerType
    )

    @Test
    fun `reminders are sorted correctly`() {
        val now = Instant.now()

        assertThat(ReminderLogic.getOrderedListOfReminder(null)).isNull()
        assertThat(ReminderLogic.getOrderedListOfReminder(arrayListOf())).isNull()

        // this is expected order
        val r1 = getNewReminderData(now.plus(1, ChronoUnit.DAYS), OwnerType.TYPE_TASK)
        val r2 = getNewReminderData(now.plus(1, ChronoUnit.DAYS), OwnerType.TYPE_HABIT)
        val r3 = getNewReminderData(now.plus(2, ChronoUnit.DAYS), OwnerType.TYPE_TASK)
        val r4 = getNewReminderData(now.plus(3, ChronoUnit.DAYS), OwnerType.TYPE_HABIT)
        val r5 = getNewReminderData(now.plus(3, ChronoUnit.DAYS), OwnerType.TYPE_TASK)

        val arr1 = arrayListOf(r1, r2, r3, r4, r5)
        val arr2 = arrayListOf(r5, r3, r1, r2, r4)

        assertThat(ReminderLogic.getOrderedListOfReminder(arr1))
            .containsExactly(r1, r2, r3, r4, r5)
        assertThat(ReminderLogic.getOrderedListOfReminder(arr2))
            .containsExactly(r1, r2, r3, r4, r5)

    }
}