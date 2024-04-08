package com.example.towardsgoalsapp.database.repositories

import androidx.lifecycle.MutableLiveData
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.OwnerType
import com.example.towardsgoalsapp.database.HabitData
import com.example.towardsgoalsapp.database.TGDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit

class HabitRepository(private val db: TGDatabase): OwnedByOneTypeOnlyOwnerUserData {
    override suspend fun getAllByOwnerId(ownerId: Long, allowUnfinished: Boolean): ArrayList<HabitData> {
        return withContext(Dispatchers.IO) {
            val all = db.habitDataQueries.selectAllOf(ownerId).executeAsList()
                .toCollection(ArrayList())
            val filtered =
                if (allowUnfinished) all else all.filter { hd -> !hd.habitEditUnfinished }
            filtered.toCollection(ArrayList())
        }
    }

    suspend fun getAllByGoalId(goalId: Long, allowUnfinished: Boolean = true): ArrayList<HabitData>
        = getAllByOwnerId(goalId, allowUnfinished)

    suspend fun addHabit(habitName: String, habitDescription: String,
                        habitTargetCount: Long, habitTargetPeriod: Long, goalId: Long ) : Long {
        return withContext(Dispatchers.IO) {
            db.habitDataQueries.insertOneHabit(
                null,
                habitName,
                habitDescription,
                habitTargetCount,
                habitTargetPeriod,
                goalId
            )
            db.habitDataQueries
                .lastInsertRowId().executeAsOneOrNull() ?: Constants.IGNORE_ID_AS_LONG
        }
    }

    suspend fun putAsUnfinished(habitData: HabitData) {
        return withContext(Dispatchers.IO) {
            db.habitDataQueries.insertOneEditUnfinishedHabit(
                habitData.habitId,
                habitData.habitName,
                habitData.habitDescription,
                habitData.habitTargetCount,
                habitData.habitTargetPeriod,
                habitData.goalId,
                habitData.habitDoneWellCount,
                habitData.habitDoneNotWellCount,
                habitData.habitTotalCount,
                habitData.habitTargetCompleted,
                habitData.habitMarkCount,
                habitData.habitLastMarkedOn
            )
        }
    }

    suspend fun putAllHabits(habits: ArrayList<MutableLiveData<HabitData>>) {
        return withContext(Dispatchers.IO) {
            db.taskDataQueries.transaction {
                for (ht in habits) ht.value?.run {
                    db.habitDataQueries.insertOneHabit(
                        null,
                        this.habitName,
                        this.habitDescription,
                        this.habitTargetCount,
                        this.habitTargetPeriod,
                        this.goalId
                    )
                }
            }
        }
    }

    override suspend fun updateTexts(id: Long, firstText: String, secondText: String) {
        return withContext(Dispatchers.IO) {
            db.habitDataQueries.updateHabitTexts(firstText, secondText, id)
        }
    }

    suspend fun updateTargets(id: Long, targetCount: Long, targetPeriod: Long) {
        return withContext(Dispatchers.IO) {
            db.habitDataQueries.updateHabitTargets(targetCount,targetPeriod, id)
        }
    }

    suspend fun autoSkipDaysWithoutMarkingIfApplicableOfHabit(id: Long) : Long{
        //                                                                daysSkipped
        return withContext(Dispatchers.IO) {
            val habit = db.habitDataQueries.selectGivenHabit(id).executeAsOneOrNull()
                ?: return@withContext 0L
            // lastMarkedOn == null <=> habit was never marked before, so its useless to skip
            val lastMarkedOn = habit.habitLastMarkedOn ?: return@withContext 0L
            val lMOtoDateTime = LocalDateTime.ofInstant(lastMarkedOn, ZoneId.systemDefault())
            val now = LocalDateTime.now()
            // allow to at most 23:59:00 of next day
            val latestDateTimeToAllow = lMOtoDateTime
                .plusDays(1)
                .withSecond(0)
                .withHour(23)
                .withNano(0)
                .withMinute(59)
            val earliestTimeOfTwoDaysAfter = lMOtoDateTime
                .plusDays(2)
                .withSecond(0)
                .withHour(0)
                .withNano(0)
                .withMinute(0)
            if (now <= latestDateTimeToAllow) return@withContext 0
            // skip one day if user happens to start filling in questions about habit between
            // 23:59:00 and 00:00:00 of the next day
            if ( now <= earliestTimeOfTwoDaysAfter) {
                skipHabit(id, now.atZone(ZoneId.systemDefault()).toInstant())
                return@withContext 1
            }

            val daysToSkip = Duration.between(now, earliestTimeOfTwoDaysAfter).toDays()
            db.habitDataQueries.autoSkipHabitByDayCount(daysToSkip, daysToSkip, id)

            var dayOfSkipping = earliestTimeOfTwoDaysAfter
                .plusMinutes(1)
                .atZone(ZoneId.systemDefault())
                .toInstant() // assume we skip at 00:01 of each day to skip
            // add proper information for HabitStatsData
            for (i in 0..<daysToSkip) {
                db.habitStatsDataQueries.insertData(
                    habit.goalId,
                    habit.habitId,
                    // habit skipped
                    false,
                    false,
                    dayOfSkipping
                )
                dayOfSkipping = dayOfSkipping.plus(1,ChronoUnit.DAYS)
            }

            return@withContext daysToSkip
        }
    }

    override suspend fun markEditing(id: Long, isUnfinished: Boolean) {
        return withContext(Dispatchers.IO) {
            db.habitDataQueries.markHabitEdit(isUnfinished, id)
        }
    }

    suspend fun skipHabit(id: Long, markedOn: Instant) {
        return withContext(Dispatchers.IO) {
            db.habitDataQueries.skipHabit(markedOn,id)
        }
    }

    suspend fun markHabitDoneWell(id: Long, markedOn: Instant) {
        return withContext(Dispatchers.IO) {
            db.habitDataQueries.markHabitAsDoneWell(markedOn,id)
        }
    }

    suspend fun markHabitDoneNotWell(id: Long, markedOn: Instant) {
        return withContext(Dispatchers.IO) {
            db.habitDataQueries.markHabitAsNotDoneWell(markedOn,id)
        }
    }

    override suspend fun getOneById(id: Long, allowUnfinished: Boolean ): HabitData? {
        return withContext(Dispatchers.IO) {
            val unfinished = db.habitDataQueries.getHabitUnfinished(id).executeAsOneOrNull()
                ?: return@withContext null
            if (unfinished) {
                if (!allowUnfinished) return@withContext null
                val uhd = db.habitDataQueries.selectGivenUnfinishedHabit(id).executeAsOneOrNull()
                if (uhd == null) null
                else HabitData(
                    uhd.habitId,
                    true,
                    uhd.habitName,
                    uhd.habitDescription,
                    uhd.habitTargetCount,
                    uhd.habitTargetPeriod,
                    uhd.goalId,
                    uhd.habitDoneWellCount,
                    uhd.habitDoneNotWellCount,
                    uhd.habitTotalCount,
                    uhd.habitTargetCompleted,
                    uhd.habitMarkCount,
                    uhd.habitLastMarkedOn
                )
            }
            else {
                val x = db.habitDataQueries.selectGivenHabit(id).executeAsOneOrNull()
                x
            }
        }
    }

    override suspend fun deleteById(id: Long) {
        return withContext(Dispatchers.IO) {
            val reminder = db.reminderDataQueries.selectOf(id, OwnerType.TYPE_HABIT)
                .executeAsOneOrNull()
            reminder?.run { db.reminderDataQueries.deleteReminder(reminder.remId) }
            db.habitDataQueries.deleteHabit(id)
        }
    }
}