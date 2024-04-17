package org.cezkor.towardsgoalsapp.reminders

import org.cezkor.towardsgoalsapp.database.ReminderData
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderLogic private constructor(){

    companion object {

        val comparator = object : Comparator<ReminderData> {
            override fun compare(o1: ReminderData?, o2: ReminderData?): Int {
                // eldest reminder has the highest priority
                // task have higher priority than habits
                if (o1 == null && o2 == null) return 0
                if (o1 == null) return -1
                if (o2 == null) return 1
                val byRemindOn = o1.remindOn.compareTo(o2.remindOn)
                if (byRemindOn != 0) return byRemindOn
                if (o1.ownerType == org.cezkor.towardsgoalsapp.OwnerType.TYPE_TASK &&
                    o2.ownerType == org.cezkor.towardsgoalsapp.OwnerType.TYPE_HABIT)
                    return 1
                if (o2.ownerType == org.cezkor.towardsgoalsapp.OwnerType.TYPE_TASK &&
                    o1.ownerType == org.cezkor.towardsgoalsapp.OwnerType.TYPE_HABIT)
                    return -1
                return 0
            }
        }

        fun prepareInstantForNextDayBasedOnHourAndToday(time: LocalTime) : Instant {
            val ret : LocalDateTime
            val now = LocalTime.now()
            ret = if (now.isAfter(time)) {
                LocalDateTime.now().plusDays(1)
                    .withHour(time.hour).withMinute(time.minute)
                    .withSecond(0).withNano(0)
            } else {
                LocalDateTime.now()
                    .withHour(time.hour).withMinute(time.minute)
                    .withSecond(0).withNano(0)
            }
            return ret.atZone(ZoneId.systemDefault()).toInstant()
        }

        fun getOrderedListOfReminder(reminderData: List<ReminderData>?) : List<ReminderData>? {
            if (reminderData == null) return null
            if (reminderData.isEmpty()) return null
            return reminderData.sortedWith(comparator)
        }

    }

}
